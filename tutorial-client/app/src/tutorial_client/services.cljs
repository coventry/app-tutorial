(ns tutorial-client.services
  (:require [io.pedestal.app.protocols :as p]
            [cljs.reader :as reader]))

;; Put any messages received over the SSE connection on the local
;; message input queue.
(defn receive-ss-event [app e]
  (let [message (reader/read-string (.-data e))]
    (.log js/console (format "sse message: %s" message))
    (p/put-message (:input app) message)))

;; Interface to the javascript which actually starts the SSE
;; connection.
(defrecord Services [app]
  p/Activity
  (start [this]
    (let [source (js/EventSource. "/msgs")]
      (.addEventListener source
                         "msg"
                         (fn [e] (receive-ss-event app e))
                         false)))
  ;; That this does nothing...  It is required as part of the Activity
  ;; protocol
  (stop [this]))

;; This is called by the loop in app.clj/consume-effects each time a
;; message is received on the *output* queue.  These are things which
;; get published by the :effect entry in behavior/example-app (the
;; dataflow.)  So this is where the counter increments are being
;; published.
(defn services-fn [message input-queue]
  (let [body (pr-str message)]
    (let [http (js/XMLHttpRequest.)]
      (.open http "POST" "/msgs" true)
      (.setRequestHeader http "Content-type" "application/edn")
      (.send http body))))
