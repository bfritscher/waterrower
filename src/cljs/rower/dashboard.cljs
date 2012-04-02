(ns rower.dashboard
  (:use [rower.dashboard :only [$ set-text]])
  (:require [goog.dom :as dom]
            [goog.dom.classes :as classes]
            [goog.events :as events]
            [clojure.browser.repl :as repl]))

(def socket (new js/ReconnectingWebSocket (str "ws://" (.-host (.-location js/document)) "/ws")))

(def distance (atom 0))

(defn pad
  [s n]
  (loop [s (str s)]
    (if (< (count s) n) (recur (str "0" s)) s)))

(defmulti command #(.-type %))

(defmethod command "display-hr"      [d] (set-text :hour (pad (.-value d) 2)))
(defmethod command "display-min"     [d] (set-text :min (pad (.-value d) 2)))
(defmethod command "display-sec"     [d] (set-text :sec (pad (.-value d) 2)))
(defmethod command "display-sec-dec" [d] (set-text :sec-dec (pad (.-value d) 2)))

(defmethod command "total-distance-m"
  [d]
  (let [v (.-value d)]
    (set-text :total-distance-m (pad v 5))
    (set-text :total-meters-remaining (pad (- @distance v) 5))))

(defmethod command "stroke-rate"   [d] (set-text :stroke-rate (.-value d)))
(defmethod command "total-strokes" [d] (set-text :total-strokes (.-value d)))

(defmethod command "avg-distance-cmps"
  [d]
  (let [v (.-value d)]
   (when (> v 0)
     (let [mps (.toFixed (/ v 100) 2)
           total-s (/ 500 mps)
           mins (.toFixed (.floor js/Math (/ total-s 60)) 0)
           secs (.floor js/Math (mod total-s 60))]
       (set-text :avg-mps mps)
       (set-text :avg-500m-min (pad mins 2))
       (set-text :avg-500m-sec (pad secs 2))))))

(defmethod command "total-kcal"
  [d]
  (let [v (.-value d)]
   (when (> v 0)
     (set-text :kcal (pad (.toFixed (/ v 1000) 2) 4)))))

(defmethod command :default
  [v]
  (.log js/console (str "no command handler for: " v)))

(defn set-status
  [msg & [class]]
  (let [e       ($ :#status)
        classes (classes/get e)
        class   (or class msg)]
    (doseq [c classes]
      (classes/remove e c))
    (set-text e msg)
    (classes/add e class)))

(defn on-message
  [msg]
  (let [data (.parse js/JSON (.-data msg))]
    (command data))) 

(defn start-workout
  [event]
  (let [workout-distance (.-value ($ :#distance))
        data (.-strobj {"type" "start-workout"
                        "data" (.-strobj {"type" "distance"
                                          "units" "meters"
                                          "value" workout-distance})})
        to-send (.stringify js/JSON data)]
    (.log js/console to-send)
    (reset! distance workout-distance)
    (set-text :total-meters-remaining workout-distance)
    (.send socket to-send)
    false))

(defn init
  []
  (set-status "connecting..." "connecting")
  (set! (.-onerror socket)   #(set-status "error"))  
  (set! (.-onopen socket)    #(set-status "connected"))
  (set! (.-onclose socket)   #(set-status "disconnected"))
  (set! (.-onmessage socket) on-message)
  (events/listen ($ :#start-workout) "click" start-workout))

