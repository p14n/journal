(ns command.system
	 (:require [clojure.core.async :refer [chan <!!]]
            [clojure.java.io :refer [resource]]
            [mount.core :refer [defstate start stop]]
            [command.catalog :refer [build-catalog] :as sc]
            [command.workflow :refer [workflow]]
            [command.flow :as sf]
            [command.lifecycle :refer [build-lifecycles] :as sl]
            [onyx.plugin.core-async]
            [onyx.api]))

(defn startapp[]
  (println "Starting Onyx development environment")
    (let [n-peers 1
          onyx-id (java.util.UUID/randomUUID)
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

    (assoc component :env nil :peer-group nil :peers nil))

(defstate app-state
  :start (startapp)
  :stop (stopapp app-state))

(def input-segments
    [{:sentence "Hey there user"}
     {:sentence "It's really nice outside"}
     {:sentence "I live in Redmond"}])

(defn submit-job [] 
  (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
        peer-config (assoc dev-cfg :onyx/tenancy-id (:onyx-id app-state))
        dev-catalog (build-catalog 10 50) 
        dev-lifecycles (build-lifecycles)]
    ;; Automatically pipes the data structure into the channel, attaching :done at the end
    (sl/bind-inputs! dev-lifecycles {:in input-segments})
    (let [job {:workflow workflow
               :catalog dev-catalog
               :lifecycles dev-lifecycles
               :flow-conditions sf/flow-conditions
               :task-scheduler :onyx.task-scheduler/balanced}]
      (onyx.api/submit-job peer-config job)
      ;; Automatically grab output from the stubbed core.async channels,
      ;; returning a vector of the results with data structures representing
      ;; the output.
      (sl/collect-outputs! dev-lifecycles [:loud-output :question-output]))))


(defn -main [& args]
  (start))
