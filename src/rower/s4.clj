(ns rower.s4
  (:use serial-port)
  (:require [clojure.string :as string]))

(defonce config (atom nil))

(defn initialize!
  [new-config]
  (reset! config new-config))

(defn whitespace?
  [c]
  (contains? #{\return \newline} c))

(defn hex->decimal
  [cs]
  (Integer/valueOf (apply str cs) 16))

(defn event 
  [type & [value]]
  {:type type :value value})

(def ping         (partial event :ping))
(def pulse        (partial event :pulse))
(def stroke-start (partial event :stroke-start))
(def stroke-end   (partial event :stroke-end))
(def ok           (partial event :ok))
(def error        (partial event :error))
(def model        (partial event :model))

(defn event-for
  [cs]
  (let [cmd (apply str cs)]
    (cond
     (= "PING" cmd)          (ping)
     (= \P (first cmd))      (pulse (hex->decimal (subs cmd 1)))
     (= "SS"   cmd)          (stroke-start)
     (= "SE"   cmd)          (stroke-end)
     (= "OK"   cmd)          (ok)
     (= "ERROR" cmd)         (error)
     (= "IV" (subs cmd 0 2)) (model (subs cmd 2))
     :else                   (event :unknown cs))))

(defn bytes-seq
  [input]
  (lazy-seq
   (when-let [c (.read input)]
     (.print *err* c)
     (.flush *err*)
     (cons c (bytes-seq input)))))

(defn lazy-events
  [chars]
  (when (seq chars)
    (lazy-seq
     (let [[f r] (split-with (complement whitespace?) chars)
           event (event-for f)]
       (cons event (lazy-events (drop-while whitespace? r)))))))

(defn events-seq
  [input]
  (lazy-events (map char (bytes-seq input))))

(defn send-command
  [cmd port]
  (write port (.getBytes (str cmd "\r\n"))))

(def send-start             (partial send-command "USB"))
(def send-exit              (partial send-command "EXIT"))

(defn start
  [on-event]
  (let [{:keys [path baud]} @config]
    (let [port (open path baud)]
      (try
        (let [input (:in-stream port)]
          (send-start port)
          (doseq [event (events-seq input)]
            (on-event event)))
        (catch Exception e
          (send-exit port)
          (close port)
          (throw e))))))


;; (def send-reset             (partial send-command "RESET"))
;; (def send-info              (partial send-command "IV?"))
;; (def send-intensity-mps     (partial send-command "DIMS"))
;; (def send-intensity-mph     (partial send-command "DIMPH"))
;; (def send-intensity-500     (partial send-command "DI500"))
;; (def send-intensity-2km     (partial send-command "DI2KM"))
;; (def send-intensity-watts   (partial send-command "DIWA"))
;; (def send-intensity-cal-ph  (partial send-command "DICH"))
;; (def send-intensity-avg-mps (partial send-command "DAMS"))
;; (def send-intensity-avg-mph (partial send-command "DAMPH"))
;; (def send-intensity-avg-500 (partial send-command "DA500"))
;; (def send-intensity-avg-2km (partial send-command "DA2KM"))
;; (def send-distance-meters   (partial send-command "DDME"))
;; (def send-distance-miles    (partial send-command "DDMI"))
;; (def send-distance-km       (partial send-command "DDKM"))
;; (def send-distance-strokes  (partial send-command "DDST"))
