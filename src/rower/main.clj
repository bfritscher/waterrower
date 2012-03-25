(ns rower.main
  (:use [clojure.tools.cli :only [cli]])
  (:require [rower
             [s4      :as s4]
             [s4-stub :as s4-stub]
             [www     :as www]]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:gen-class))

(defn -main
  [& args]
  (let [dev? (= "-dev" (first args))]
    (with-open [s4-mon (if dev?
                         (s4-stub/new-stub-s4)
                         (s4/new-s4monitor "/dev/tty.usbmodemfd121"))]
      (www/run-webbit s4-mon dev?)
      (.join (Thread/currentThread)))))