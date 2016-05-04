(ns pujbullet.example
  (:require [clojure.repl :as repl]
            [manifold.stream :as stream]
            [pujbullet.core :as puj]))

(def last-push (atom nil))

(def apikey "yoursecretkey")

(defn sigint [_]
  (println "Stopping...")
  (System/exit 0))

(defn handle [info]
  (when (= (get info "subtype") "push")
    (let [push (as-> @last-push _
                 (puj/pull {:type :pushes :modified-after _})
                 (_ "pushes")
                 (first _))]
      (reset! last-push (push "modified"))
      (prn (push "body")))))

(defn run []
  (repl/set-break-handler! sigint)
  (puj/set-key apikey)
  (reset! last-push (puj/tnow))
  (puj/run-forever :handle handle :apikey apikey))

(defn block []
  (Thread/sleep 3000)
  (println "tick")
  (recur))

(defn main []
  (.start (Thread. run))
  (block))

