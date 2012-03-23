(ns rower.www
  (:require [cheshire.core :as json]
            [rower.s4 :as s4])
  (:import (org.webbitserver WebServer WebServers WebSocketHandler)
           (org.webbitserver.handler StaticFileHandler)
           (java.io FileWriter PrintWriter)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn build-filename
  [{value :value units :units}]
  (str "data/" (.format (SimpleDateFormat. "yyyy-MM-dd_H_m") (Date.))
       value (name units) ".cap"))

(defn ->workout
  [m]
  (into {} (map (fn [[k v]] [k (if (contains? #{:units :type} k) (keyword v) v)]) m)))

(defmulti handle (fn [_ data _] (:type data)))

(defn on-event
  [conn file event]
  (let [event (json/encode event)]
    (.println file event)
    (.flush file)
    (.send conn event)))

(defmethod handle "start-workout"
  [conn msg s4-mon]
  (s4/clear-handlers s4-mon)
  (let [workout (->workout (:data msg))
        file    (-> (build-filename workout) FileWriter. PrintWriter.)]
    (s4/add-handler s4-mon (partial on-event conn file))
    (s4/start-workout s4-mon workout)))

(defn run-webbit
  [s4-mon]
  (doto (WebServers/createWebServer 3000)
    (.add "/rower"
          (proxy [WebSocketHandler] []
            (onOpen [_])
            (onClose [_] )
            (onMessage [conn msg]
              (let [msg (json/decode msg true)]
                (println "received:" msg)
                (handle conn msg s4-mon)))))
    (.add (StaticFileHandler. "./resources/public"))
    (.start)))