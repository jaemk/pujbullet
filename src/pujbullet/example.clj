(ns pujbullet.example
  (:require [clojure.repl :as repl]
            [manifold.stream :as stream]
            [pujbullet.core :as puj]))

(def apikey "yourkey!")

(def last-push (atom nil))

(defn sigint [_]
  (println "Stopping...")
  (System/exit 0))

(defn trigger [iden]
  (puj/dismiss {:type :note :iden iden})
  (puj/delete {:type :note :iden iden})
  (puj/push {:type :note :title "title" :body "got it!"}))

(defn dispatch [push]
  (let [msg (push "body")
        iden (push "iden")
        active (push "active")
        dismissed (push "dismissed")]
    (reset! last-push (push "modified"))
    (when (and active (not dismissed))
      (cond
        (= msg "say hello!") (trigger iden)
        :else (println msg)))))

(defn handle [info]
  (println info)
  (when (= (get info "subtype") "push")
    (if-let [push (as-> @last-push _
                    (puj/pull {:type :pushes :modified-after _})
                    (_ "pushes")
                    (first _))]
      (dispatch push))))

(defn run []
  (repl/set-break-handler! sigint) ; Add a KeyboardInterrupt handler
  (puj/set-key apikey)             ; Set your secret key
  (reset! last-push (puj/tnow))    ; Set the time (for fetching pushes modified-after a time)
  (puj/run-forever :handle handle :apikey apikey)  ; Run forever with a custom handle
  (println "started"))

(defn block []
  (Thread/sleep 1000)
  (recur))

(defn main []
  (.start (Thread. run)) ; Startup the listener
  (block))   ; Need to do something to keep listener alive

