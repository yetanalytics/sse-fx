(ns com.yetanalytics.sse-fx.command
  "Easily send command events to a server. Posts, uses transit, and expects a 202"
  (:require [goog.net.XhrIo :as xhrio]
            [com.yetanalytics.sse-fx.codec :as codec]
            [re-frame.core :as re-frame]))

(defn send-command! [{:keys [uri data
                             encode-msg-fn
                             handle-accepted
                             handle-error
                             headers]
                      :or {encode-msg-fn
                           codec/encode-string}
                      :as arg-map}]
  (xhrio/send uri
              (fn [e]
                (let [xhr (.-target e)
                      status (.getStatus xhr)]
                  (if (= 202 status)
                    (when handle-accepted
                      (handle-accepted xhr
                                       arg-map))
                    (when handle-error
                      (handle-error xhr
                                    arg-map)))))
              "POST"
              (cond-> data
                encode-msg-fn
                encode-msg-fn)
              (clj->js (merge {"Content-Type" "application/transit+json"}
                              headers))))

(defn send-fx
  [{:keys [handle-accepted
           handle-error]
    :as arg-map}]
  (send-command!
   (cond-> arg-map
     handle-accepted
     (assoc :handle-accepted
            (fn [xhr argm]
              (re-frame/dispatch
               (conj handle-accepted
                     xhr argm))))
     handle-error
     (assoc :handle-error
            (fn [xhr argm]
              (re-frame/dispatch
               (conj handle-error
                     xhr argm)))))))

(defn register!
  "Register fx for commands"
  []
  (do
    (re-frame/reg-fx
     ::send
     send-fx)))
