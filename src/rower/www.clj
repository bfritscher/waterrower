(ns rower.www
  (:require [cheshire.core :as json]
            [rower.s4 :as s4]
            [rower.s4-stub :as s4-stub])
  (:import (org.webbitserver WebServer WebServers WebSocketHandler)
           (org.webbitserver.handler StaticFileHandler)
           (java.io FileWriter PrintWriter)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn send-event
  [conn event]
  (.send conn (json/encode event)))

(defn build-filename
  []
  (str "data/" (.format (SimpleDateFormat. "yyyy-MM-dd_H_m") (Date.)) ".cap"))

(defn ->workout
  [m]
  (into {} (map (fn [[k v]] [k (if (contains? #{:units :type} k) (keyword v) v)]) m)))

(defmulti handle (fn [_ data _] (:type data)))

(defmethod handle "start-workout"
  [conn msg port]
  (let [f (-> (build-filename) FileWriter. PrintWriter.)]
    (s4/add-handler #(.println f (json/encode %))))
  (s4/add-handler (partial send-event conn))
  (s4/start port (->workout (:data msg))))

(defn run-webbit
  [port]
  (doto (WebServers/createWebServer 3000)
    (.add "/rower"
          (proxy [WebSocketHandler] []
            (onOpen [_])
            (onClose [_] )
            (onMessage [conn msg]
              (let [msg (json/decode msg true)]
                (println "received:" msg)
                (handle conn msg port)))))
    (.add (StaticFileHandler. "./resources/public"))
    (.start)))