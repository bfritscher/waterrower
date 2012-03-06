(ns rower.stats
  (:import (java.util.concurrent Executors TimeUnit)
           (clojure.lang PersistentQueue)))

(def sample-queue-size 10)

(def sample-rate-ms 1000)

(def publish-rate-ms 4000)

(def start-time-ns (atom nil))

(def state (ref {:meters   0.0
                 :strokes  0.0}))

(def samples (ref PersistentQueue/EMPTY))

(def sample-state (ref {:meters  0.0
                        :strokes 0.0}))

(defn now-ns
  []
  (.. System nanoTime))

(defn ns->s
  [ns]
  (* ns 1e-9))

(defn bounded-conj
  [q v]
  (let [count (count q)]
    (if (> count sample-queue-size)
      (pop (conj q v))
      (conj q v))))

(defmulti handle :type)

(defmethod handle :distance
  [{value :value}]
  (dosync
   (alter sample-state update-in [:meters] + value)
   (alter state update-in [:meters] + value)))

(defmethod handle :stroke-start
  [_]
  (dosync
   (alter sample-state update-in [:strokes] + 0.5)
   (alter state update-in [:strokes] + 0.5)))

(defmethod handle :stroke-end
  [_]
  (dosync
   (alter sample-state update-in [:strokes] + 0.5)
   (alter state update-in [:strokes] + 0.5)))

(defmethod handle :default [_])

(defn sample
  []
  (dosync
   (alter samples bounded-conj @sample-state)
   (ref-set sample-state {:meters 0.0 :strokes 0.0})))

(defn publish
  [handler]
  (let [total-s (ns->s (- (now-ns) @start-time-ns))
        {meters :meters strokes :strokes} @state
        samples @samples
        total-mps         (/ meters total-s)
        total-spm         (* (/ strokes total-s) 60.0)
        total-500-split   (/ total-s (/ meters 500.0))
        sample-s          (* (* sample-rate-ms sample-queue-size) 1e-3)
        current-m         (reduce + (map :meters samples))
        current-strokes   (reduce + (map :strokes samples))
        current-mps       (/ current-m sample-s)
        current-spm       (* (/ current-strokes sample-s) 60.0)
        current-500-split (/ sample-s (/ current-m 500.0))]
    (handler {:total-mps         total-mps
              :total-spm         total-spm
              :total-meters      meters
              :total-strokes     strokes
              :total-s           total-s
              :total-500-split   total-500-split
              :total-2k-split    (* 4.0 total-500-split)
              :current-m         current-m
              :current-strokes   current-strokes
              :current-mps       current-mps
              :current-spm       current-spm
              :current-500-split current-500-split
              :current-2k-split  (* 4.0 current-500-split)})))

(defn start
  [handler]
  (let [exec (Executors/newScheduledThreadPool 2)]
    (reset! start-time-ns (now-ns))
    (.scheduleAtFixedRate exec
                          sample
                          sample-rate-ms
                          sample-rate-ms
                          TimeUnit/MILLISECONDS)
    (.scheduleAtFixedRate exec
                          (partial publish handler)
                          publish-rate-ms
                          publish-rate-ms
                          TimeUnit/MILLISECONDS)
    exec))

(defn stop
  [exec]
  (.shutdown exec))

