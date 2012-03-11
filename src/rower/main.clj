(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower.s4     :as s4]
            [rower.stats  :as stats]
            [rower.stdout :as stdout]
            [cheshire.core :as json])
  (:gen-class))

(defn -main
  [& args]
  (let [[options _ banner] (cli args
                                ["--ui" "console or web"
                                 :default :console
                                 :parse-fn keyword])]
    (with-open [workout (s4/new-workout {:path "/dev/tty.usbmodemfd121"
                                         :baud 19200}
                                        {:type  :distance
                                         :units :meters
                                         :value 100})]
      (s4/start workout println)
      (.join (Thread/currentThread)))))