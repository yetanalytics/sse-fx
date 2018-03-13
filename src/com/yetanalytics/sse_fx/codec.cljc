(ns com.yetanalytics.sse-fx.codec
  (:require [cognitect.transit :as transit]
            #?(:clj [clojure.java.io :as io]))
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream])))

(def write-handlers
  #?(:clj {}
     :cljs {}))

(def read-handlers
  #?(:clj {}
     :cljs {}))

(defn encode-string [data]
  #?(:clj (let [o (ByteArrayOutputStream. 4096)
                w (transit/writer o :json {:handlers write-handlers})]
            (transit/write w data)
            (.toString o))
     :cljs (let [w (transit/writer :json {:handlers write-handlers})]
             (transit/write w data))))

(defn decode-string [s]
  #?(:clj (with-open [i (io/input-stream (.getBytes s))]
            (let [r (transit/reader i :json {:handlers read-handlers})]
              (transit/read r)))
     :cljs (let [r (transit/reader :json {:handlers read-handlers})]
             (transit/read r s))))
