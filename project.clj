(defproject com.yetanalytics/sse-fx "0.1.1-SNAPSHOT"
  :description "re-frame FX and CoFX for working with EventSource connections in the browser"
  :url "https://github.com/yetanalytics/sse-fx"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.126" :scope "provided"]
                 [re-frame "0.10.5" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.243"]])
