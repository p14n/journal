(ns p14n.spec
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::firstname string?)
(spec/def ::lastname string?)
(spec/def ::name string?)
(spec/def ::email string?)
(spec/def ::ID int?)

(spec/def ::groups (spec/coll-of ::Group))

(spec/def ::people (spec/coll-of ::Person))

(spec/def ::Person (spec/keys
                    :req [::email ::ID]
                    :opt [::firstname ::lastname ::groups]))

(spec/def ::Group (spec/keys
                    :req [::name ::ID]
                    :opt [::people]))

(def app-schema
  {:objects [::Person ::Group]
   :queries [::Person ::Group]})
