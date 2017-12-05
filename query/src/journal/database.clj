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
  (do (d/shutdown false)))

(defstate conn
  :start (setup-and-connect-to-db "datomic:mem://journal")
  :stop (close-db))

(defn toTxData [object-name args]
  (into {} (map (fn [[k v]]
                  (do [(keyword object-name (name k))
                       v])) args )))

(defn create-entity
  "Takes transaction data and returns the resolved tempid"
  [con tx-data]
  (let [had-id (contains? tx-data :db/id)
        data-with-id (if had-id
                       tx-data
                       (assoc tx-data :db/id #db/id[:db.part/user -1000001]))
        data-as-vec (vec (flatten (vec data-with-id)))
        tx @(d/transact con [data-with-id])]
    (if had-id (assoc tx :db/id (tx-data :db/id))
        (assoc tx :db/id (d/resolve-tempid (d/db con) (:tempids tx)
                                           (d/tempid :db.part/user -1000001))))))

(defn resolve-entity "Takes a db id and db and returns the entity"
  [db id] (into {:db/id id} (d/touch (d/entity db id))))

(defn mutate-function [object-name conn]
  (fn [ctx args val]
    (->> args
         (toTxData object-name)
         (#(do (println %) %))
         (create-entity conn)
         (#(resolve-entity (:db-after %) (:db/id %))))))
