(ns journal.query
  (:require [datomic.api :only [q db] :as d]
            [com.walmartlabs.lacinia.executor :as executor]
            [journal.database :refer [conn
                                      upsert-entity
                                      resolve-entity
                                      keyname-only
                                      query-from-selection
                                      to-tx-data]]))

(defn query-function [is-id?]
  (fn [ctx args val]
    (try (query-from-selection (executor/selections-tree ctx) is-id?)
         (catch Exception e (do (.printStackTrace e) (throw e))))))

(defn mutate-function [object-name con]
  (fn [ctx args val]
    (try (->> args
              (to-tx-data object-name)
              ;;              (#(do (println %) %))
              ;;            (#(do (println "mutate") %))
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
               tx @(d/transact con sdat)
               ttt (println "AAAAWWWWW")
               t (println tx)]
           ;;((query-function is-id?) ctx args val)
           {}))
    (catch Exception e (do (.printStackTrace e) (throw e)))))

