(ns aggregate-this.core-test
  (:require [clojure.test :refer :all]
            [net.cgrand.enlive-html :as html]
            [aggregate-this.data :as test-data]
            [aggregate-this.core :as agg-core]))


(testing "get-post-upvoted"
  (testing "Return upvote"
    (is (= "357" (agg-core/get-post-upvoted (html/html-snippet test-data/upvoted-html-snippet))))))

;; TODO get-post-title
;; TODO cleanup-url
;; TODO scrape-user
;; TODO scrape-post
;; TODO extract-posts
;; TODO extract-comment
;; TODO scrape-comment
