(ns journal.spectool-test
  (:require  [clojure.test :as t]
             [clojure.java.io :as io]
             [clojure.edn :as edn]
             [journal.spectool :as pst]
             [journal.graphql :as g]
             [clojure.spec.alpha :as spec]
             [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
             [com.walmartlabs.lacinia.schema :as schema]
             [com.walmartlabs.lacinia :refer [execute]]))

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
                       :fields { ::groups { :description
                                           "Groups this person belongs to"
                                           :resolve :Person/groups}}}
             ::Group {:description "A group of people"
                      :fields { ::people {:description "People in this group"
                                          :resolve :Group/people}}}}
   :queries [::Person ::Group]})


(def schema-map
  (read-string
   (g/read-edn-from-file "test-resources/target-schema.edn")))

(def converted (pst/convert-to-object-tuples app-schema))

(def graphql (pst/convert-to-graphql converted))

(t/deftest verify-object-conversion
  (t/is (= (:objects schema-map) (:objects graphql))))

(t/deftest verify-query-conversion
  (t/is (= (:queries schema-map) (:queries graphql))))
