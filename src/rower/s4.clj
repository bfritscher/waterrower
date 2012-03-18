(ns rower.s4
  (:require [serial-port :as sp]
            [clojure.string :as string]))

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

(def memory-map
  {"054" {:type :total-distance-dec    :size :single :base 16}
   "055" {:type :total-distance-m      :size :double :base 16}
   "05A" {:type :clock-countdown-dec   :size :single :base 16}
   "05B" {:type :clock-countdown       :size :double :base 16}
   "140" {:type :total-strokes         :size :double :base 16}
   "1A9" {:type :stroke-rate           :size :single :base 16}
   "08A" {:type :total-kcal            :size :triple :base 16}
   "1A0" {:type :heart-rate            :size :single :base 16}
   "14A" {:type :avg-distance-cmps     :size :double :base 16}
   "1E0" {:type :display-sec-dec       :size :single :base 10}
   "1E1" {:type :display-sec           :size :single :base 10}
   "1E2" {:type :display-min           :size :single :base 10}
   "1E3" {:type :display-hr            :size :single :base 10}
   "1E8" {:type :total-workout-time    :size :double :base 16}
   "1EA" {:type :total-workout-mps     :size :double :base 16}
   "1EC" {:type :total-workout-strokes :size :double :base 16}})

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
  (let [hex (loop [h (Integer/toHexString value)]
              (if (< (count h) 4)
                (recur (str "0" h))
                h))]
    (str (get workout-map type) (get unit-map units) hex)))

(defn send-command
  [cmd port]
  (let [cmd (.toUpperCase (if (keyword? cmd) (get commands cmd) cmd))]
    (sp/write port (.getBytes (str cmd "\r\n")))))

(defn whitespace?
  [c]
  (contains? #{\return \newline} c))

(defn event 
  [type & [value]]
  {:type type :value value :at (System/currentTimeMillis)})

(def ping         (partial event :ping))
(def pulse        (partial event :pulse))
(def stroke-start (partial event :stroke-start))
(def stroke-end   (partial event :stroke-end))
(def ok           (partial event :ok))
(def error        (partial event :error))
(def model        (partial event :model))

(defn read-reply
  [s]
  (if-let [{:keys [type size base]} (get memory-map (subs s 3 6))]
    (event type (condp = size
                  :single (Integer/valueOf (subs s 6 8) base)
                  :double (Integer/valueOf (subs s 6 10) base)
                  :triple (Integer/valueOf (subs s 6 12) base)))
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

(defn start-requesting
  [port]
  (doseq [[addr {size :size}] memory-map]
    (let [cmd (condp = size
                :single "IRS"
                :double "IRD"
                :triple "IRT")]
      (send-command (str cmd addr) port))
    (Thread/sleep 10))
  (recur port))

(defn start-capturing
  [port handlers]
  (let [input (:in-stream port)]
    (loop [buffer []]
      (let [c (char (.read input))]
        (if-not (whitespace? c)
          (recur (conj buffer c))
          (if (= \newline c)
            (let [event (event-for buffer)]
              (doseq [h handlers]
                (h event))
              (recur []))
            (recur buffer)))))))

(defprotocol IWorkout
  (add-handler [this handler])
  (start [this])
  (close [this]))

(defn stop
  [port]
  (send-command :exit port)
  (sp/close port))

(defmacro spawn
  [& body]
  `(.start (Thread. (fn [] ~@body))))

(defn new-workout
  [config workout]
  (let [{:keys [path baud]} config
        port (sp/open path baud)
        handlers (atom [])]
    (reify IWorkout
      (add-handler [_ handler]
        (swap! handlers conj handler))
      (start [this]
        (try
          (send-command :reset port)
          (send-command (workout->command workout) port)
          (send-command :start port)
          (spawn (start-capturing port @handlers))
          (spawn (start-requesting port))
          port
          (catch Exception e
            (stop port)
            (throw e))))
      (close [_] (stop port)))))



