(ns ring.websocket.keepalive-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.websocket :as ws]
            [ring.websocket.protocols :as wsp]
            [ring.websocket.keepalive :as ka]))

(deftest test-websocket-keepalive-request
  (testing "websocket pings"
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
      (Thread/sleep 20)
      (is (= 5 @ping-count))))
  (testing "response passthrough"
    (let [response {:status 200
                    :headers {"Content-Type" "text/plain"}
                    :body "Non-websocket response"}]
      (is (= response (ka/websocket-keepalive-response response))))))

(deftest test-wrap-websocket-keepalive
  (testing "websocket pings"
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
          handler    (fn
                       ([_] response)
                       ([_ respond _] (respond response)))
          handler'   (ka/wrap-websocket-keepalive handler {:period 10})]
      (testing "synchronous handlers"
        (let [listener (::ws/listener (handler' {}))]
          (wsp/on-open listener socket)
          (Thread/sleep 41)
          (wsp/on-close listener socket 1000 "")
          (Thread/sleep 20)
          (is (= 4 @ping-count))))
      (testing "asynchronous handlers"
        (let [respond (promise)]
          (reset! ping-count 0)
          (handler' {} respond (fn [_]))
          (let [listener (::ws/listener @respond)]
            (wsp/on-open listener socket)
            (Thread/sleep 51)
            (wsp/on-close listener socket 1000 "")
            (Thread/sleep 20)
            (is (= 5 @ping-count)))))))
  (testing "response passthrough"
    (testing "response passthrough"
      (let [response {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "Non-websocket response"}
            handler  (fn
                       ([_] response)
                       ([_ respond _] (respond response)))
            handler' (ka/wrap-websocket-keepalive handler)]
        (testing "synchronous handlers"
          (is (= response (handler' {}))))
        (testing "asynchronous handlers"
          (let [respond (promise)]
            (handler' {} respond (fn [_]))
            (is (= response @respond))))))))
