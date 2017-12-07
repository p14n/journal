(ns journal.spectool-test
  (:require  [clojure.java.io :as io]
             [clojure.edn :as edn]
             [journal.spectool.core :as pst]
             [journal.spectool.datomic :as dst]
             [journal.graphql :as g]
             [clojure.spec.alpha :as spec]
             [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
             [com.walmartlabs.lacinia.schema :as schema]
             [com.walmartlabs.lacinia :refer [execute]]
             [clojure.pprint :refer [pprint]])
  (:use [clojure.test]))

(spec/def ::firstname string?)
(spec/def ::lastname string?)
(spec/def ::name string?)
(spec/def ::email string?)
(spec/def ::ID int?)

(spec/def ::groups (spec/coll-of ::Group))

(spec/def ::people (spec/coll-of ::Person))

(spec/def ::Person
  (spec/keys
   :req [::email ::ID]
   :opt [::firstname ::lastname ::groups]))

(spec/def ::Group
  (spec/keys
   :req [::name ::ID]
   :opt [::people]))

(def app-schema
  {:objects {::Person {:description "A person in the system"
                       :args #{::email ::ID}
                       :unique #{::email}
                       :fields { ::groups { :description
                                           "Groups this person belongs to"
                                           :resolve :Person/groups}}}
             ::Group {:description "A group of people"
                      :args #{::name ::ID}
                      :fields {::people {:description "People in this group"
                                          :resolve :Group/people}}}}
   :queries [::Person ::Group]})


(def schema-map
  (read-string
   (g/read-edn-from-file "test-resources/target-schema.edn")))

(def converted (pst/convert-to-object-tuples app-schema))

;;(pprint converted)

(def datomic-schema (pst/create-datomic-schema converted))

(pprint datomic-schema)

(defn type-mapping-func [field]
  (if (= field '::ID) (symbol "ID") nil))

(def graphql (pst/convert-to-graphql converted type-mapping-func '::ID))

(deftest graphql-conversion
  (testing "Graphql conversion"
    (testing "to objects"
      (is (= (:objects schema-map) (:objects graphql))))
    (testing "to queries"
      (is (= (:queries schema-map) (:queries graphql))))
    (testing "to mutations"
      (is (= (:mutations schema-map) (:mutations graphql))))))

(deftest datomic-conversion
  (testing "Datomic conversion"
    (testing "identifies many relationships"
      (is (dst/is-many '(::Group clojure.spec.alpha/coll-of))))
    (testing "contains Person and Group"
      (do (is (= "Person" (first (keys datomic-schema))))
          (is (= "Group" (second (keys datomic-schema))))))))



