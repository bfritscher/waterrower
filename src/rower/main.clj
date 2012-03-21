(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower.s4      :as s4]
            [rower.s4-stub :as s4-stub]
            [rower.stdout  :as stdout]
            [rower.www     :as www]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [serial-port     :as sp])
  (:gen-class))

(defn -main
  [& args]
  (with-open [port (sp/open "/dev/tty.usbmodemfd121" 19200)]
    (www/run-webbit port)
    (.join (Thread/currentThread))))