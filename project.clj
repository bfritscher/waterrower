(defproject rower "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure       "1.3.0"]
                 [serial-port               "1.1.2"]
                 [aleph                     "0.2.1-alpha2-SNAPSHOT"]
                 [cheshire                  "2.2.1"]
                 [hiccup                    "1.0.0-beta1"]
                 [compojure                 "1.0.1"]
                 [org.clojure/clojurescript "0.0-1006"]]
  :plugins [[lein-cljsbuild "0.1.5"]]
  :source-path "src/clj"
  :extra-classpath-dirs ["src/cljs"]
  :cljsbuild {:builds
              [{:source-path "src/cljs"
                :compiler {:output-to "resources/public/js/app.js"
                           :libs ["reconnecting-websocket.min.js"]
                           ;; :optimizations :advanced
                           }}]}
  :main rower.main)