# -*- coding: utf-8 -*-
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
import signal
import sys
import datetime

from tornado.options import define, options

from logger import DataLogger

define("port", default=8000, help="port to run on", type=int)
define("demo", default=False, help="stub connection to rower", type=bool)
define("debug", default=False, help="run in debug mode", type=bool)


class Application(tornado.web.Application):
    def __init__(self, rower_interface):
        routes = [
            (r"/ws", handlers.DashboardWebsocketHandler, dict(rower_interface=rower_interface)),
            (r"/(.*.html)", handlers.TemplateHandler),
            (r"/", handlers.TemplateHandler),
            ]
        settings = {
            'template_path': os.path.join(os.path.dirname(__file__), "templates"),
            'static_path': os.path.join(os.path.dirname(__file__), "static"),
            'debug': options.debug
            }
        tornado.web.Application.__init__(self, routes, **settings)


def build_cleanup(rower_interface):
    def cleanup(signum, frame):
        logging.info("cleaning up")
        rower_interface.close()
        sys.exit(0)
    return cleanup


def main():
    tornado.options.parse_command_line()
    rower_interface = interface.Rower(options)
    #TODO allow to load history of logger?
    DataLogger(rower_interface)
    cleanup = build_cleanup(rower_interface)
    signal.signal(signal.SIGINT, cleanup)
    rower_interface.open()
    http_server = tornado.httpserver.HTTPServer(Application(rower_interface))
    http_server.listen(options.port)
    tornado.ioloop.IOLoop.instance().start()

if __name__ == "__main__":
    main()

