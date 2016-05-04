(ns pujbullet.core
  (:require byte-streams
            [aleph.http :as http]
            [manifold.stream :as stream]
            [cheshire.core :refer [parse-string generate-string]]
            [pujbullet.url :as url]))

(defn tnow []
  (quot (System/currentTimeMillis) 1000))

;;-- Config setup
(def config (atom {:key "setkey"}))

(defn set-key [k]
  (swap! config assoc :key k))

(defn apikey?
  "Prefer a supplied :key over the default config"
  [opts]
  (get opts :key (:key @config)))

;;-- Decode byte-stream response
(defn decode-resp [resp]
  (-> resp
    (:body)
    (byte-streams/convert String)
    (parse-string)))

;;-- Pull specified :type key
(defn pull [opts]
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)]
    (as-> endpoint _
      @(http/get _ {:headers {"Access-Token" apikey}})
      (decode-resp _))))

;;-- Dismiss
(defn dismiss [opts]
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)]
    (as-> endpoint _
    @(http/post _
                {:headers {"Access-Token" apikey
                           "Content-Type" "application/json"}
                 :body (generate-string {"dismissed" true})})
    (decode-resp _)
    (prn _))))

;;-- Delete
(defn delete [opts]
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)]
    (as-> endpoint _
      @(http/delete _ {:headers {"Access-Token" apikey}})
      (decode-resp _)
      (prn _))))

;;-- Pushes, setups required
(defmulti push
  (fn [opts]
    (:type opts)))

(defmethod push :note [opts]
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)
        title (opts :title)
        body (opts :body)]
    (if (nil? body)
      (throw (Throwable. "message body required"))
      (as-> endpoint _
        @(http/post _ {:headers {"Access-Token" apikey
                                 "Content-Type" "application/json"}
                       :body (generate-string {"body" body
                                               "title" title
                                               "type" "note"})})
        (decode-resp _)
        (prn _)))))

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

(defn run-forever
  "Attaches a custom handle to Pushbullet event stream.
   Supplied handle should accept a map (status pings)"
  [& {:keys [apikey handle]
      :or {apikey (apikey? {})
           handle default-handle}}]
  (reset! socket-handle handle)
  (let [conn @(http/websocket-client (url/ws apikey))]
    (stream/consume handle-wrap conn)))


