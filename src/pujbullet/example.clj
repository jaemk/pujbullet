(ns pujbullet.example
  (:require [clojure.repl :as repl]
            [manifold.stream :as stream]
            [pujbullet.core :as puj]))

(def last-push (atom nil))

(def apikey "yourkeyhere")

(defn sigint [_]
  (println "Stopping...")
  (System/exit 0))

(defn handle [info]
  (when (= (get info "subtype") "push")
    (let [push (as-> @last-push _
                 (puj/pull {:type :push :modified-after _})
                 (first _))]
      (reset! last-push (get push "modified"))
      (prn (get push "body"))))
    (prn @last-push))

(defn run []
  ;(repl/set-break-handler! sigint)
  (puj/set-key apikey)
  (reset! last-push (puj/tnow))
  (puj/run-forever :handle handle :apikey apikey))

(defn main [] (run))
