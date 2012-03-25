(ns rower.s4-stub
  (:use [rower.s4 :only [S4Monitor]]))

(defn new-stub-s4
  [& args]
  (reify S4Monitor
    (clear-handlers [_])
    (add-handler    [_ handler])
    (start-workout  [_ workout])
    (send-command   [_ command])
    (close          [_])))