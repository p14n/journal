(ns app.spec
  (:require [clojure.spec.alpha :as spec]
            [journal.spectool.core :as st]
            [com.walmartlabs.lacinia.executor :as executor]
            [journal.database :refer [conn]]
            [journal.query :refer [mutate-function
                                   add-attribute-function
                                   query-function]]))

(spec/def ::firstname string?)
(spec/def ::lastname string?)
(spec/def ::name string?)
(spec/def ::email string?)
(spec/def ::ID int?)
(spec/def ::title string?)

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
                                           "Groups this person belongs to"}}}
             ::Group {:description "A group of people"
                      :args #{::name ::ID}
                      :unique #{::name}
                      :fields { ::people {:description "People in this group"}}}}})

(defn is-id?[field] (= (name field) "ID"))

(defn type-mapping-function[field]
  (if (= field ::ID) (symbol "ID") nil))

(defn q [object-name default-search-attribute] (query-function object-name default-search-attribute is-id?))
(defn string-to-long [as-string] (Long/parseLong as-string))

(defn resolver-map []
  {:query/person (q "Person" "email")
   :query/group (q "Group" "name")
   :mutation/addPerson (mutate-function "Person" is-id? conn)
   :mutation/addGroup (mutate-function "Group" is-id? conn)
   :mutation/changePerson (mutate-function "Person" is-id? conn)
   :mutation/changeGroup (mutate-function "Group" is-id? conn)
   :mutation/addGroupToPerson
     (add-attribute-function
      (fn [{p :person g :group}]
        [{:db/id (string-to-long p)
          :Person/groups (string-to-long g)}]) conn is-id?)})

(defn write-app-schema []
  (let [converted (st/convert-to-object-tuples app-schema)
        tographql (st/convert-to-graphql converted
                                         type-mapping-function
                                         '::ID [::addGroupToPerson])]
    (do
      (st/write-datomic-schema-files converted "./resources/datomic/")
      (->> tographql
             (prn-str)
             (spit "./resources/graphql.edn")))))


