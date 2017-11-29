(ns p14n.graphql
  (:require  [clojure.java.io :as io]
             [clojure.edn :as edn]
             [p14n.spec :as ps]
             [p14n.spectool :as pst]
             [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
             [com.walmartlabs.lacinia.schema :as schema]
             [com.walmartlabs.lacinia :refer [execute]]))

(defn read-edn-from-file [name]
  (with-open [in (java.io.PushbackReader.
                  (io/reader name))]
    (let [edn-seq (repeatedly (partial edn/read {:eof :theend} in))]
      (apply str (take-while (partial not= :theend) edn-seq)))))

(def schema-from-file
  (read-string
   (read-edn-from-file "resources/converted.edn")))

(def graphql-schema
  (-> schema-from-file
      (attach-resolvers {:query/Person-by-id (fn [a b c] (do{:email "hi"}))
                         :query/Group-by-id (constantly {})
                         :Person/groups (constantly {})
                         :Group/people (constantly {})})
      schema/compile))
