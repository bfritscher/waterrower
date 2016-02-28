# -*- coding: utf-8 -*-
import threading
import random
import Queue
import logging
import time

from interface import PING_RESPONSE, RESET_REQUEST, OK_RESPONSE, MODEL_INFORMATION_REQUEST, \
    MODEL_INFORMATION_RESPONSE


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

    def __publish(self, s):
        self.__queue.put(s.upper() + '\r\n')

    def generate(self):
        i = 0
        m = 0
        s = 0
        self.__publish(OK_RESPONSE)
        # ping until a workout is started
        while not self.__workout_event.is_set():
            self.__publish(PING_RESPONSE)
            self.__workout_event.wait(0.1)

        while not self.__stop_event.is_set():  # TODO maybe parse workout
            self.__publish(PING_RESPONSE)
            self.__publish('IDD055' + tohex(m, 4))
            self.__publish('IDD140' + tohex(s, 4))
            self.__publish('IDS1E1{0}'.format(int(i / 10) % 60, 2))
            self.__publish('IDS1E0{0}'.format(int(i % 10), 2))
            self.__publish('IDS1E2{0}'.format(int(i / 600), 2))
            if i % 10 == 0:
                s += 1
                self.__publish('IDS1A9' + tohex(random.randint(22, 27), 2))
                if m > 0 and i > 10:
                    self.__publish('IDD14A' + tohex(int((m / (i / 10)) * 100), 4))
                self.__publish('IDD1A0' + tohex(random.randint(80, 140), 4))
            if i % 2 == 0:
                m += 1
            i += 1
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
        if s.strip() == MODEL_INFORMATION_REQUEST:
            self.__publish(MODEL_INFORMATION_RESPONSE + '40200')
        if s[3:6] == '0A9':
            self.__publish('IDS0A9' + tohex(180, 2))
        if s[:2] == 'WS':
            self.__workout_event.set()
        if s.strip() == RESET_REQUEST:
            self.close()
            time.sleep(0.2)  # sleep cos stop_event re-use
            self.__queue.queue.clear()
            self.start_publishing()

    def flush(self):
        pass
