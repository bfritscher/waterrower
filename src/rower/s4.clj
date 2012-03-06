(ns rower.s4
  (:use serial-port))

(def config {:port-name "/dev/tty.usbmodemfd121"
             :baud-rate 1200})

(defn whitespace?
  [c]
  (contains? #{\return \newline} c))

(defn hex-chars->decimal
  [cs]
  (* 0.01 (read-string (apply str "0x" cs))))

(defn bytes-seq
  [input]
  (lazy-seq
   (when-let [c (.read input)]
     (cons c (bytes-seq input)))))

(defrecord Event [type at value])

(defn event
  [type & [value]]
  (Event. type (.. System nanoTime) value))

(def ping (partial event :ping))
(def distance (partial event :distance))
(def stroke-start (partial event :stroke-start))
(def stroke-end (partial event :stroke-end))

(defn event-for
  [cs]
  (cond
   (= "PING" (apply str cs)) (ping)
   (= \P (first cs))         (distance (hex-chars->decimal (rest cs)))
   (= "SS" (apply str cs))   (stroke-start)
   (= "SE" (apply str cs))   (stroke-end)
   :else                     (event :unknown cs)))

(defn lazy-events
  [chars]
  (when (seq chars)
    (lazy-seq
     (let [[f r] (split-with (complement whitespace?) chars)]
       (cons (event-for f) (lazy-events (drop-while whitespace? r)))))))

(defn events-seq
  [input]
  (lazy-events (map char (bytes-seq input))))

(defn with-events
  [body-fn]
  (with-open [port (open (:port-name config)
                         (:baud-rate config))]
    (try
      (let [input (:in-stream port)]
        (write port (.getBytes "USB\r\n"))
        (body-fn (events-seq input)))
      (catch Exception e
        (write port (.getBytes "EXIT\r\n"))
        (throw e)))))