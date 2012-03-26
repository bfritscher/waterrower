(ns rower.main
  (:require
   [rower
    [s4      :as s4]
    [s4-stub :as s4-stub]
    [www     :as www]]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as string])
  (:gen-class))

(defn new-s4
  [dev? path]
  (if dev?
    (s4-stub/new-stub-s4)
    (s4/new-serial-s4 path)))

(defn -main
  [& args]
  (when (= "-dev" (first args))
    (reset! dev? true))
  (with-open [s4-mon (new-s4 dev? "/dev/tty.usbmodemfd121")]
    (www/start s4-mon)
    (.join (Thread/currentThread))))