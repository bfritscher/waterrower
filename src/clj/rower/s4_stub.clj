(ns rower.s4-stub
  (:use [rower.s4 :only [event S4Monitor]]))

(def handlers (atom []))

(defn start-publishing
  []
  (loop [i 1
         m 0]
    (doseq [h @handlers]
      (h (event :total-distance-m m))
      (h (event :display-sec (mod (int (/ i 10)) 60)))
      (h (event :display-sec-dec (mod i 10)))
      (h (event :display-min (int (/ i 600))))
      (h (event :avg-distance-cmps (int (* 100 (/ m (/ i 10)))))))
    (Thread/sleep 100)
    (recur (inc i)
           (if (zero? (mod i 2)) (inc m) m))))

(defn new-stub-s4
  []
  (let [start-fn (delay (future (start-publishing)))]
    (reify S4Monitor
      (clear-handlers [_]
        (reset! handlers []))
      (add-handler    [_ handler]
        (swap! handlers conj handler))
      (start-workout  [_ workout]
        @start-fn)
      (send-command   [_ command])
      (close          [_]
        (future-cancel @start-fn)))))
