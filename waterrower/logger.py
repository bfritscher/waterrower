# -*- coding: utf-8 -*-
import json
import threading
import copy
import time
import io
import logging

IGNORE_LIST = ['graph', 'tank_volume', 'display_hr', 'display_min', 'display_sec', 'display_sec_dec']


class DataLogger(object):
    def __init__(self, rower_interface):
        self._rower_interface = rower_interface
        self._rower_interface.register_callback(self.on_rower_event)
        self._event = {}
        self._events = []
        self._activity = None
        self._stop_event = threading.Event()

    def on_rower_event(self, event):
        if event['type'] in IGNORE_LIST:
            return
        if event['value']:
            self._event[event['type']] = event['value']

        if event['type'] == 'workout-start':
            self.end_activity()
            self.start_activity()

        if event['type'] == 'reset':
            self.end_activity()

        if event['type'] == 'exit':
            self.end_activity()

        #TODO allow to get history?

    def start_activity(self):
        self._stop_event = threading.Event()
        self._activity = {
            'start_time': int(round(time.time() * 1000))
        }
        self._event = {}
        self._events = []
        thread = threading.Thread(target=self._save_event)
        thread.daemon = True
        thread.start()

    def end_activity(self):
        self._stop_event.set()
        if self._activity:
            self._activity['end_time'] = int(round(time.time() * 1000))
            self._activity['total_strokes'] = 0
            self._activity['total_distance_m'] = 0
            if 'total_strokes' in self._event:
                self._activity['total_strokes'] = self._event['total_strokes']
            if 'total_distance_m' in self._event:
                self._activity['total_distance_m'] = self._event['total_distance_m']
            # TODO add global avg
            # TODO add max?

            with io.open('json/event_%s.json' % self._activity['start_time'], 'w', encoding='utf-8') as f:
                f.write(unicode(json.dumps(self._events, ensure_ascii=False)))
            activities = None
            try:
                with open('json/workouts.json') as data_file:
                    activities = json.load(data_file)
            except:
                pass
            if activities is None:
                activities = []
            activities.append(self._activity)
            with io.open('json/workouts.json', 'w', encoding='utf-8') as f:
                f.write(unicode(json.dumps(activities, ensure_ascii=False)))

            save_to_google_fit(self._activity)

            self._activity = None

    def _save_event(self):
        while not self._stop_event.is_set():
            #TODO keep track of max?
            #TODO keep track of history?
            #TODO reset to 0 if not received since...
            self._event['time'] = int(round(time.time() * 1000))
            self._event['elapsed'] = self._event['time'] - self._activity['start_time']
            self._events.append(copy.deepcopy(self._event))
            self._rower_interface.notify_callbacks({
                "type": "graph",
                "value": self._event,
                "raw": None,
                "at": self._event['time']
            })
            self._stop_event.wait(2)


def save_to_google_fit(activity):
    try:
        with open('json/config.json') as data_file:
            config = json.load(data_file)
            from oauth2client import GOOGLE_REVOKE_URI
            from oauth2client import GOOGLE_TOKEN_URI
            from oauth2client.client import OAuth2Credentials
            from apiclient import discovery
            import httplib2

            credentials = OAuth2Credentials(None, config['CLIENT_ID'],
                               config['CLIENT_SECRET'], config['REFRESH_TOKEN'], None,
                               GOOGLE_TOKEN_URI, None,
                               revoke_uri=GOOGLE_REVOKE_URI,
                               id_token=None,
                               token_response=None)
            http_auth = credentials.authorize(httplib2.Http())
            fit_service = discovery.build('fitness', 'v1', http_auth)
            #TODO check that datasources exists

            start_time = activity['start_time'] * 1000000
            end_time = activity['end_time'] * 1000000

            body = {
             "dataSourceId": "raw:com.google.activity.segment:197772635046:waterrower:S4:1",
             "maxEndTimeNs": end_time,
             "minStartTimeNs": start_time,
             "point": [
              {
               "dataTypeName": "com.google.activity.segment",
               "endTimeNanos": end_time,
               "startTimeNanos": start_time,
               "value": [
                {
                 "intVal": 103
                }
               ]
              }
             ]
            }

            print fit_service.users().dataSources().datasets().patch(userId="me",
                    dataSourceId="raw:com.google.activity.segment:197772635046:waterrower:S4:1",
                    datasetId="%s-%s" % (start_time, end_time), body=body).execute()

            body = {
             "dataSourceId": "raw:com.google.distance.delta:197772635046:waterrower:S4:1",
             "maxEndTimeNs": end_time,
             "minStartTimeNs": start_time,
             "point": [
              {
               "dataTypeName": "com.google.distance.delta",
               "endTimeNanos": end_time,
               "startTimeNanos": start_time,
               "value": [
                {
                 "fpVal": activity['total_distance_m']
                }
               ]
              }
             ]
            }

            print fit_service.users().dataSources().datasets().patch(userId="me",
                    dataSourceId="raw:com.google.distance.delta:197772635046:waterrower:S4:1",
                    datasetId="%s-%s" % (start_time, end_time), body=body).execute()

            body = {
             "activityType": 103,
             "application": {
              "name": "waterrower"
             },
             "endTimeMillis":  activity['end_time'],
             "id": "ergo_%s" % activity['start_time'],
             "name": "Ergo rowing",
             "startTimeMillis": activity['start_time']
            }
            print fit_service.users().sessions().update(userId="me", sessionId="ergo_%s" % activity['start_time'],
                                                        body=body).execute()

    except Exception as e:
        logging.error(e)
