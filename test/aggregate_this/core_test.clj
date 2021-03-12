(ns aggregate-this.core-test
  (:require [clojure.test :refer :all]
            [net.cgrand.enlive-html :as html]
            [aggregate-this.db :as db]
            [aggregate-this.data :as test-data]
            [aggregate-this.core :as agg-core]))


(testing "get-post-upvoted"
  (testing "Return upvote when it is there"
    (is (= "357" (agg-core/get-post-upvoted (html/html-snippet test-data/upvoted-html-snippet)))))
  (testing "Return nil when it is not there"
    (is (nil? (agg-core/get-post-upvoted (html/html-snippet "<div>Nothing</div>"))))))

(testing "get-post-title"
  (testing "Return title when it is there"
    (is (= "I am a title" (agg-core/get-post-title (html/html-snippet test-data/title-html-snippet)))))
  (testing "Return nil when it is not there"
    (is (nil? (agg-core/get-post-title (html/html-snippet "<div>Nothing</div>"))))))

(testing "cleanup-url"
  (testing "Dirty->clean"
    (is (= "https://old.reddit.com/r/wallstreetbets/comments/lv5exe/gme_to_the_moon_boiz_lets_get_it" (agg-core/cleanup-url "https://old.reddit.com/r/wallstreetbets/comments/lv5exe/gme_to_the_moon_boiz_lets_get_it/gpbwn9x/")))))

(testing "scrape-user"
  (with-redefs [agg-core/http-get (fn [_] (hash-map :body test-data/user-html-snippet))]
    (= ["http://old.reddit.com/r/my-sub/comment1" "http://old.reddit.com/r/my-sub/comment2" "http://old.reddit.com/r/my-sub/link1" "http://old.reddit.com/r/my-sub/link2"] (agg-core/scrape-user "http://old.reddit.com"))))

(testing "scrape-post"
  (with-redefs [agg-core/http-get (fn [_] (hash-map :body test-data/post-html-snippet))]
    (= (db/map->RPost
        {:link "http://old.reddit.com/r/my-sub/link1"
         :title "I am a title"
         :upvote "357"})
       (agg-core/scrape-post "http://old.reddit.com/r/my-sub/link1"))))

;; TODO extract-posts
;; TODO extract-comment
;; TODO scrape-comment
