(ns rower.utils
  (:require [goog.dom :as dom]))

(defn nodelist->coll [nl]
  (for [x (range 0 (.-length nl))]
    (aget nl x)))

(defn $
  [selector]
  (nodelist->coll (.querySelectorAll js/document (name selector))))

(defn set-text
  [el & contents]
  (let [el (if (keyword? el) (dom/getElement (name el)) el)]
    (dom/setTextContent el (apply str contents))))

(defn clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings.

   Borrowed and updated from mmcgrana."
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (.-strobj (reduce (fn [m [k v]]
               (assoc m (clj->js k) (clj->js v))) {} x))
    (coll? x) (apply array (map clj->js x))
    :else x))