(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower.s4     :as s4]
            [rower.stats  :as stats]
            [rower.stdout :as stdout]
            [cheshire.core :as json])
  (:gen-class))

(defn start-publishing
  [handler events]
  (stats/start handler)
  (doseq [e events]
    (stats/handle e)))

(defn handle-events
  [handler events]
  (loop [events events]
    (if (contains? #{:ping :unknown} (:type (first events)))
      (recur (rest events))
      (start-publishing handler events))))

(defn -main
  [& args]
  (let [[options _ banner] (cli args
                                ["--ui" "console or web"
                                 :default :console
                                 :parse-fn keyword])]
    (s4/initialize! {:path "/dev/tty.usbmodemfd121"
                     :baud 19200})
    (s4/start {:type  :distance
               :units :km
               :value 8} println)))