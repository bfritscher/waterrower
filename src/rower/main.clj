(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower.s4      :as s4]
            [rower.s4-stub :as s4-stub]
            [rower.stdout  :as stdout]
            [rower.www     :as www]
            [cheshire.core :as json]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main
  [& args]
  (let [[options _ banner] (cli args
                                ["--dev" "run stub rower" :flag true :default false]
                                ["--ui" "console or web"
                                 :default :console
                                 :parse-fn keyword])]
    (with-open [workout (if (:dev options)
                          (s4-stub/new-workout)
                          (s4/new-workout {:path "/dev/tty.usbmodemfd121"
                                           :baud 19200}
                                          {:type  :distance
                                           :units :meters
                                           :value 100}))]
      (www/run-webbit workout)
      (.join (Thread/currentThread)))))