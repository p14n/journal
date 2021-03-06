(ns journal.database
  (:require [datomic.api :only [q db] :as d]
            [mount.core :refer [defstate start]]
            [clojure.java.io :refer [file]]
            [journal.logging :as l]))

(defn install-base-schema-file[conn file]
  (do 
    (l/info l/systemlog :startup/datomic (str "Install schema " file ))
    (l/info l/systemlog :startup/datomic @(d/transact conn (read-string (slurp file))))))
  
(defn install-base-schema [conn]
  (let [dir (file "./resources/datomic")
        files (rest (file-seq dir))]
    (doall (map #(install-base-schema-file conn (.getPath %)) files))))

(defn setup-and-connect-to-db [uri]
  (do (l/info l/systemlog :startup/datomic (str "Starting Datomic at " uri))
      (d/create-database uri)
      (d/connect uri)))

(defn close-db []
  (do (d/shutdown false)))

(defstate conn
  :start (setup-and-connect-to-db "datomic:mem://journal")
  :stop (close-db))

(defn to-tx-data [object-name is-id? args]
  (into {} (map (fn [[k v]]
                  (if (is-id? k)
                    (do [:db/id (Long/parseLong v)])
                    (do [(keyword object-name (name k))
                         v]))) args )))

(defn to-query [selection-tree is-id?]
  (vec (map (fn [[k v]]
              (cond
                (is-id? k) :db/id
                (nil? v) [k :as (keyword (name k))]
                (contains? v :selections) { [k :as (keyword (name k))]
                                           (to-query (:selections v) is-id?)}
                :default nil)) selection-tree)))

(defn replace-id-in-result [res is-id?]
  (if (map? res)
    (into {}
          (map
           (fn [[k v]]
             [(if (is-id? k) :ID k) (replace-id-in-result v is-id?)]) res))
    res))

(defn replace-id-in-results [res is-id?]
  (map #(replace-id-in-result % is-id?) res))

(defn run-query [db {pattern :pattern lookup :lookup}]
  (let [res (if (nil? lookup)
              (map first (d/q pattern db))
              (let [maybe-one (d/pull db pattern lookup)]
                (if (nil? maybe-one) [] [maybe-one])))] 
  res))

(defn to-where [object-name args entity-symbol is-id?]
  (map (fn[[k v]](if (is-id? k)
                [entity-symbol (Long/parseLong v)]
                [entity-symbol (keyword object-name (name k)) v])) args))

(defn query-from-selection "Runs a query from a selection tree and returns the result"
  ([object-name default-search-attribute selection-tree args is-id?]
   (let [pattern (to-query selection-tree is-id?)
         by-id (some is-id? (keys args))]
     (if by-id
       (let [id-key (first (filter is-id? (keys args)))
             id-val (Long/parseLong (args id-key))]
         {:pattern pattern :lookup id-val})
       (let [where-clause (to-where object-name args (symbol "?e") is-id?)]
         (if (empty? where-clause)
           {:pattern  `[:find (~(symbol "pull") ~(symbol "?e") ~pattern) :where [~(symbol "?e") ~(keyword object-name default-search-attribute)]] :lookup nil}
           {:pattern  `[:find (~(symbol "pull") ~(symbol "?e") ~pattern) :where ~@where-clause] :lookup nil}
           )
         )))))

(defn upsert-entity
  "Takes transaction data and returns the resolved tempid"
  [logger con tx-data]
  (let [had-id (contains? tx-data :db/id)
        maybe-id (d/tempid :db.part/user)
        data-with-id (if had-id
                       tx-data
                       (assoc tx-data :db/id maybe-id))
        data-as-vec (l/info logger :datomic/upsert (vec (flatten (vec data-with-id))))
        tx (l/info logger :datomic/upsert-response @(d/transact con [data-with-id]))]
    (if had-id (assoc tx :ID (tx-data :db/id))
        (assoc tx :ID (d/resolve-tempid (d/db con) (:tempids tx) maybe-id)))))

(defn resolve-entity "Takes a db id and db and returns the entity"
  [db id] (into {:ID id :db/id id} (d/touch (d/entity db id))))

(defn keyname-only [m]
              (into {}
                    (map (fn [[k v]]
                           (cond
                             (map? v) [(keyword (name k)) (keyname-only v)]
                             :default [(keyword (name k)) v]))) m))

