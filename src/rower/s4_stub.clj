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
  (let [handlers (atom [])]
   (reify s4/IWorkout
     (s4/add-handler [_ handler]
       (swap! handlers conj handler))
     (s4/start [this]
       (s4/spawn
        (let [start-ms (System/currentTimeMillis)]
          (loop [distance-m 0.0]
            (doseq [h @handlers]
              (h (s4/event :total-distance-m (int distance-m)))
              (h (s4/event :total-distance-dec (int (* 10 (- distance-m (int distance-m))))))
              (h (s4/event :display-min (mins start-ms)))
              (h (s4/event :display-sec (sec start-ms)))
              (h (s4/event :display-sec-dec (sec-dec start-ms)))
              (h (s4/event :stroke-rate (int (+ 25 (rand 5)))))
              (h (s4/event :avg-distance-cmps (int (+ 400 (rand 20)))))
              (h (s4/event :total-kcal (int (* 1000 distance-m)))))
            (Thread/sleep 100)
            (recur (+ distance-m 0.2))))))
     (s4/close [this]))))

