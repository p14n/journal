(ns app.spec
  (:require [clojure.spec.alpha :as spec]
            [journal.spectool :as st]))

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

(defn type-mapping-function[field]
  (if (= field ::ID) (symbol "ID") nil))

(def app-schema
  {:objects {::Person {:description "A person in the system"
                       :args #{::email ::ID}
                       :fields { ::groups { :description
                                           "Groups this person belongs to"}}}
             ::Group {:description "A group of people"
                      :args #{::name ::ID}
                      :fields { ::people {:description "People in this group"}}}}
   :queries [::Person ::Group]})

(defn write-app-schema []
  (let [converted (st/convert-to-object-tuples app-schema)
        tographql (st/convert-to-graphql converted
                                         type-mapping-function
                                         '::ID)]
    (->> tographql
         (prn-str)
         (spit "./resources/converted.edn"))))


