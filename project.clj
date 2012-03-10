(defproject rower "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure       "1.3.0"]
                 [org.clojure/tools.cli     "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [serial-port               "1.1.2"]
                 [org.webbitserver/webbit   "0.4.6"]
                 [cheshire                  "2.2.1"]
                 [log4j                     "1.2.16"]]
  :main rower.main)