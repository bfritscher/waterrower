(ns rower.www
  (:require [cheshire.core :as json])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler]
           [org.webbitserver.handler StaticFileHandler]))

(defn on-message
  [con data]
  (let [data (-> data (json/decode true))]
    (.send con (json/encode {:received data}))))

(defn run-webbit
  []
  (doto (WebServers/createWebServer 3000)
    (.add "/"
          (proxy [WebSocketHandler] []
            (onOpen [c] (println "open"))
            (onClose [c] (println "close"))
            (onMessage [c data] (on-message c data))))
    (.add (StaticFileHandler. "."))
    (.start)))