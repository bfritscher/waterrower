(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower.s4      :as s4]
            [rower.s4-stub :as s4-stub]
            [rower.stdout  :as stdout]
            [rower.www     :as www]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.util Date)
           (java.text SimpleDateFormat))
  (:gen-class))

(defn write-file
  [f event]
  (.println f (json/encode event))
  (.flush f))

(defn build-filename
  [dev?]
  (str "data/" (if dev? "dev/" "") (.format (SimpleDateFormat. "yyyy-MM-dd_H_m") (Date.)) ".cap"))

(defn -main
  [& args]
  (let [[options _ banner] (cli args
                                ["--dev" "run stub rower" :flag true :default false]
                                ["--ui" "console or web"
                                 :default :console
                                 :parse-fn keyword])]
    (with-open [f (-> (build-filename (:dev options))
                      java.io.File.
                      io/writer
                      java.io.PrintWriter.)
                workout (if (:dev options)
                          (s4-stub/new-workout)
                          (s4/new-workout {:path "/dev/tty.usbmodemfd121"
                                           :baud 19200}
                                          {:type  :distance
                                           :units :meters
                                           :value 8000}))]
      (s4/add-handler workout (partial write-file f))
      (www/run-webbit workout)
      (.join (Thread/currentThread)))))