(ns app.main
  (:require [mount.core :refer [defstate start stop]]
            [journal.http :as http]
            [journal.graphql :as g]
            [journal.database :refer [conn install-base-schema]]
            [com.walmartlabs.lacinia.executor :as executor]))


(def resolver-map
  {:query/person (fn [a b c] (do (println (executor/selections-tree a)) {:email "dean@p14n.com"}))
   :query/group (fn [a b c] (do{:name "Group 1"}))
   :mutation/addPerson (fn [a b c] (do{:email "added"}))
   :mutation/addGroup (fn [a b c] (do{:name "added"}))
   :mutation/changePerson (fn [a b c] (do{:email "changed"}))
   :mutation/changeGroup (fn [a b c] (do{:name "changed"}))})

(defn startapp[]
  (let [db-install (install-base-schema conn)
        gql-schema (g/graphql-schema-from-edn-file "resources/graphql.edn" resolver-map)
        httpserver (http/start-server gql-schema)]
    {:http httpserver}))

(defn stopapp [{server :http}]
  (server :timeout 100))

(defstate app-state
  :start (startapp)
  :stop (stopapp app-state))

(defn -main [& args]
  (start))
