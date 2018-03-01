(ns journal-command.system
	 (:require [clojure.core.async :refer [chan <!!]]
            [clojure.java.io :refer [resource]]
            [mount.core :refer [defstate start stop]]
            [onyx.plugin.core-async]
            [onyx.api]))

(defn startapp[]
  (println "Starting Onyx development environment")
    (let [onyx-id (java.util.UUID/randomUUID)
          env-config (assoc (-> "env-config.edn" resource slurp read-string)
                            :onyx/tenancy-id onyx-id)
          peer-config (assoc (-> "dev-peer-config.edn"
                                 resource slurp read-string) :onyx/tenancy-id onyx-id)
          env (onyx.api/start-env env-config)
          peer-group (onyx.api/start-peer-group peer-config)
          peers (onyx.api/start-peers n-peers peer-group)]
      (assoc {} :env env :peer-group peer-group
             :peers peers :onyx-id onyx-id)))

(defn stopapp [component]
(println "Stopping Onyx development environment")

    (doseq [v-peer (:peers component)]
      (onyx.api/shutdown-peer v-peer))

    (onyx.api/shutdown-peer-group (:peer-group component))
    (onyx.api/shutdown-env (:env component))

    (assoc component :env nil :peer-group nil :peers nil)))

(defstate app-state
  :start (startapp)
  :stop (stopapp app-state))

(defn -main [& args]
  (start))
