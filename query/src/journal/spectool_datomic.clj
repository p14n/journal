(ns journal.spectool-datomic
  (:use [journal.spectool-shared]
        [datomic.api :only [q db] :as d]))


;;look at objects
;;object name becomes namespace
;;field name is item name
;;type is type

(defn datomic-type[field { object-set :object-set }]
  (cond
    (coll? field) (remove nil? (map datomic-type field))
    (= field 'clojure.core/string?) "string"
    (contains? object-set field) "ref"
    :default nil))

(defn is-many [field]
  (and
   (coll? field)
   (contains? field 'clojure.spec.alpha/coll-of)))

(defn create-datomic-field[ns name ismany type]
  (str "{
   :db/id #db/id[:db.part/db]
   :db/ident" ;;(keyword ns name)
   ":db/cardinality" ;;(if ismany :db.cardinality/many :db.cardinality/one)
   ":db/valueType" ;;(keyword "db.type" type)
   ":db.install/_attribute :db.part/db
   }"))

(defn create-datomic-fields [schema-object object-set]
  (let [name (name (get-in schema-object [:object :def]))
        fields (apply conj (get-in schema-object [:object :req])
                      (get-in schema-object [:object :opt]))
        ref (:ref schema-object)]
    (map #(create-datomic-field name (name %) (is-many %) (ref %)) fields)))

(defn create-datomic-schema [object-tuples]
  (let [object-name-set (set (map first object-tuples))
        object-list (map second object-tuples)
        fields (map #(create-datomic-fields % object-name-set) object-list)]
    fields))
