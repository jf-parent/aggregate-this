(ns aggregate-this.core
  (:require [net.cgrand.enlive-html :as html]
            [aggregate-this.db :as db]
            [clj-http.client :as client]))

(defn http-get [url]
  (let [h {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/85.0"}]
    (client/get url {:headers h})))

(defn search-reddit
  ([query] (search-reddit "" query))
  ([subreddit query]
   (let [subreddit (str subreddit "/")
         url (format "https://old.reddit.com/%ssearch?&restrict_sr=on&q=%s&include_over_18=on&sort=relevance&t=day" subreddit query)
         page (http-get url)
         search-result (html/select (html/html-snippet (:body page)) [:header.search-result-header])]
     (println url)
     search-result)))

(defn get-post-upvoted [html-snippet]
  (->> html-snippet
       (#(html/select % [:.score.unvoted]))
       ((comp first :content first))))

(defn get-post-title [html-snippet]
  (->> html-snippet
       (#(html/select % [:head :title]))
       ((comp first :content first))))

(defn scrape-post [url]
  (let [body (:body (http-get url))
        html-snippet (html/html-snippet body)
        upvote (get-post-upvoted html-snippet)
        title (get-post-title html-snippet)
        comments []]
    {:link url
     :title title
     :upvote upvote}))

(defn extract-posts [dom]
  (->> dom
       (map (comp :href :attrs first :content))
       (map scrape-post)))

;; REPL
;; (let [search-result (search-subreddit "r/wallstreetbets" "$BB")
;;       posts (extract-posts search-result)]
;;   (map db/insert-post posts))

;; TODO Scrape comment
;; TODO scrape user
;; TODO scrape daily .clj

(defn -main [& args])
