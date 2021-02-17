(ns aggregate-this.db
  (:require [clojure.java.jdbc :as jdbc]))


(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "db/database.db"})

(def reddit-post-ddl
  (jdbc/create-table-ddl :redditpost
                         [[:id "integer primary key autoincrement"]
                          [:link "varchar(255)"]
                          [:upvote "integer"]
                          [:title "varchar(255)"]]))

(def reddit-postcomment-ddl
  (jdbc/create-table-ddl :redditpostcomment
                         [[:id "integer primary key autoincrement"]
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
                        "CREATE INDEX link_ix ON redditpost (link);"]))

(defn insert-post [post]
  (try
    (jdbc/insert! db-spec :redditpost post)
    (catch org.sqlite.SQLiteException  e
      (println e)
      (println "[*] ignoring duplicate:" post))))

;; https://statcompute.wordpress.com/2018/03/12/clojure-and-sqlite/
(create-tables)
;; (insert-post {:link "test" :title "test"})
;; TODO Add creationdate

(defn -main []
  (create-tables)
  (println "Done!")))
