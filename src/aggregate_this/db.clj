(ns aggregate-this.db
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "db/database.db"})

;; Models
(defrecord RPost [id link upvote title])
(defrecord RComment [commentid parentid postid user upvote body])

(def reddit-post-ddl
  (jdbc/create-table-ddl :redditpost
                         [[:id "integer primary key autoincrement"]
                          [:link "text"]
                          [:upvote "integer"]
                          [:title "text"]]))

(def reddit-postcomment-ddl
  (jdbc/create-table-ddl :redditpostcomment
                         [[:commentid "text"]
                          [:parentid "text"]
                          [:postid "integer"]
                          [:user "text"]
                          [:upvote "integer"]
                          [:body "text"]
                          ["FOREIGN KEY(\"postid\") REFERENCES \"redditpost\"(\"id\")"]]))

(defn create-tables []
  (jdbc/db-do-commands db-spec
                       ["PRAGMA foreign_keys = ON;"
                        reddit-post-ddl
                        reddit-postcomment-ddl
                        "CREATE UNIQUE INDEX link_ix ON redditpost (link);"
                        "CREATE UNIQUE INDEX comment_id_ix ON redditpostcomment (commentid)"]))

(defn select-all-posts []
  (jdbc/query db-spec ["select * from redditpost"]))

(defn insert-post [post]
  (try
    (jdbc/insert! db-spec :redditpost post)
    (catch org.sqlite.SQLiteException  e
      (println e)
      (println "[*] ignoring duplicate:" post))))

(defn upsert-comment [comment]
  (when (:commentid comment)
    (try
      (jdbc/insert! db-spec :redditpostcomment comment)
      (catch org.sqlite.SQLiteException  e
        (println "[*] Updating instead of inserting:" comment)
        (jdbc/update! db-spec :redditpostcomment comment ["commentid = ?" (:commentid comment)])))))

(defn get-redditpost-by-id [post-id]
  (map->RPost
   (first (jdbc/query db-spec
                      ["select * from redditpost where id = ?" post-id]))))

(defn get-redditpostcomment-by-id [comment-id]
  (map->RComment
   (first (jdbc/query db-spec
                      ["select * from redditpostcomment where commentid = ?" comment-id]))))

(defmulti get-id class)
(defmethod get-id RPost [r-post] (:id r-post))
(defmethod get-id RComment [r-comment] (:commentid r-comment))

(defmulti persist class)
(defmethod persist RPost [r-post]
  (insert-post r-post))
(defmethod persist RComment [r-comment]
  (upsert-comment r-comment))

;; https://statcompute.wordpress.com/2018/03/12/clojure-and-sqlite/
;; (create-tables)
;; (insert-post {:link "test" :title "test"})
;; TODO Add creationdate

(defn -main []
  (create-tables)
  (println "Done!"))
