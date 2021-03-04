(ns aggregate-this.core
  (:require [net.cgrand.enlive-html :as html]
            [aggregate-this.db :as db]
            [clj-http.client :as client]))

(def HOST "https://old.reddit.com")

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

(defn cleanup-url [url]
  (when (and (not (nil? url))(re-find #"\/r\/" url))
    (->> url
         (#(clojure.string/split % #"\/"))
         drop-last
         (clojure.string/join "/"))))
;;(cleanup-url "https://old.reddit.com https://old.reddit.com/r/wallstreetbets/comments/lv5exe/gme_to_the_moon_boiz_lets_get_it/gpbwn9x/")

;; https://old.reddit.com/user/The-Zombie-ZAR
(defn scrape-user [url]
  (let [body (:body (http-get url))
        html-snippet (html/html-snippet body)
        comments (html/select html-snippet [(html/attr= :data-type "comment")])
        links (html/select html-snippet [(html/attr= :data-type "link")])]
    (->>
     (doall (map #(->> %
                       :attrs
                       :data-permalink
                       cleanup-url)
                 (merge comments links)))
     (remove nil?)
     dedupe)))
;; (def r-u-links (scrape-user "https://old.reddit.com/user/The-Zombie-ZAR"))

(defn scrape-post [url]
  (let [body (:body (http-get url))
        html-snippet (html/html-snippet body)
        upvote (get-post-upvoted html-snippet)
        title (get-post-title html-snippet)]
    (db/map->RPost
     {:link url
      :title title
      :upvote upvote})))

(defn extract-posts [dom]
  (->> dom
       (map (comp :href :attrs first :content))
       (map scrape-post)))

(defn extract-comment
  ([post comment-dom] (extract-comment post comment-dom ""))
  ([post comment-dom parentid]
   (let [extract-child-comments (fn  [post comment-dom parentid]
           (let [child-comments (:content comment-dom)]
             (doall (mapcat #(extract-comment post % parentid) child-comments))))
         comment-body (html/select comment-dom [:.usertext-body])
         comment-attrs (:attrs comment-dom)
         comment-text (apply str (html/select comment-body [:.md]))
         comment-id (:id comment-attrs)
         username (:data-author comment-attrs)
         upvote (:title (:attrs (first (html/select comment-dom [:.score.unvoted]))))
         comment (db/map->RComment
                  {:postid (:id post)
                   :parentid parentid
                   :commentid comment-id
                   :user username
                   :upvote upvote
                   :body comment-text})]
     (concat [comment] (extract-child-comments post (first (html/select comment-dom [:.sitetable])) comment-id)))))

(defn scrape-comment [reddit-post-id]
  (let [post (db/get-post-by-id reddit-post-id)
        body (:body (http-get (:link post)))
        comments-dom (html/select (html/html-snippet body) [:.nestedlisting :> :div.comment])]
    (doall (mapcat #(extract-comment post % "top") comments-dom))))

;; REPL
;; (def r-post (db/get-post-by-id 1))
;; (def r-body (:body (http-get (:link r-post))))
;; (def comments-dom (html/select (html/html-snippet r-body) [:.nestedlisting :> :div.comment]))
;; (def f-c (first comments-dom))
;; (def f-c-text (html/select f-c [:.usertext-body]))
;; (def f-c-comment-text (first (:content (first (html/select f-c-text [:.md :p])))))
;; (def f-cs (scrape-comment 1))
;; (def f-cc (extract-comment r-post f-c "vvv"))

;; (def c-body (:body (http-get "https://old.reddit.com/r/math/comments/6hu1ph/math_is_just_beautiful/")))
;; (spit "./test.html" c-body)
;; (println (html/select (html/html-snippet c-body) [:#thing_t1_dj1pz4t]))
;; (def cc-body (html/select (html/html-snippet c-body) [:#thing_t1_dj1pz4t]))
;; (println (first cc-body))
;; (println (mapcat (comp :content) (html/select cc-body [:.md :p])))
;; (db/upsert-comment (first (extract-comment {:id 1} (html/select (html/html-snippet c-body) [:#thing_t1_dj1pz4t]) "top")))

;; (def c (db/get-redditpostcomment-by-id "thing_t1_goevqjt"))
;; (html/select (read-string (:body c)) [:.md])

;; (let [search-result (search-reddit "r/wallstreetbets" "$ZOM")
;;       posts (extract-posts search-result)]
;;   (map db/persist posts))

;; (let [posts (db/select-all-posts)]
;;   (doseq [post posts]
;;     (let [comments (scrape-comment (:id post))]
;;       (doseq [comment comments]
;;         (db/persist comment)))))

;; TODO Testing
;; TODO Frontend
;; TODO scrape daily .clj
;; TODO Plug REBL

(defn -main [& args])
