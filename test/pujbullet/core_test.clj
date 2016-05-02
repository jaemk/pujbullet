(ns pujbullet.core-test
  (:require [clojure.test :refer :all]
            [pujbullet.core :refer :all]
            [pujbullet.url :refer :all]))

(deftest push-url
  (testing "push url"
    (is (re-find #"/pushes$" (encode {:type :push})))))
