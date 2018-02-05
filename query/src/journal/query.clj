(ns journal.query
  (:require [datomic.api :only [q db] :as d]
            [com.walmartlabs.lacinia.executor :as executor]
            [journal.database :refer [conn
                                      upsert-entity
                                      resolve-entity
                                      keyname-only
                                      query-from-selection
                                      replace-id-in-results
                                      run-query
                                      to-tx-data]]))

(defn query-function [object-name default-search-attribute is-id?]
  (fn [ctx args val]
    (try (let [pattern-lookup (query-from-selection object-name default-search-attribute (executor/selections-tree ctx) args is-id?)
               res (run-query (d/db conn) pattern-lookup)]
           (replace-id-in-results res #(= :db/id %)))
         (catch Exception e (do (.printStackTrace e) (throw e))))))

(defn mutate-function [object-name is-id? con]
  (fn [ctx args val]
    (try (->> args
              (to-tx-data object-name is-id?)
              (upsert-entity con)
              (#(do (println (str "xxxjhfskjhsjkdhf" %)) %))
              (#(resolve-entity (:db-after %) (:ID %)))
              (#(do (println %) %))
              (keyname-only)
              (#(do (println (str "WAT" %)) %)))
         (catch Exception e (do (.printStackTrace e) (throw e))))))

(defn add-attribute-function [args-to-tx con is-id?]
  (try (fn [ctx args val]
         (let [args-vec (args-to-tx args)
               ;;tx-data (vec (apply conj [:db/add] args-vec))
               ;;tx-data (vec (apply conj [:db/add] args-vec))
               sdat ((fn [{p :person g :group}] [{:db/id p :Person/firstname "hry6"}]) {:person 17592186045490})
               v (println (type (second (ffirst args-vec))))
               v2 (println (type (second (ffirst sdat))))
               v3 (println (= (second (ffirst args-vec)) (second (ffirst sdat))))
               tt (println "WWWWW")
               tx @(d/transact con args-vec)
               ttt (println "AAAAWWWWW")
               t (println tx)]
           ;;((query-function is-id?) ctx args val)
           {}))
    (catch Exception e (do (.printStackTrace e) (throw e)))))

