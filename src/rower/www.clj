(ns rower.www
  (:require [cheshire.core :as json]
            [rower
             [s4 :as s4]
             [views :as views]])
  (:import (org.webbitserver WebServer WebServers WebSocketHandler
                             HttpHandler)
           (org.webbitserver.handler StaticFileHandler)
           (java.io FileWriter PrintWriter)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn reload-all-ns
  []
  (doseq [ns-sym (->> (all-ns)
                      (map str)
                      (filter #(re-find #"^rower\.*" %))
                      (map symbol))]
    (require ns-sym :reload)))

(defn time-str
  []
  (.format (SimpleDateFormat. "yyyy-MM-dd_H_m") (Date.)))

(defn build-filename
  [{value :value units :units}]
  (str "data/" (time-str) "_" value "_" (name units) ".cap"))

(defn ->workout
  [m]
  (let [maybe-keywordize-value #(if (contains? #{:units :type} %1)
                                  (keyword %2)
                                  %2)]
    (into {} (map (fn [[k v]] [k (maybe-keywordize-value k v)]) m))))

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

(defn http-handler
  [content content-type dev?]
  (proxy [HttpHandler] []
    (handleHttpRequest [request response _]
      (when dev?
        (reload-all-ns))
      (doto response
        (.header "content-type" content-type)
        (.content (content request))
        (.end)))))

(defn session-data
  [req]
  (views/session (.queryParam req "filename")))

(defn run-webbit
  [s4-mon dev?]
  (doto (WebServers/createWebServer 3000)
    (.add "/ws"
          (proxy [WebSocketHandler] []
            (onOpen [_])
            (onClose [_])
            (onMessage [conn msg]
              (let [msg (json/decode msg true)]
                (handle conn msg s4-mon)))))
    (.add "/analysis"
          (http-handler views/analysis
                        "text/html"
                        dev?))
    (.add "/session"
          (http-handler session-data
                        "application/json"
                        dev?))
    (.add "/"
          (http-handler views/dashboard
                        "text/html"
                        dev?))
    (.add (StaticFileHandler. "./resources/public"))
    (.start)))