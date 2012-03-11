(ns rower.s4
  (:use serial-port)
  (:require [clojure.string :as string]))

(defonce config (atom nil))

(defn initialize!
  [new-config]
  (reset! config new-config))

(def commands
  {:start             "USB"
   :reset             "RESET"
   :exit              "EXIT"
   :info              "IV?"
   :intensity-mps     "DIMS"
   :intensity-mph     "DIMPH"
   :intensity-500     "DI500"
   :intensity-2km     "DI2KM"
   :intensity-watts   "DIWA"
   :intensity-cal-ph  "DICH"
   :intensity-avg-mps "DAMS"
   :intensity-avg-mph "DAMPH"
   :intensity-avg-500 "DA500"
   :intensity-avg-2km "DA2KM"
   :distance-meters   "DDME"
   :distance-miles    "DDMI"
   :distance-km       "DDKM"
   :distance-strokes  "DDST"})

(def memory-map ;; for some reason most dont work, but these do?
  {"054" {:type :total-distance-dec :size :single}
   "055" {:type :total-distance-m   :size :double}
   "140" {:type :total-strokes      :size :double}})

(defn send-command
  [cmd port]
  (let [cmd (if (keyword? cmd) (get commands cmd) cmd)]
    (write port (.getBytes (str cmd "\r\n")))))

(defn whitespace?
  [c]
  (contains? #{\return \newline} c))

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

(defn read-reply
  [s]
  (if-let [{type :type size :size} (get memory-map (subs s 3 6))]
    (event type (condp = size
                  :single (Integer/valueOf (subs s 6 8) 16)
                  :double (Integer/valueOf (subs s 6 10) 16)
                  :triple (Integer/valueOf (subs s 6 12) 24)))
    (.println *err* (str "cannot read reply from " s))))

(defn event-for
  [cs]
  (let [cmd (apply str cs)]
    (cond
     (= "PING"  cmd)            (ping)
     (= \P      (first cmd))    (pulse (Integer/valueOf (subs cmd 1) 16))
     (= "SS"    cmd)            (stroke-start)
     (= "SE"    cmd)            (stroke-end)
     (= "OK"    cmd)            (ok)
     (= "IV"    (subs cmd 0 2)) (model (subs cmd 2))
     (= "IDS"   (subs cmd 0 3)) (read-reply cmd)
     (= "IDD"   (subs cmd 0 3)) (read-reply cmd)
     (= "IDT"   (subs cmd 0 3)) (read-reply cmd)
     (= "ERROR" cmd)            (error)
     :else                      (event :unknown cmd))))

(defn request-data
  [port]
  (doseq [[addr {size :size}] memory-map]
    (let [cmd (condp = size
                :single "IRS"
                :double "IRD"
                :triple "IRT")]
      (command (str cmd addr) port))))

(defn pulse? [e] (= :pulse (:type e)))

(defn start-capturing
  [port on-event]
  (let [input (:in-stream port)]
    (loop [buffer []
           pulses 0] ;; request data every 10 pulses
      (let [c (char (.read input))]
        (if-not (whitespace? c)
          (recur (conj buffer c) pulses)
          (if (= \newline c)
            (let [event         (event-for buffer)
                  next-pulses   (if (pulse? event) (inc pulses) pulses)
                  send-request? (> next-pulses 10)]
              (on-event event)
              (when send-request?
                (request-data port))
              (recur [] (if send-request? 0 next-pulses)))
            (recur buffer pulses)))))))

(def workout-map
  {:distance "WSI"
   :duration "WSU"})

(def unit-map
  {:meters  1
   :miles   2
   :km      3
   :strokes 4})

(defn workout->command
  [{:keys [type units value]}]
  (str (get workout-map type) (get unit-map units) (Integer/toHexString value)))

(defn start
  [workout on-event]
  (let [{:keys [path baud]} @config]
    (let [port (open path baud)]
      (try
        (send-command :reset port)
        (send-command (workout->command workout) port)
        (send-command :start port)
        (start-capturing port on-event)
        port
        (catch Exception e
          (send-command :exit port)
          (close port)
          (throw e))))))

(defn stop
  [port]
  (send-command :exit port)
  (close port))

(comment
  ;; example workout
  (workout->command {:type  :distance
                     :units :meters
                     :value 8000})

  )

   ;;"08a" {:type :total-kcal         :size :triple}
   ;;"1A0" {:type :heart-rate         :size :single}
   ;;"14a" {:type :avg-distance-cmps  :size :double}
   ;;"1e0" {:type :display-sec-dec    :size :single}
   ;; "1e1" {:type :display-sec        :size :single}
   ;;"1e2" {:type :display-min        :size :single}
   ;; "1e3" {:type :display-hr         :size :single}
   ;;"1e8" {:type :total-workout-time :size :double}
   ;; "1ea" {:type :total-workout-mps  :size :double}
