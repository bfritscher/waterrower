(ns rower.www
  (:require [cheshire.core :as json]
            [rower.s4 :as s4]
            [rower.s4-stub :as s4-stub])
  (:import (org.webbitserver WebServer WebServers WebSocketHandler)
           (org.webbitserver.handler StaticFileHandler)
           (java.io FileWriter PrintWriter)
           (java.util Date)
           (java.text SimpleDateFormat)))

(def current-workout (atom nil))

(defn send-event
  [conn event]
  (.send conn (json/encode event)))

(defn build-filename
  []
  (str "data/" (.format (SimpleDateFormat. "yyyy-MM-dd_H_m") (Date.)) ".cap"))

(defn new-workout
  [distance dev?]
  (if dev?
    (s4-stub/new-workout)
    (s4/new-workout {:path "/dev/tty.usbmodemfd121"
                     :baud 19200}
                    {:type  :distance
                     :units :meters
                     :value distance})))

(defmulti handle (fn [_ data _] (:type data)))

(defmethod handle "start-workout"
  [conn {distance :distance} {dev? :dev?}]
  (when @current-workout
    (println "closing existing workout")
    (s4/close @current-workout))
  (println "starting workout")
  (let [workout (new-workout distance dev?)]
    (when-not dev?
      (let [f (-> (build-filename) FileWriter. PrintWriter.)]
        (s4/add-handler workout #(.println f (json/encode %)))))
    (reset! current-workout workout)
    (s4/add-handler workout (partial send-event conn))

    (s4/start workout)))

(defmethod handle "stop-workout"
  [conn _ _]
  (println "stopping workout")
  (when @current-workout
    (s4/close @current-workout)
    (reset! current-workout nil)))

(defn run-webbit
  [opts]
  (doto (WebServers/createWebServer 3000)
    (.add "/rower"
          (proxy [WebSocketHandler] []
            (onOpen [_])
            (onClose [_] )
            (onMessage [conn data]
              (let [data (json/decode data true)]
                (println "received:" data)
                (handle conn data opts)))))
    (.add (StaticFileHandler. "./resources/public"))
    (.start)))