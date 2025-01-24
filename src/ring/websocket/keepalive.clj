(ns ring.websocket.keepalive
  (:require [ring.websocket :as ws]
            [ring.websocket.protocols :as wsp])
  (:import [java.util.concurrent
            Executors Future ScheduledExecutorService TimeUnit]))

(def ^:private default-schedule-executor
  (delay (Executors/newSingleThreadScheduledExecutor)))

(defn websocket-keepalive-response
  "Adds a periodic ping to a WebSocket response; returns the response unaltered
  otherwise. See wrap-websocket-keepalive for the available options."
  ([response]
   (websocket-keepalive-response response {}))
  ([response options]
   (if (ws/websocket-response? response)
     (let [listener (::ws/listener response)
           executor (:executor options @default-schedule-executor)
           period   (:period options 20000)
           task     (promise)]
       (assoc response ::ws/listener
              (reify wsp/Listener
                (on-open [_ socket]
                  (deliver task (.scheduleAtFixedRate
                                 ^ScheduledExecutorService executor
                                 #(ws/ping socket)
                                 period period TimeUnit/MILLISECONDS))
                  (wsp/on-open listener socket))
                (on-message [_ socket message]
                  (wsp/on-message listener socket message))
                (on-pong [_ socket data]
                  (wsp/on-pong listener socket data))
                (on-error [_ socket throwable]
                  (wsp/on-error listener socket throwable))
                (on-close [_ socket code reason]
                  (.cancel ^Future @task false)
                  (wsp/on-close listener socket code reason))
                wsp/PingListener
                (on-ping [_ socket data]
                  (if (satisfies? wsp/PingListener listener)
                    (wsp/on-ping listener socket data)
                    (wsp/-ping socket data))))))
     response)))

(defn wrap-websocket-keepalive
  "Middleware that adds a periodic ping to WebSocket responses, in order to
  stop them from timing out. Takes the following options:

    :period   - the time in milliseconds between pings (default 20000)
    :executor - a custom ScheduledExecutorService to handle scheduling"
  ([handler]
   (wrap-websocket-keepalive handler {}))
  ([handler options]
   (fn
     ([request]
      (websocket-keepalive-response (handler request) options))
     ([request respond raise]
      (handler request
               #(respond (websocket-keepalive-response % options))
               raise)))))
