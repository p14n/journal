(ns p14n.spectool-test
  (:require  [clojure.test :as t]
             [clojure.java.io :as io]
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

(def schema-map
  (read-string
   (read-edn-from-file "test-resources/target-schema.edn")))

(def converted (pst/convert-to-object-tuples ps/app-schema))

(def graphql (pst/convert-to-graphql converted))

                                        ;(clojure.pprint/pprint (keys  schema-map))
;(println "+++++++++++++++++")
;(clojure.pprint/pprint (:objects schema-map))
;(println "**********************")
                                        ;(clojure.pprint/pprint (:objects graphql))

(->> graphql
    (prn-str)
    (spit "test-resources/converted.edn"))

(def actual-schema
  (read-string
   (read-edn-from-file "test-resources/converted.edn")))

(t/deftest verify-object-conversion
  (t/is (= (:objects schema-map) (:objects actual-schema))))

(t/deftest verify-query-conversion
  (t/is (= (:queries schema-map) (:queries actual-schema))))

(def graphql-schema
  (-> actual-schema
      (attach-resolvers {:query/Person-by-id (constantly {})
                         :query/Group-by-id (constantly {})
                         :Person/groups (constantly {})
                         :Group/people (constantly {})})
      schema/compile))

(clojure.pprint/pprint (execute graphql-schema "{ query { Person_by_id(iden: 1) { email } } }" nil nil))


