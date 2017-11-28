(ns p14n.spectool
  (:require [clojure.spec.alpha :as spec]
            [spec-tools.visitor :as visitor]
            [p14n.spec :as ps]))

(defn convert-spec-object [so]
  (let [specs (atom {})]
    (visitor/visit
     so
     (fn [_ spec _ _]
       (if-let [s (spec/get-spec spec)]
         (swap! specs assoc spec (spec/form s))
         @specs)))))

(defn refactor-spec-output [spec-key output]
  (into {:ref output :def spec-key}
        (map vec (partition 2 (drop 1 (spec-key output))))))

(defn convert-spec-object-tuple-to-data [object-tuple]
  (let [converted-key
        (->> (first object-tuple)
             (convert-spec-object)
             (refactor-spec-output (first object-tuple)))]
    [(first object-tuple) {:object (dissoc converted-key :ref)
                           :ref (:ref converted-key)
                           :other (second object-tuple)}]))


(defn graphql-type [refs field object-set wrapfunc]
  (do ;(println object-set)
      ;(println "TYPE " field " " (coll? field) " " (contains? object-set field))
      (cond
        (contains? object-set field) (wrapfunc field)
        (coll? field) (map #(graphql-type refs % object-set wrapfunc) field)
        (= field :p14n.spec/ID) (symbol "ID")
        (= field 'clojure.core/string?) (wrapfunc (symbol "String"))
        (= field 'clojure.spec.alpha/coll-of) (symbol "list")
        (contains? refs field) (graphql-type refs (refs field) object-set wrapfunc)
        :default (wrapfunc field))))

(defn create-field [refs field object-set wrapfunc fields-info]
  {field (merge (field fields-info)
                {:type (graphql-type refs field object-set wrapfunc)})})

(defn not-null-wrapper [x]
  `(~(symbol "non-null") ~x))

(defn create-lacinia-object [[spec-key spec-converted] object-set]
  (let [refs (:ref spec-converted)
        spec-object (:object spec-converted)
        fields-info (get-in spec-converted [:other :fields])]
    {(:def spec-object)
     (merge (:other spec-converted)
            {:fields (apply merge
                            (apply conj
                                   (map #(create-field refs % object-set not-null-wrapper fields-info) (:req spec-object))
                                   (map #(create-field refs % object-set identity fields-info) (:opt spec-object))))})}))


(defn convert-to-object-tuples [app-schema]
  (map convert-spec-object-tuple-to-data
       (:objects app-schema)))

(defn convert-to-graphql [refactored-object-tuples ]
  (let [object-set (set (map first refactored-object-tuples))
        lacinia-objects (into {} (map #(create-lacinia-object % object-set)
                                       refactored-object-tuples))]
    {:objects lacinia-objects}))
