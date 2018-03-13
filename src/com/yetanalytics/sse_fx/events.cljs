(ns com.yetanalytics.sse-fx.events
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.sse-fx.event-source :as event-source]
            [com.yetanalytics.sse-fx.command :as command]))

(defn register-all!
  "Register all handlers for sse-fx.
   Could be called in a re-frame events ns."
  []
  (do
    (event-source/register!)
    (command/register!)))
