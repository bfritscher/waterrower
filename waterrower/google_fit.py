import os
from oauth2client import client, tools, file
from googleapiclient.discovery import build
from googleapiclient.http import build_http
import logging


def build_fit_service():
    filename = "json/"
    scope = [
        "https://www.googleapis.com/auth/fitness.activity.read",
        "https://www.googleapis.com/auth/fitness.activity.write",
        "https://www.googleapis.com/auth/fitness.location.read",
        "https://www.googleapis.com/auth/fitness.location.write",
    ]
    # Name of a file containing the OAuth 2.0 information for this
    # application, including client_id and client_secret, which are found
    # on the API Access tab on the Google APIs
    # Console <http://code.google.com/apis/console>.
    client_secrets = os.path.join(
        os.path.dirname(filename), "client_secrets.json")

    # Set up a Flow object to be used if we need to authenticate.
    flow = client.flow_from_clientsecrets(
        client_secrets, scope=scope, message=tools.message_if_missing(
            client_secrets)
    )

    # Prepare credentials, and authorize HTTP object with them.
    # If the credentials don't exist or are invalid run through the native client
    # flow. The Storage object will ensure that if successful the good
    # credentials will get written back to a file.
    storage = file.Storage(os.path.join(
        os.path.dirname(filename), "token.json"))
    credentials = storage.get()
    if credentials is None or credentials.invalid:
        # maybe add argv to support --noauth_local_webserver
        credentials = tools.run_flow(flow, storage)
    http = credentials.authorize(http=build_http())

    fit_service = build(serviceName='fitness', version='v1', http=http)
    return fit_service


def post_activity(activity):
    fit_service = build_fit_service()
    start_time = activity['start_time'] * 1000000
    end_time = activity['end_time'] * 1000000
    # TODO check that datasources exists?
    dataSourceIdPostFix = "197772635046:waterrower:S4:1"
    datasetId = "%s-%s" % (start_time, end_time)
    sessionId = "ergo_%s" % activity['start_time']
    name = "Ergo rowing"
    description = ""

    dataSourceId = "raw:com.google.activity.segment:%s" % dataSourceIdPostFix
    body = {
        "dataSourceId": dataSourceId,
        "maxEndTimeNs": end_time,
        "minStartTimeNs": start_time,
        "point": [
            {
                "dataTypeName": "com.google.activity.segment",
                "endTimeNanos": end_time,
                "startTimeNanos": start_time,
                "value": [
                    {
                        "intVal": 103  # Ergometer https://developers.google.com/fit/rest/v1/reference/activity-types
                    }
                ]
            }
        ]
    }

    out = fit_service.users().dataSources().datasets().patch(userId="me",
                                                             dataSourceId=dataSourceId,
                                                             datasetId=datasetId, body=body).execute()
    logging.debug(out)
    dataSourceId = "raw:com.google.distance.delta:%s" % dataSourceIdPostFix
    body = {
        "dataSourceId": dataSourceId,
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

    out = fit_service.users().dataSources().datasets().patch(userId="me",
                                                             dataSourceId=dataSourceId,
                                                             datasetId=datasetId, body=body).execute()
    logging.debug(out)

    body = {
        "activityType": 103,
        "application": {
            "name": "waterrower"
        },
        "endTimeMillis":  activity['end_time'],
        "id": sessionId,
        "name": name,
        "description": description,
        "startTimeMillis": activity['start_time']
    }
    out = fit_service.users().sessions().update(userId="me", sessionId=sessionId,
                                                body=body).execute()
    logging.debug(out)


if __name__ == '__main__':
    print(build_fit_service())
