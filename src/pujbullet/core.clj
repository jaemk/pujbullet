(ns pujbullet.core
  (:require byte-streams
            [clojure.string :as string]
            [aleph.http :as http]
            [manifold.stream :as stream]
            [ring.util.mime-type :as mime]
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
  (println "dismissing")
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)]
    (as-> endpoint _
    @(http/post _
                {:headers {"Access-Token" apikey
                           "Content-Type" "application/json"}
                 :body (generate-string {"dismissed" true})})
    (decode-resp _)))
  (println "dismissed!"))

;;-- Delete
(defn delete [opts]
  (println "deleting")
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)]
    (as-> endpoint _
      @(http/delete _ {:headers {"Access-Token" apikey}})
      (decode-resp _)))
  (println "deleted!"))

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
  (let [apikey (apikey? opts)
        endpoint (url/encode opts)
        title (opts :title)
        body (opts :body)
        link (opts :url)]
    (if (nil? link)
      (throw (Throwable. "link url required"))
      (as-> endpoint _
        @(http/post _ {:headers {"Access-Token" apikey
                                 "Content-Type" "application/json"}
                       :body (generate-string {"body" body
                                               "title" title
                                               "url" link
                                               "type" "link"})})
        (decode-resp _)
        (prn _)))))

; still working on multipart file post
(defmethod push :file [opts]
  (let [apikey (apikey? opts)
        upload-point (url/encode (assoc opts :iden :upload))
        endpoint (url/encode opts)
        title (opts :title)
        body (opts :body)
        file (opts :file)
        filename (-> file (string/split #"/") (last))]
    (if (nil? file)
      (throw (Throwable. "file path required"))
      (as-> upload-point _
        @(http/post _ {:headers {"Access-Token" apikey
                                 "Content-Type" "application/json"}
                       :body (generate-string {"file_name" filename
                                               "file_type" (mime/ext-mime-type filename)})})
        (decode-resp _)
        @(http/post (_ "upload_url") {:headers {"Access-Token" apikey
                                                "Content-Type" "multipart/form-data"}
                                      :multipart [:form
                                                    {:action "/file" :method "post" :enctype "multipart/form-data"}
                                                    [:input {:name file :type "file"}]]})))))


;;--- Socket Subscription
(defn default-handle [msg]
  (prn 'tickle! msg))

(def socket-handle
  (atom nil))

(defn handle-wrap [msg]
  (-> msg
    ;(#( (prn %1) (parse-string %1)))
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


