(ns rower.analysis
  (:require [clojure.java.io :as io]
            [cheshire.core   :as json])
  (:import (java.io RandomAccessFile)))

(def ^:const wanted-events
  #{"total-distance-m"
    "total-distance-dec"
    "total-strokes"
    "avg-distance-cmps"
    "total-kcal"
    "stroke-rate"})

(def ^:const DATA-DIR "data")

(def filename-regexp #"(\d{4}-\d{2}-\d{2})_(\d+)_(\d+)_(\d+)_(.+)\.cap")

(defn within-sec?
  [t1 t2]
  (<= (- t2 t1) 1000))

(defn ->seconds
  ([events] (->seconds (first events) (rest events)))
  ([{at :at} events]
     (lazy-seq
      (let [next-sec (drop-while #(within-sec? at (:at %)) events)]
        (when (seq next-sec)
          (cons (first next-sec)
                (->seconds (rest next-sec))))))))

(defn events-from
  [filename max-sec]
  (->> (str "data/" filename)
       io/reader
       line-seq
       (map #(json/decode % true))
       (filter #(contains? wanted-events (:type %)))
       (drop-while #(or (not= "total-distance-m" (:type %))
                        (zero? (:value %))))
       (take-while #(or (not= "total-distance-m" (:type %))
                        (< (:value %) max-sec)))))

(defn session-filenames
  []
  (-> DATA-DIR java.io.File. .list vec))

(defn events-of
  [type coll]
  (->> coll
       (filter #(= type (:type %)))
       ->seconds
       (map (juxt :at :value))))

(defn filename->session
  [filename]
  (let [[_ date hour min distance units] (re-find filename-regexp filename)
        distance (Integer/parseInt distance)
        events (events-from filename distance)]
    {:date         date
     :time         (str hour ":" min)
     :distance     distance
     :units        units
     :stroke-rate  (events-of "stroke-rate" events)
     :avg-speed    (events-of "avg-distance-cmps" events)
     :filename     filename
     ;; :total-strokes (-> (get-events "total-strokes" events)
     ;;                    last
     ;;                    :value)
     }))

(defn sessions
  []
  (->> (session-filenames)
       (map filename->session)
       (sort-by :date)))

