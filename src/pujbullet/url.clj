(ns pujbullet.url)

(def urls
  {:base "https://api.pushbullet.com/v2/"
   :ws-base "wss://stream.pushbullet.com/websocket/"
   :contacts "contacts"
   :me "users/me"
   :pushes "pushes"
   :note "pushes"
   :devices "devices"
   :chats "chats"
   :modified-after "modified_after="})

(defn add-str [base-url qs]
  (cond-> base-url
    (< (.indexOf base-url "?") 0) (str "?")
    true (str qs)))

(defn encode [opts]
  (let [sub (urls (opts :type))
        iden (opts :iden)
        after (opts :modified-after)]
    (cond-> (urls :base)
      sub (str sub)
      iden (str "/" iden)
      after (add-str (str (:modified-after urls) after)))))

(defn ws [apikey]
  (str (urls :ws-base) apikey))

