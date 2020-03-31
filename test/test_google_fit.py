import unittest
import time
import logging
from waterrower.google_fit import build_fit_service, post_activity

class TestGoogleFit(unittest.TestCase):
    def test_build_fit_service(self):
        pass

    def test_post_activity(self):
        logging.basicConfig(level=logging.DEBUG)
        end = int(round(time.time() * 1000))
        start = end - 1000000
        activity = {
            'start_time': start,
            'end_time': end,
            'total_distance_m': 1000.0,
        }
        try:
            post_activity(activity)
        except Exception as e:
            self.fail(e)

if __name__ == '__main__':
    unittest.main()
