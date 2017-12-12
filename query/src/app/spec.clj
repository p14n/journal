(ns app.spec
  (:require [clojure.spec.alpha :as spec]
            [journal.spectool.core :as st]
            [com.walmartlabs.lacinia.executor :as executor]
            [journal.database :refer [conn
                                      mutate-function
                                      query-from-selection]]))

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
                                           "Groups this person belongs to"}}}
             ::Group {:description "A group of people"
                      :args #{::name ::ID}
                      :fields { ::people {:description "People in this group"}}}}})

(defn is-id?[field] (= (name field) "ID"))

(defn type-mapping-function[field]
  (if (= field ::ID) (symbol "ID") nil))

(defn query-function []
  (fn [ctx args val]
    (try (query-from-selection (executor/selections-tree ctx) is-id?)
         (catch Exception e (do (.printStackTrace e) (throw e))))))

(defn resolver-map []
  {:query/person (query-function)
   :query/group (query-function)
   :mutation/addPerson (mutate-function "Person" conn)
   :mutation/addGroup (mutate-function "Group" conn)
   :mutation/changePerson (mutate-function "Person" conn)
   :mutation/changeGroup (mutate-function "Group" conn)})


(defn write-app-schema []
  (let [converted (st/convert-to-object-tuples app-schema)
        tographql (st/convert-to-graphql converted
                                         type-mapping-function
                                         '::ID)]
    (do
      (st/write-datomic-schema-files converted "./resources/datomic/")
      (->> tographql
             (prn-str)
             (spit "./resources/graphql.edn")))))


