# -*- coding: utf-8 -*-
import threading
import random
import Queue
import logging
import time

def tohex(i, size):
    return hex(i)[-size:].rjust(size, '0').replace('x', '0')

class FakeS4(object):
    """
    A fake serial port implementation that roughly estimates the
    actual S4 rowing interface.
    """
    def __init__(self):
        self.__queue = Queue.Queue()
        self.__thread = None
        self.__workout_event = None
        self.__stop_event = None
        self.__i = 0
        self.__total_meters = 0

    def __publish(self, s):
        self.__queue.put(s + '\r\n')

    def generate(self):
        self.__publish('OK')
        # ping until a workout is started
        while not self.__workout_event.is_set():
            self.__publish('PING')
            self.__workout_event.wait(0.1)

        self.__i = 0
        self.__total_meters = 0
        while not self.__stop_event.is_set(): # TODO maybe parse workout
            self.__publish('PING')
            self.__publish('IDD055' + tohex(self.__m, 4))
            self.__publish('IDS1E1{0}'.format(int(self.__i / 10) % 60, 2))
            self.__publish('IDS1E0{0}'.format(int(self.__i % 10), 2))
            self.__publish('IDS1E2{0}'.format(int(self.__i / 600), 2))
            if self.__i % 10 == 0:
                self.__publish('IDS1A9' + tohex(random.randint(22, 27), 2))
                if self.__m > 0 and self.__i > 10:
                    self.__publish('IDD14A' + tohex(int((self.__m / (self.__i / 10)) * 100), 4))

            self.__i = self.__i + 1
            if self.__i % 2 == 0:
                self.__m += 1

            self.__stop_event.wait(0.1)
        logging.info("stopped generating")

    def start_publishing(self):
        self.__workout_event = threading.Event()
        self.__stop_event = threading.Event()
        self.__thread = threading.Thread(target=self.generate)
        self.__thread.setDaemon(True)
        self.__thread.start()

    def open(self):
        self.start_publishing()

    def isOpen(self):
        return self.__thread and self.__thread.is_alive()

    def close(self):
        if self.__stop_event:
            self.__stop_event.set()
        if self.__workout_event:
            self.__workout_event.set()

    def readline(self):
        return self.__queue.get()

    def write(self, s):
        if s[:2] == 'WS':
            self.__workout_event.set()
        if s.strip() == 'RESET':
            self.close()
            time.sleep(0.2) # sleep cos i re-use the stop_event
            self.__queue.queue.clear()
            self.__i = 0
            self.__m = 0
            self.start_publishing()

    def flush(self):
        pass
