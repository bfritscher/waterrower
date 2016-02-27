# -*- coding: utf-8 -*-
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
              '088': {'type': 'total_watts', 'size': 'double', 'base': 16},
              '140': {'type': 'total_strokes',     'size': 'double', 'base': 16},
              '1A9': {'type': 'stroke_rate',       'size': 'single', 'base': 16},
              '08A': {'type': 'total_kcal',        'size': 'triple', 'base': 16},
              '14A': {'type': 'avg_distance_cmps', 'size': 'double', 'base': 16},
              '148': {'type': 'total_speed_cmps', 'size': 'double', 'base': 16},
              '1E0': {'type': 'display_sec_dec',   'size': 'single', 'base': 10},
              '1E1': {'type': 'display_sec',       'size': 'single', 'base': 10},
              '1E2': {'type': 'display_min',       'size': 'single', 'base': 10},
              '1E3': {'type': 'display_hr',        'size': 'single', 'base': 10},
              # from zone math
              '1A0': {'type': 'heartrate',        'size': 'double',  'base': 16},
              '1A2': {'type': 'cmps',        'size': 'double',  'base': 16},
              '1A6': {'type': '500mps',        'size': 'double',  'base': 16},
              # explore
              '047': {'type': 'stroke_rate2',        'size': 'double',  'base': 16},
              '057': {'type': 'distance',        'size': 'double',  'base': 16},
              '0A9': {'type': 'tank_volume',        'size': 'double',  'base': 16},
              '05B': {'type': 'clock_countdown',        'size': 'double',  'base': 16},
              '142': {'type': 'avg_time_stroke_whole',        'size': 'double',  'base': 16},
              '143': {'type': 'avg_time_stroke_pull',        'size': 'double',  'base': 16},
              }


# ACH values = Ascii coded hexadecimal
# REQUEST sent from PC to device
# RESPONSE sent from device to PC

USB_REQUEST = "USB"                      # Application starting communication’s
WR_RESPONSE = "_WR_"                     # Hardware Type, Accept USB start sending packets
EXIT_REQUEST = "EXIT"  # Application is exiting, stop sending packets
OK_RESPONSE = "OK"        # Packet Accepted
ERROR_RESPONSE = "ERROR"  # Unknown packet
PING_RESPONSE = "PING"    # Ping
RESET_REQUEST = "RESET" # Request the rowing computer to reset, disable interactive mode
MODEL_INFORMATION_REQUEST = "IV?"   # Request Model Information
MODEL_INFORMATION_RESPONSE = "IV"    # Current model information IV + Model + Version High + Version Low
READ_MEMORY_REQUEST = "IR"    # Read a memory location IR+(S=Single,D=Double,T=Triple) + XXX
READ_MEMORY_RESPONSE = "ID"    # Value from a memory location ID +(type) + Y3 Y2 Y1
STROKE_START_RESPONSE = "SS"    # Start of stroke
STROKE_END_RESPONSE = "SE"    # End of stroke
PULSE_COUNT_RESPONSE = "P"  # Pulse Count XX in the last 25mS, ACH value

# Display Settings (not used)
DISPLAY_SET_INTENSITY_MPS_REQUEST = "DIMS"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_MPH_REQUEST = "DIMPH"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_500M_REQUEST = "DI500"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_2KM_REQUEST = "DI2KM"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_WATTS_REQUEST = "DIWA"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_CALHR_REQUEST = "DICH"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_AVG_MPS_REQUEST = "DAMS"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_AVG_MPH_REQUEST = "DAMPH"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_AVG_500M_REQUEST = "DA500"    # Display: Set Intensity
DISPLAY_SET_INTENSITY_AVG_2KM_REQUEST = "DA2KM"    # Display: Set Intensity
DISPLAY_SET_DISTANCE_METERS_REQUEST = "DDME"    # Display: Set Distance
DISPLAY_SET_DISTANCE_MILES_REQUEST = "DDMI"    # Display: Set Distance
DISPLAY_SET_DISTANCE_KM_REQUEST = "DDKM"    # Display: Set Distance
DISPLAY_SET_DISTANCE_STROKES_REQUEST = "DDST"    # Display: Set Distance

# Interactive mode

INTERACTIVE_MODE_START_RESPONSE = "AIS"  # interactive mode requested by device
INTERACTIVE_MODE_START_ACCEPT_REQUEST = "AIA"  # confirm interactive mode, key input is redirect to PC
INTERACTIVE_MODE_END_REQUEST = "AIE"  # cancel interactive mode
INTERACTIVE_KEYPAD_RESET_RESPONSE = "AKR"  # RESET key pressed, interactive mode will be cancelled
INTERACTIVE_KEYPAD_UNITS_RESPONSE = "AK1"  # Units button pressed
INTERACTIVE_KEYPAD_ZONES_RESPONSE = "AK2"  # Zones button pressed
INTERACTIVE_KEYPAD_WORKOUT_RESPONSE = "AK3"  # Workout button pressed
INTERACTIVE_KEYPAD_UP_RESPONSE = "AK4"  # Up arrow button pressed
INTERACTIVE_KEYPAD_OK_RESPONSE = "AK5"  # Ok button pressed
INTERACTIVE_KEYPAD_DOWN_RESPONSE = "AK6"  # Down arrow button pressed
INTERACTIVE_KEYPAD_ADVANCED_RESPONSE = "AK7"  # Advanced button pressed
INTERACTIVE_KEYPAD_STORED_RESPONSE = "AK8"  # Stored Programs button pressed
INTERACTIVE_KEYPAD_HOLD_RESPONSE = "AK9"  # Hold/cancel button pressed

# Workout
WORKOUT_SET_DISTANCE_REQUEST = "WSI"   # Define a distance workout + x(unit, 1-4) + YYYY = ACH
WORKOUT_SET_DURATION_REQUEST = "WSU"   # Define a duration workout + YYYY = ACH seconds
WORKOUT_INTERVAL_START_SET_DISTANCE_REQUEST = "WII"  # Define an interval distance workout
WORKOUT_INTERVAL_START_SET_DURATION_REQUEST = "WIU"  # Define an interval duration workout
WORKOUT_INTERVAL_ADD_END_REQUEST         = "WIN"   # Add/End an interval to a workout XXXX(==FFFFF to end) + YYYY

# UNITS
UNIT_METERS = 1
UNIT_MILES = 2
UNIT_KM = 3
UNIT_STROKES = 4

# case 'S': // SS
# 		if s4.workout.state == ResetPingReceived {
# 			s4.workout.state = WorkoutStarted
# 			// these are the things we want captured from the S4
# 			for address, mmap := range g_memorymap {
# 				s4.readMemoryRequest(address, mmap.size)
# 			}
# 		}
# 		s4.aggregator.consume(AtomicEvent{
# 			Time:  millis(),
# 			Label: "stroke_start",
# 			Value: 1})
# 	case 'E': // SE
# 		s4.aggregator.consume(AtomicEvent{
# 			Time:  millis(),
# 			Label: "stroke_end",
# 			Value: 0})
# 	}


# // responses can start with:
# 	// _ : _WR_
# 	// O : OK
# 	// E : ERROR
# 	// P : PING, P
# 	// S : SS, SE
# 	c := b[0]
# 	switch c {
# 	case '_':
# 		s4.wRHandler(b)
# 	case 'I':
# 		s4.informationHandler(b)
# 	case 'O':
# 		s4.oKHandler()
# 	case 'E':
# 		s4.errorHandler()
# 	case 'P':
# 		s4.pingHandler(b)
# 	case 'S':
# 		s4.strokeHandler(b)
# 	default:
# 		jww.INFO.Printf("Unrecognized packet: %s", string(b))
# 	}

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
        if value is None:
            logging.error('unknown size: %s', size)
        else:
            return build_event(memory['type'], int(value, base=memory['base']), cmd)
    else:
        logging.error('cannot read reply for %s', cmd)

def event_from(line):
    try:
        cmd = line.strip()
        if cmd == STROKE_START_RESPONSE:
            return build_event(type='stroke_start', raw=cmd)
        elif cmd == STROKE_END_RESPONSE:
            return build_event(type='stroke_end', raw=cmd)
        elif cmd == OK_RESPONSE:
            return build_event(type='ok', raw=cmd)
        elif cmd[:2] == MODEL_INFORMATION_RESPONSE:
            return build_event(type='model', raw=cmd)
        elif cmd[:2] == READ_MEMORY_RESPONSE:
            return read_reply(cmd)
        elif cmd[:1] == PULSE_COUNT_RESPONSE:
            ##TODO handle pulse event
            return build_event(type='pulse', raw=cmd)
        elif cmd == ERROR_RESPONSE:
            return build_event(type='error', raw=cmd)
        else:
            #ignore
            #WR_RESPONSE
            #PING_RESPONSE
            #AND INTERACTIVE_MODE
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



