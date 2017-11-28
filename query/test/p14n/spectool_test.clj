(ns p14n.spectool-test
  (:require  [clojure.test :as t]
             [clojure.java.io :as io]
             [clojure.edn :as edn]
             [p14n.spec :as ps]
             [p14n.spectool :as pst]))

(def expected-schema-string
  (with-open [in (java.io.PushbackReader.
                  (io/reader "test-resources/target-schema.edn"))]
    (let [edn-seq (repeatedly (partial edn/read {:eof :theend} in))]
      (apply str (take-while (partial not= :theend) edn-seq)))))

(def schema-map (read-string expected-schema-string))

(def converted (pst/convert-to-object-tuples ps/app-schema))

(def graphql (pst/convert-to-graphql converted))
;(clojure.pprint/pprint (keys  schema-map))
;(println "+++++++++++++++++")
;(clojure.pprint/pprint (:objects schema-map))
;(println "**********************")
;(clojure.pprint/pprint (:objects graphql))

(t/deftest verify-object-conversion
  (t/is (= (:objects schema-map) (:objects graphql))))

(t/deftest verify-query-conversion
  (t/is (= (:queries schema-map) (:queries graphql))))





