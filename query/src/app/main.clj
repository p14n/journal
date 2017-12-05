(ns app.main
  (:require [mount.core :refer [defstate start stop]]
            [journal.http :as http]
            [journal.graphql :as g]
            [journal.database :refer [conn
                                      install-base-schema
                                      mutate-function
                                      query-from-selection]]
            [clojure.pprint :refer [pprint]]
            [com.walmartlabs.lacinia.executor :as executor]))


(defn query-function []
  (fn [ctx args val] (query-from-selection (executor/selections-tree ctx))))

(defn resolver-map []
  {:query/person (query-function)
   :query/group (query-function)
   :mutation/addPerson (mutate-function "Person" conn)
   :mutation/addGroup (mutate-function "Group" conn)
   :mutation/changePerson (mutate-function "Person" conn)
   :mutation/changeGroup (mutate-function "Group" conn)})

(defn startapp[]
  (let [db-install (install-base-schema conn)
        x (println db-install)
        gql-schema (g/graphql-schema-from-edn-file "resources/graphql.edn" (resolver-map))
        httpserver (http/start-server gql-schema)]
    {:http httpserver}))

(defn stopapp [{server :http}]
  (server :timeout 100))

(defstate app-state
  :start (startapp)
  :stop (stopapp app-state))

(defn -main [& args]
  (start))
