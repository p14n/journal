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
  (let [mapped (if (= field ::ID) (symbol "ID") nil)]
    mapped))

;; (deftask write-schema []
;;   (require 'app.spec)
;;   (require 'journal.spectool)
;;   (let [type-mapping-function (resolve 'app.spec/type-mapping-function)
;;         convert-object (resolve 'journal.spectool/convert-to-object-tuples)
;;         app-schema (var-get (resolve 'app.spec/app-schema))
;;         convert-graphql (resolve ')
;;         converted (convert-object app-schema)
;;         tographql (convert-graphql converted type-mapping-function)
;;         x (println tographql)]
;;     ))

(def app-schema
  {:objects {::Person {:description "A person in the system"
                       :fields { ::groups { :description
                                           "Groups this person belongs to"
                                           :resolve :Person/groups}}}
             ::Group {:description "A group of people"
                      :fields { ::people {:description "People in this group"
                                          :resolve :Group/people}}}}
   :queries [::Person ::Group]})

(defn write-app-schema []
  (let [converted (st/convert-to-object-tuples app-schema)
        tographql (st/convert-to-graphql converted
                                                       type-mapping-function)]
    (->> tographql
         (prn-str)
         (spit "./resources/converted.edn"))))


