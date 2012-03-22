(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower.s4      :as s4]
            [rower.www     :as www]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:gen-class))

(defn -main
  [& args]
  (with-open [s4-mon (s4/new-s4monitor "/dev/tty.usbmodemfd121")]
    (www/run-webbit s4-mon)
    (.join (Thread/currentThread))))