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

(defn to-tx-data [object-name args]
  (into {} (map (fn [[k v]]
                  (do [(keyword object-name (name k))
                       v])) args )))

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

(defn run-query [db pattern lookup]
  (println (str "pattern/lookup "  pattern lookup))
  (let [res (if (nil? lookup)
              (map first (d/q pattern db))
              (let [maybe-one (d/pull db pattern lookup)]
                (if (nil? maybe-one) [] [maybe-one]))) 
        x (println res)] res))

(defn to-where [object-name args entity-symbol is-id?]
  (map (fn[[k v]](if (is-id? k)
                [entity-symbol (Long/parseLong v)]
                [entity-symbol (keyword object-name (name k)) v])) args))

(defn query-from-selection
  ([object-name selection-tree args is-id? db]
   (let [pattern (to-query selection-tree is-id?)
         by-id (some is-id? (keys args))
         res (if by-id
               (let [id-key (first (filter is-id? (keys args)))
                     id-val (Long/parseLong (args id-key))]
                 (run-query db pattern id-val))
               (let [where-clause (to-where object-name args (symbol "?e") is-id?)]
                 (run-query db `[:find (~(symbol "pull") ~(symbol "?e") ~pattern) :where ~@where-clause] nil)))
         replaced (replace-id-in-results res #(= :db/id %))
         x (println replaced)]
     replaced
     ))

  ([object-name selection-tree args is-id?]
   (query-from-selection object-name selection-tree args is-id? (d/db conn))))

(defn upsert-entity
  "Takes transaction data and returns the resolved tempid"
  [con tx-data]
  (let [had-id (contains? tx-data :db/id)
        data-with-id (if had-id
                       tx-data
                       (assoc tx-data :db/id #db/id[:db.part/user -1000001]))
        data-as-vec (vec (flatten (vec data-with-id)))
        ;;xxx (println data-with-id)
        tx @(d/transact con [data-with-id])]
    (if had-id (assoc tx :ID (tx-data :db/id))
        (assoc tx :ID (d/resolve-tempid (d/db con) (:tempids tx)
                                        (d/tempid :db.part/user -1000001))))))

(defn resolve-entity "Takes a db id and db and returns the entity"
  [db id] (into {:ID id :db/id id} (d/touch (d/entity db id))))

(defn keyname-only [m]
              (into {}
                    (map (fn [[k v]]
                           (cond
                             (map? v) [(keyword (name k)) (keyname-only v)]
                             :default [(keyword (name k)) v]))) m))

