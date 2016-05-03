(ns pujbullet.core
  (:require byte-streams
            [aleph.http :as http]
            [manifold.stream :as stream]
            [cheshire.core :refer [parse-string]]
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
    (as-> endpoint _
      @(http/get _ {:headers {"Access-Token" apikey}})
      (:body _)
      (byte-streams/convert _ String)
      (parse-string _)
      (get _ "pushes"))))

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
(defn default-handle [msg]
  (prn 'tickle! msg))

(def socket-handle
  (atom nil))

(defn handle-wrap [msg]
  (-> msg
    (parse-string)
    (@socket-handle)))

(defn run-forever [& {:keys [apikey handle]
                      :or {apikey (apikey? {})
                           handle default-handle}}]
  (reset! socket-handle handle)
  (let [conn @(http/websocket-client (str "wss://stream.pushbullet.com/websocket/" apikey))]
    (stream/consume handle-wrap conn)))

