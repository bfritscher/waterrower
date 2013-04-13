(ns rower.views
  (:require [rower [analysis :as anal]]
            [cheshire.core :as json]
            [hiccup
             [core :as hic]
             [page :as hicp]]))

(defn dashboard
  []
  (hic/html
   [:html
    [:head
     (hicp/include-css "css/reset.css"
                       "css/main.css"
                       "css/dashboard.css")]
    [:body#dashboard-page
     [:div#bar.clearfix
      [:div.pad
       [:div#status "Idle"]
       [:form#workout-form
        [:dl
         [:dt "Distance (m):"]
         [:dd
          [:input#distance {:type "text" :value "8000"}]]]
        [:ul
         [:li [:a#start-workout {:href "#"} "Start Workout"]]]]]]
     [:div#content
      [:div.pad
       [:div#sub
        [:dl
         [:dt "total strokes"]
         [:dd [:span#total-strokes "0"]]]
        [:dl#meters-sec
         [:dt "meters/sec"]
         [:dd [:span#avg-mps "0.00"]]]
        [:dl#total-distance
         [:dt "meters"]
         [:dd [:span#total-distance-m "00000"]]]
        [:dl#duration
         [:dt "total time"]
         [:dd
          [:span#hour "00"] ":"
          [:span#min "00"] ":"
          [:span#sec "00"] ":"
          [:span#sec-dec "00"]]]
        [:dl
         [:dt "cal"]
         [:dd#kcal "0000"]]]
       [:div#main
        [:dl
         [:dt "strok rate/min"]
         [:dd [:span#stroke-rate "00"]]]
        [:dl#five-hundred
         [:dt "/500m"]
         [:dd
          [:span#avg-500m-min "00"] ":"
          [:span#avg-500m-sec "00"]]]
        [:dl#total-distance-remaining
         [:dt "meters remaining"]
         [:dd [:span#total-meters-remaining "00000"]]]]]]
     (hicp/include-js ;; "js/jquery-1.7.1.min.js"
                      "js/reconnecting-websocket.min.js"
                      "js/app.js")
     [:script "rower.dashboard.init();"]]]))

