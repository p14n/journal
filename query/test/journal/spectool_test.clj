(ns journal.spectool-test
  (:require  [clojure.java.io :as io]
             [journal.spectool.core :as pst]
             [journal.spectool.datomic :as dst]
             [journal.spectool.graphql :as gst]
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

(spec/fdef ::addGroupToPerson
           :args (spec/cat :person ::ID :group ::ID)
           :ret ::Person)

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
                                         :resolve :Group/people}}}}})

(def schema-map
  (read-string
   (g/read-edn-from-file "test-resources/target-schema.edn")))

(defn type-mapping-func [field]
  (let [match (if (= field '::ID) (symbol "ID") nil)]
    match))

(deftest graphql-conversion
  (testing "Graphql conversion"
    (let [converted (pst/convert-to-object-tuples app-schema)
          graphql (pst/convert-to-graphql
                   converted
                   type-mapping-func
                   '::ID)]
      (testing "to objects"
        (is (= (:objects schema-map) (:objects graphql))))
      (testing "to queries"
        (is (= (:queries schema-map) (:queries graphql))))
      (testing "to mutations"
        (is (= (:mutations schema-map) (:mutations graphql))))
      (testing "mutations from function spec"
        (let [converted (gst/mutation-from-spec ::addGroupToPerson type-mapping-func)]
          (is (= [:addGroupToPerson
                  {:args
                   {:person {:type 'ID}
                    :group {:type 'ID}}
                   :resolve :mutation/addGroupToPerson
                   :type :Person}] converted)))))))

(deftest datomic-conversion
  (testing "Datomic conversion"
    (let [converted (pst/convert-to-object-tuples app-schema)
          datomic-schema (pst/create-datomic-schema converted)]
      (testing "identifies many relationships"
        (is (dst/is-many '(::Group clojure.spec.alpha/coll-of))))
      (testing "contains Person and Group"
        (do (is (= "Person" (first (keys datomic-schema))))
            (is (= "Group" (second (keys datomic-schema)))))))))

(deftest datomic-field-creation
  (testing "A datomic field is correctly created"
    (testing "Creates unique single field"
      (is (= {:db/cardinality :db.cardinality/one,
              :db/ident :Person/email,
              :db/unique :db.unique/identity,
              :db/valueType :db.type/string,
              :db.install/_attribute :db.part/db}
             (dst/create-datomic-field "Person" "email" false "string" true))))
    (testing "Creates non-unique collection field"
      (is (= {:db/cardinality :db.cardinality/many,
              :db/ident :Person/middlenames,
              :db/valueType :db.type/string,
              :db.install/_attribute :db.part/db}
             (dst/create-datomic-field "Person" "middlenames" true "string" false))))))

(deftest datomic-types
  (testing "All datomic types are coverted"
    (testing "String" (is (= "string"
                    (dst/datomic-type 'clojure.core/string? {}))))
    (testing "String in collection" (is (= "string"
                    (dst/datomic-type ['clojure.core/string?] {}))))
    (testing "Long"(is (= "long"
                    (dst/datomic-type 'clojure.core/int? {}))))
    (testing "Reference" (is (= "ref"
                    (dst/datomic-type :Person #{:Person}))))))
