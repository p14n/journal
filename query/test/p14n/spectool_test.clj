(ns p14n.spectool-test
  (:require  [clojure.test :as t]
             [clojure.java.io :as io]
             [clojure.edn :as edn]
             [p14n.spec :as ps]
             [p14n.spectool :as pst]))

(t/is (= (pst/apply-to-vals
          {:a {:1 2}}
          #(pst/apply-to-vals % inc))
         {:a {:1 3}}))

(def expected-schema-string
  (with-open [in (java.io.PushbackReader.
                  (io/reader "test-resources/target-schema.edn"))]
    (let [edn-seq (repeatedly (partial edn/read {:eof :theend} in))]
      (apply str (take-while (partial not= :theend) edn-seq)))))

(def schema-map (read-string expected-schema-string))

(def converted (pst/convert-to-graphql ps/app-schema))

(println (pst/convert-and-refactor (first (:objects ps/app-schema))))

(t/is (= schema-map converted))


