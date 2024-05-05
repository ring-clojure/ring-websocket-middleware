(ns ring.middleware.websocket-keepalive-test
  (:require [clojure.test :refer [deftest is]]
            [ring.middleware.websocket-keepalive :as keepalive]))

(deftest a-test
  (is (= 0 1)))
