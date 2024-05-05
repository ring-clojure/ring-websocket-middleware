(defproject org.ring-clojure/ring-websocket-middleware "0.1.0"
  :description "Library with additional middleware for Ring websockets"
  :url "https://github.com/ring-clojure/ring-websocket-middleware"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "1.12.1"]]
  :plugins [[lein-codox "0.10.8"]]
  :codox {:output-path "codox"
          :source-uri "http://github.com/ring-clojure/ring-websocket-middleware/blob/{version}/{filepath}#L{line}"})
