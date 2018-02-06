(ns journal.query
  (:require [datomic.api :only [q db] :as d]
            [com.walmartlabs.lacinia.executor :as executor]
            [journal.logging :as l]
            [journal.database :refer [conn
                                      upsert-entity
                                      resolve-entity
                                      keyname-only
                                      query-from-selection
                                      replace-id-in-results
                                      run-query
                                      to-tx-data]]))

(defn query-function [object-name default-search-attribute is-id?]
  (fn [{:keys [logger] :as ctx} args val]
    (try (let [pattern-lookup (query-from-selection object-name default-search-attribute (executor/selections-tree ctx) args is-id?)
               res (run-query (d/db conn) (l/info logger :graphql/query pattern-lookup))]
           (l/info logger :graphql/query-response (replace-id-in-results res #(= :db/id %))))
         (catch Exception e (do (l/error logger :graphql/query "Failed to query" e) (throw e))))))

(defn mutate-function [object-name is-id? con]
  (fn [{:keys [logger] :as ctx} args val]
    (try (->> args
              (to-tx-data object-name is-id?)
              (l/info logger :graphql/mutate) 
              (upsert-entity con)
              (#(resolve-entity (:db-after %) (:ID %)))
              (keyname-only)
              (l/info logger :graphql/mutate-response))
         (catch Exception e (do (l/error logger :graphql/mutate "Failed to mutate" e) (throw e))))))

(defn add-attribute-function [args-to-tx con is-id?]
  (fn [{:keys [logger] :as ctx} args val]
    (try 
         (let [args-vec (args-to-tx args)
               a (l/info logger :graphql/add-attribute args-vec)
               tx @(d/transact con args-vec)
               b (l/info logger :graphql/add-attribute-response tx)]
           {})
    (catch Exception e (do (l/error logger :graphql/add-attribute "Failed to add attribute" e) (throw e))))))