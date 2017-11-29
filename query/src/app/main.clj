(ns app.main
  (:require [mount.core :refer [defstate start stop]]
            [journal.http :as http]
            [journal.graphql :as g]))


(def resolver-map
  {:query/Person-by-id (fn [a b c] (do{:email "hi"}))
   :query/Group-by-id (constantly {})
   :Person/groups (constantly {})
   :Group/people (constantly {})})

(defn startapp[]
  (let [gql-schema (g/graphql-schema-from-edn-file "resources/converted.edn")
        httpserver (http/start-server gql-schema)]
    {:http httpserver}))

(defn stopapp [{server :http}]
  (server :timeout 100))

(defstate app-state :start (startapp)
  :stop (stopapp :timeout 100))

(defn -main [& args]
  (start))
