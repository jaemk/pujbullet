(ns pujbullet.core
  (:require [aleph.http :as http]
            [clj-time.core :as cljt]
            [pujbullet.url :as url]))

(defn tnow []
  (quot (System/currentTimeMillis) 1000))

;;-- Optionally setup config once
(def config (atom {:key "setkey"}))

(defn set-key [k]
  (swap! config assoc :key k))

(defn apikey?
  "Prefer a supplied :key over the default config"
  [opts]
  (get opts :key (:key @config)))

;;-- Pull specified :type key
(defn pull [opts]
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)]
    @(http/get endpoint
                {:headers
                  {"Access-Token" apikey}})))

;;-- Different setups required
(defmulti push
  (fn [opts]
    (:type opts)))

(defmethod push :note [opts]
  (println opts))

(defmethod push :link [opts]
  (println opts))

(defmethod push :file [opts]
  (println opts))

;;--- Socket Subscription
(def sock-conn (atom {}))

(defn connect-socket [& {:keys [apikey] :or {apikey (apikey? {})}}]
  (swap! sock-conn assoc :conn
                         @(http/websocket-client
                            (str "wss://stream.pushbullet.com/websocket/" apikey))))

;in a run forever to get nops and tickles
;@(s/take! conn)

