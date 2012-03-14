(ns rower.s4-stub
  (:require [rower.s4 :as s4]))

(defn mins
  [from]
  (int (mod (/ (/ (- (System/currentTimeMillis) from) 1000) 60) 60)))

(defn sec
  [from]
  (int (mod (/ (- (System/currentTimeMillis) from) 1000) 60)))

(defn sec-dec
  [from]
  (int (/ (mod (- (System/currentTimeMillis) from) 1000) 100)))

(defn new-workout
  []
  (reify s4/IWorkout
    (s4/start [this on-event]
      (s4/spawn
       (let [start-ms (System/currentTimeMillis)]
         (loop [distance-m 0.0]
           (on-event (s4/event :total-distance-m (int distance-m)))
           (on-event (s4/event :total-distance-dec (int (* 10 (- distance-m (int distance-m))))))
           (on-event (s4/event :display-min (mins start-ms)))
           (on-event (s4/event :display-sec (sec start-ms)))
           (on-event (s4/event :display-sec-dec (sec-dec start-ms)))
           (on-event (s4/event :stroke-rate (int (+ 25 (rand 5)))))
           (on-event (s4/event :avg-distance-cmps (int (+ 400 (rand 20)))))
           (Thread/sleep 100)
           (recur (+ distance-m 0.2))))))
    (s4/close [this])))

