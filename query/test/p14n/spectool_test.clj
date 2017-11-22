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

(println schema-map)

(def converted (pst/convert-to-graphql ps/app-schema))
(println converted)

(t/is (= schema-map converted))

