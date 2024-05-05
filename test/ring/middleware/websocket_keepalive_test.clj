(ns ring.middleware.websocket-keepalive-test
  (:require [clojure.test :refer [deftest is]]
            [ring.websocket :as ws]
            [ring.websocket.protocols :as wsp]
            [ring.middleware.websocket-keepalive :as ka]))

(deftest test-websocket-keepalive-request
  (let [ping-count (atom 0)
        socket     (reify wsp/Socket
                     (-open? [_] true)
                     (-send [_ _])
                     (-ping [_ _] (swap! ping-count inc))
                     (-pong [_ _])
                     (-close [_ _ _]))
        response   {::ws/listener
                    (reify wsp/Listener
                      (on-open [_ _])
                      (on-message [_ _ _])
                      (on-pong [_ _ _])
                      (on-error [_ _ _])
                      (on-close [_ _ _ _]))}
        response'  (ka/websocket-keepalive-response response {:period 10})
        listener   (::ws/listener response')]
    (wsp/on-open listener socket)
    (Thread/sleep 51)
    (wsp/on-close listener socket 1000 "")
    (Thread/sleep 30)
    (is (= @ping-count 5))))
