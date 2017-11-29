(ns app.main
  (:require [mount.core :refer [defstate start stop]]
            [journal.http :as http]
            [journal.graphql :as g]))


(def resolver-map
  {:query/person (fn [a b c] (do{:email "hi"}))
   :query/group (fn [a b c] (do{:name "hi"}))})

(defn startapp[]
  (let [gql-schema (g/graphql-schema-from-edn-file "resources/converted.edn" resolver-map)
        httpserver (http/start-server gql-schema)]
    {:http httpserver}))

(defn stopapp [{server :http}]
  (server :timeout 100))

(defstate app-state :start (startapp)
  :stop (stopapp :timeout 100))

(defn -main [& args]
  (start))
