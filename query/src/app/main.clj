(ns app.main
  (:require [mount.core :refer [defstate start stop]]
            [journal.http :as http]
            [journal.graphql :as g]
            [app.spec :as as]
            [journal.database :refer [conn
                                      install-base-schema]]
            [clojure.pprint :refer [pprint]]))

(defn startapp[]
  (let [db-install (install-base-schema conn)
        gql-schema (g/graphql-schema-from-edn-file "resources/graphql.edn" (as/resolver-map))
        httpserver (http/start-server gql-schema)]
    {:http httpserver}))

(defn stopapp [{server :http}]
  (server :timeout 100))

(defstate app-state
  :start (startapp)
  :stop (stopapp app-state))

(defn -main [& args]
  (start))
