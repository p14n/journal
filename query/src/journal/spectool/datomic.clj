(ns journal.spectool.datomic
  (:use [journal.spectool.shared]
        [datomic.api :only [q db] :as d]
        [clojure.string :only [join]]))


;;look at objects
;;object name becomes namespace
;;field name is item name
;;type is type

(defn datomic-type[field object-set]
  (cond
    (coll? field) (first (remove nil? (map #(datomic-type % object-set) field)))
    (= field 'clojure.core/string?) "string"
    (= field 'clojure.core/int?) "long"
    (contains? object-set field) "ref"
    :default nil))

(defn is-many [field]
  (and
   (coll? field)
   (contains? (set field) 'clojure.spec.alpha/coll-of)))

(defn create-datomic-field[ns name ismany type]
  {
   ;;:db/id #db/id[:db.part/db]
   :db/ident  (keyword ns name)
   :db/cardinality (if ismany :db.cardinality/many :db.cardinality/one)
   :db/valueType (keyword "db.type" type)
   :db.install/_attribute :db.part/db
   })

(defn create-datomic-fields [schema-object object-set]
  (let [object-name (name (get-in schema-object [:object :def]))
        fields (apply conj (get-in schema-object [:object :req])
                      (get-in schema-object [:object :opt]))
        ref (:ref schema-object)]
    {object-name (map #(create-datomic-field object-name
                                              (name %)
                                              (is-many (ref %))
                                              (datomic-type (ref %) object-set)) fields)}))

(defn create-datomic-schema [object-tuples]
  (let [object-name-set (set (map first object-tuples))
        object-list (map second object-tuples)
        fields (map #(create-datomic-fields % object-name-set) object-list)]
    (apply merge fields)))

(defn create-field-schema-string [field-parts]
  (str "{ :db/id #db/id[:db.part/db] "
       (join " " (map (fn [[k v]] (str " " k " " v)) field-parts))
       " }\n"))

(defn create-datomic-schema-string [datomic-fields]
  (map create-field-schema-string datomic-fields))

(defn write-datomic-schema-files[converted dir]
  (let [datomic-schema (create-datomic-schema converted)]
    (doall (map (fn [[name fields]] (->> fields
                                         (create-datomic-schema-string)
                                         (join)
                                         (#(str "[\n" % "]\n"))
                                         (spit (str dir "/" name ".edn"))))
                datomic-schema))))
