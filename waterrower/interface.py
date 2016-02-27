import threading
import logging
import time

COMMANDS = {'start':             'USB',
            'reset':             'RESET',
            'exit':              'EXIT',
            'info':              'IV?',
            'intensity_mps':     'DIMS',
            'intensity_mph':     'DIMPH',
            'intensity_500':     'DI500',
            'intensity_2km':     'DI2KM',
            'intensity_watts':   'DIWA',
            'intensity_cal_ph':  'DICH',
            'intensity_avg_mps': 'DAMS',
            'intensity_avg_mph': 'DAMPH',
            'intensity_avg_500': 'DA500',
            'intensity_avg_2km': 'DA2KM',
            'distance_meters':   'DDME',
            'distance_miles':    'DDMI',
            'distance_km':       'DDKM',
            'distance_strokes':  'DDST',}

MEMORY_MAP = {'055': {'type': 'total_distance_m',  'size': 'double', 'base': 16},
              '140': {'type': 'total_strokes',     'size': 'double', 'base': 16},
              '1A9': {'type': 'stroke_rate',       'size': 'single', 'base': 16},
              '08A': {'type': 'total_kcal',        'size': 'triple', 'base': 16},
              '14A': {'type': 'avg_distance_cmps', 'size': 'double', 'base': 16},
              '1E0': {'type': 'display_sec_dec',   'size': 'single', 'base': 10},
              '1E1': {'type': 'display_sec',       'size': 'single', 'base': 10},
              '1E2': {'type': 'display_min',       'size': 'single', 'base': 10},
              '1E3': {'type': 'display_hr',        'size': 'single', 'base': 10},}

SIZE_MAP = {'single': 'IRS',
            'double': 'IRD',
            'triple': 'IRT',}

# WORKOUT_MAP = {'distance': "WSI",
#                'duration': "WSU"}

UNIT_MAP = {'meters': 1,
            'miles': 2,
            'km': 3,
            'strokes': 4}

SIZE_PARSE_MAP = {'single': lambda cmd: cmd[6:8],
                  'double': lambda cmd: cmd[6:10],
                  'triple': lambda cmd: cmd[6:12]}

def build_daemon(target):
    t = threading.Thread(target=target)
    t.daemon = True
    return t

def build_event(type, value=None, raw=None):
    return {"type": type,
            "value": value,
            "raw": raw,
            "at": int(round(time.time() * 1000))}

def is_live_thread(t):
    return t and t.is_alive()

def read_reply(cmd):
    address = cmd[3:6]
    memory = MEMORY_MAP.get(address)
    if memory:
        size = memory['size']
        value_fn = SIZE_PARSE_MAP.get(size, lambda cmd: None)
        value = value_fn(cmd)
        if value == None:
            logging.error('unknown size: %s', size)
        else:
            return build_event(memory['type'], int(value, base=memory['base']), cmd)
    else:
        logging.error('cannot read reply for %s', cmd)

def event_from(line):
    try:
        cmd = line.strip()
        if cmd == 'SS':
            return build_event(type='stroke_start', raw=cmd)
        elif cmd == 'SE':
            return build_event(type='stroke_end', raw=cmd)
        elif cmd == 'OK':
            return build_event(type='ok', raw=cmd)
        elif cmd[:2] == 'IV':
            return build_event(type='model', raw=cmd)
        elif cmd[:3] == 'IDS':
            return read_reply(cmd)
        elif cmd[:3] == 'IDD':
            return read_reply(cmd)
        elif cmd[:3] == 'IDT':
            return read_reply(cmd)
        elif cmd == 'ERROR':
            return build_event(type='error', raw=cmd)
        else:
            return None
    except Exception as e:
        logging.error('could not build event for: %s %s', line, e)

###

class Rower(object):
    def __init__(self, serial):
        self._callbacks = set()
        self._request_thread = None
        self._capture_thread = None
        self._stop_event = None
        self._serial = serial

    def is_connected(self):
        return is_live_thread(self._request_thread) and \
            is_live_thread(self._capture_thread)

    def open(self):
        self._serial.open()
        self.write('start')

    def close(self):
        self.end_workout()
        time.sleep(0.1) # time for capture and request loops to stop running
        if self._serial and self._serial.isOpen():
            self._serial.close()

    def start_capturing(self):
        while not self._stop_event.is_set() and self._serial.isOpen():
            line = self._serial.readline()
            event = event_from(line)
            if event:
                self.notify_callbacks(event)

    def start_requesting(self):
        while not self._stop_event.is_set() and self._serial.isOpen():
            for address in MEMORY_MAP:
                size = MEMORY_MAP[address]['size']
                cmd = SIZE_MAP[size]
                self.write(cmd + address)
            self._stop_event.wait(0.025)

    def begin_distance_workout(self, distance):
        self._stop_event = threading.Event()
        self._request_thread = build_daemon(target=self.start_requesting)
        self._capture_thread = build_daemon(target=self.start_capturing)
        units = UNIT_MAP['meters'] # TODO support others in UI
        hex_distance = hex(distance).split('x')[1].rjust(4, '0')
        command = 'WSI{0}{1}'.format(units, hex_distance) 
        logging.info('sending reset and workout command: %s', command)
        self.write('reset')
        self.write(command)
        self._request_thread.start()
        self._capture_thread.start()

    def write(self, cmd):
        raw = COMMANDS.get(cmd, cmd)
        self._serial.write(raw.upper() + '\r\n')
        self._serial.flush()

    def end_workout(self):
        if self._stop_event:
            self._stop_event.set()

    def register_callback(self, cb):
        self._callbacks.add(cb)

    def remove_callback(self, cb):
        self._callbacks.remove(cb)

    def notify_callbacks(self, event):
        for cb in self._callbacks:
            cb(event)



