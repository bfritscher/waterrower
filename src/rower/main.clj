(ns rower.main
  (:require [rower.s4    :as s4]
            [rower.stats :as stats]))

(defn start-publishing
  [events]
  (stats/start println)
  (doseq [e events]
    (stats/handle e)))

(defn handle-events
  [events]
  (loop [events events]
    (println (first events))
    (if (contains? #{:ping :unknown} (:type (first events)))
      (recur (rest events))
      (start-publishing events))))

(defn -main
  [& args]
  (println "starting up")
  (s4/with-events handle-events))