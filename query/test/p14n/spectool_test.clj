(ns p14n.spectool-test
  (:require  [clojure.test :as t]
             [clojure.java.io :as io]
             [clojure.edn :as edn]
             [p14n.spec :as ps]
             [p14n.spectool :as pst]
             [p14n.graphql :as g]
             [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
             [com.walmartlabs.lacinia.schema :as schema]
             [com.walmartlabs.lacinia :refer [execute]]))



(def schema-map
  (read-string
   (g/read-edn-from-file "test-resources/target-schema.edn")))

(def converted (pst/convert-to-object-tuples ps/app-schema))

(def graphql (pst/convert-to-graphql converted))

                                        ;(clojure.pprint/pprint (keys  schema-map))
;(println "+++++++++++++++++")
;(clojure.pprint/pprint (:objects schema-map))
;(println "**********************")
                                        ;(clojure.pprint/pprint (:objects graphql))

(->> graphql
    (prn-str)
    (spit "resources/converted.edn"))

(t/deftest verify-object-conversion
  (t/is (= (:objects schema-map) (:objects graphql))))

(t/deftest verify-query-conversion
  (t/is (= (:queries schema-map) (:queries graphql))))


(clojure.pprint/pprint (execute g/graphql-schema "{ query { Person_by_id(iden: 1) { email } } }" nil nil))


