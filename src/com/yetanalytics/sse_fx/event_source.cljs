(ns com.yetanalytics.sse-fx.event-source
  "Fx/CoFX for EventSource objects"
  (:require [com.yetanalytics.sse-fx.codec :as codec]
            [re-frame.core :as re-frame]))

(defn event-source-error
  "Wrap event source errors."
  [error-map & [?cause]]
  (if ?cause
    (ex-info "EventSource Error"
             (merge error-map
                    {:type ::event-source-error})
             ?cause)
    (ex-info "EventSource Error"
             (merge error-map
                    {:type ::event-source-error}))))

(defn new-event-source
  "Create a new event source, with optional handlers for open, message, error.
  By default, messages are transit decoded, but you can change this by setting
  decode-msg-fn to false or replacing it with something."
  [{:keys [uri
           on-open
           on-message
           on-error
           config
           decode-msg-fn]
    :or {config {}
         decode-msg-fn (fn [e] (some-> e .-data not-empty codec/decode-string))
         on-open (fn [e]
                   (.debug js/console "Unhandled Event Source Open Event"
                           e))
         on-message (fn [e]
                      (.debug js/console "Unhandled EventSource Message Event"
                              e))
         on-error (fn [e]
                    (.error js/console "Unhandled EventSource Error" e))}}]
  (let [es (js/EventSource. uri (clj->js config))]
    (set! (.-onopen es)
          (fn [e]
            (try (on-open e)
                 (catch js/Error err
                   (on-error (event-source-error
                              {:event-source es
                               :event e
                               :context ::open
                               :handler-fn on-open}
                              err))))))
    (set! (.-onmessage es)
          (fn [e]
            (try (on-message (cond-> e
                               decode-msg-fn
                               decode-msg-fn))
                 (catch js/Error err
                   (on-error (event-source-error
                              {:event-source es
                               :event e
                               :context ::message
                               :handler-fn on-message}
                              err))))))
    (set! (.-onerror es)
          (fn [e]
            (on-error
             (event-source-error
              {:event-source es
               :event e
               :context ::error}))))
    es))

(defonce event-sources
  ;; map of uri (or another key) to map wrapping event source object
  (atom {}))

(defn close!
  "Given an event source wrapper map, close it and run a callback if present"
  [{es :event-source
    ?on-close :on-close
    :as es-map}]
  (do
    (.close es)
    (when ?on-close
      (?on-close))))

;; Handler fns

(declare close-fx)

(defn init-fx
  "Fx handler to initialize an event source that talks back to re-frame.
   EventSources are stored in a map by their key, which can be a URI or explicit
   value.
   handle-* args are partial event dispatch vectors that will be called with the
   key/uri and the event source itself (or message)."
  [{:keys [uri
           key ;; if present, used instead of the uri
           ;; handler vectors
           handle-open
           handle-message
           handle-error
           ;; A special handler that will be stored and called
           ;; if close fx is fired
           handle-close
           ;; any override args for event source
           event-source-args]
    :as arg-map}]
  (let [key (or key uri)]
    ;; Close any open connections with this key
    (close-fx {:keys [key]})
    (let [es (new-event-source
              (merge
               (cond-> {:uri uri}
                 handle-open
                 (assoc
                  :on-open
                  #(re-frame/dispatch
                    (conj handle-open
                          key %)))
                 handle-message
                 (assoc
                  :on-message
                  #(re-frame/dispatch
                    (conj handle-message
                          key %)))
                 handle-error
                 (assoc
                  :on-error
                  #(re-frame/dispatch
                    (conj handle-error
                          key %))))
               event-source-args))]
      (swap! event-sources
             assoc
             key
             (cond-> {:event-source es}
               handle-close
               (assoc :on-close #(re-frame/dispatch
                                  (conj handle-close
                                        key es))))))))

(defn get-cofx
  "A cofx handler that gets all or a particular event source"
  [cofx ?key]
  (cond-> @event-sources
    ?key (get ?key)))

(defn close-fx
  "An fx handler that shuts down one or all event sources.
  handle-done is a partial event vector that will be dispatched with the state
  of the event-sources map before and after."
  [{:keys [keys handle-done]}]
  (let [sources @event-sources]
    (if (= keys :all)
      (do (reset! event-sources {})
          (doseq [es-map (vals sources)]
            (close! es-map)))
      (do (swap! event-sources dissoc keys)
          (doseq [es-map (vals
                          (select-keys sources keys))]
            (close! es-map))))
    (when handle-done
      (let [sources-after @event-sources]
        (re-frame/dispatch (conj handle-done
                                 sources
                                 sources-after))))))

(defn register!
  "Register fx/cofx for event sources"
  []
  (do
    ;; register init
    (re-frame/reg-fx
     ::init
     init-fx)
    ;; register get
    (re-frame/reg-cofx
     ::get
     get-cofx)
    ;; register close
    (re-frame/reg-fx
     ::close
     close-fx)))
