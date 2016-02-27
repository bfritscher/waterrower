import os.path
import json
import logging
import tornado.httpserver
import tornado.ioloop
import tornado.options
import tornado.web
import tornado.websocket
import handlers
import interface
import serial
import serial.tools.list_ports
import signal
import sys
import datetime

from tornado.options import define, options

define("port", default=8000, help="port to run on", type=int)
define("demo", default=False, help="stub connection to rower", type=bool)
define("debug", default=False, help="run in debug mode", type=bool)
define("log", default=False, help="log events to stdout", type=bool)

class Application(tornado.web.Application):
    def __init__(self, rower_interface):
        routes = [
            (r"/ws", handlers.DashboardWebsocketHandler, dict(rower_interface=rower_interface)),
            (r"/(.*.html)", handlers.TemplateHandler),
            ]
        settings = {
            'template_path': os.path.join(os.path.dirname(__file__), "templates"),
            'static_path': os.path.join(os.path.dirname(__file__), "static"),
            'debug': options.debug
            }
        tornado.web.Application.__init__(self, routes, **settings)

def ask_for_port():
    print "Choose a port to use:"
    ports = serial.tools.list_ports.comports()
    for (i, (path, _, _)) in enumerate(ports):
        print "%s. %s" % (i, path)
    result = raw_input()
    return ports[int(result)][0]

def connect_to_rower():
    logging.info('connecting to rower')
    serial_port = None
    if options.demo:
        from demo import FakeS4
        serial_port = FakeS4()
    else:
        serial_port = serial.Serial()
        serial_port.port = ask_for_port()
        serial_port.baudrate = 19200
    return interface.Rower(serial_port)

def log_event(file):
    def f(e):
        file.write(json.dumps(e) + '\n')
    return f

def build_cleanup(rower_interface):
    def cleanup(signum, frame):
        logging.info("cleaning up")
        rower_interface.close()
        sys.exit(0)
    return cleanup

def main():
    tornado.options.parse_command_line()
    rower_interface = connect_to_rower()
    rower_interface.open()
    cleanup = build_cleanup(rower_interface)
    signal.signal(signal.SIGINT, cleanup)
    if options.log:
        event_log = "./event_log_{0}.json".format(datetime.datetime.now().strftime("%Y-%m-%d_%H%M%S"))
        file = open(event_log, os.WRONLY)
        rower_interface.register_callback(log_event(file))
    http_server = tornado.httpserver.HTTPServer(Application(rower_interface))
    http_server.listen(options.port)
    tornado.ioloop.IOLoop.instance().start()

if __name__ == "__main__":
    main()

