(ns journal.database
  (:require [datomic.api :only [q db] :as d]
            [mount.core :refer [defstate start]]
            [clojure.java.io :refer [file]]))

(defn install-base-schema-file[conn file]
  @(d/transact
    conn (read-string (slurp file))))

(defn install-base-schema [conn]
  (let [dir (file "./resources/datomic")
        files (rest (file-seq dir))]
    (map #(install-base-schema-file conn (.getPath %)) files)))

(defn setup-and-connect-to-db [uri]
  (do (d/create-database uri)
      (let [conn (d/connect uri)]
        (install-base-schema conn)
        conn)))

(defn close-db []
  (do (d/shutdown false)
      ))

(defstate conn :start (setup-and-connect-to-db "datomic:mem://journal")
  :stop (close-db))



