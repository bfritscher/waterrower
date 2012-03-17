(ns rower.www
  (:require [cheshire.core :as json]
            [rower.s4 :as s4])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler]
           [org.webbitserver.handler StaticFileHandler]))

(defn send-event
  [con event]
  (.send con (json/encode event)))

(defn run-webbit
  [workout]
  (doto (WebServers/createWebServer 3000)
    (.add "/rower"
          (proxy [WebSocketHandler] []
            (onOpen [c]
              (s4/add-handler workout (partial send-event c))
              (s4/start workout))
            (onClose [c] )
            (onMessage [c data] )))
    (.add (StaticFileHandler. "./resources/public"))
    (.start)))