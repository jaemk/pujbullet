(ns pujbullet.url)

(def urls
  {:base "https://api.pushbullet.com/v2/"
   :contacts "contacts"
   :me "users/me"
   :push "pushes"
   :devices "devices"
   :modified-after "modified_after="})

(defn add-str [base-url qs]
  (cond-> base-url
    (< (.indexOf base-url "?") 0) (str "?")
    true (str qs)))

(defn encode [opts]
  (let [sub ((:type opts) urls)
        after (:modified-after opts)]
    (cond-> (:base urls)
      sub (str sub)
      after (add-str (str (:modified-after urls) after)))))

