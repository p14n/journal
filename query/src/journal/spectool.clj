(ns journal.spectool
  (:require [clojure.spec.alpha :as spec]
            [spec-tools.visitor :as visitor]))

(defn convert-spec-object [so]
  (let [specs (atom {})]
    (visitor/visit
     so
     (fn [_ spec _ _]
       (if-let [s (spec/get-spec spec)]
         (swap! specs assoc spec (spec/form s))
         @specs)))))

(defn simple-name [namespaced-key]
  (keyword (name namespaced-key)))

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


(defn graphql-type [field {refs :refs
                           object-set :object-set
                           wrapfunc :wrapfunc
                           mappingfunc :typemappingfunc :as opts}]
  (let [mapped (mappingfunc field)]
    (cond
      (not (nil? mapped)) mapped
      (contains? object-set field) (wrapfunc (simple-name field))
      (coll? field) (map #(graphql-type % opts) field)
      (= field 'clojure.core/string?) (wrapfunc (symbol "String"))
      (= field 'clojure.spec.alpha/coll-of) (symbol "list")
      (contains? refs field) (graphql-type (refs field) opts)
      :default (wrapfunc (simple-name field)))))

(defn create-field [field {fields-info :fields-info :as opts} ]
  {(simple-name field) (merge (field fields-info)
                              {:type (graphql-type field opts)})})

(defn not-null-wrapper [x]
  `(~(symbol "non-null") ~x))

(defn create-lacinia-object [[spec-key spec-converted] object-set mappingfunc]
  (let [refs (:ref spec-converted)
        spec-object (:object spec-converted)
        fields-info (get-in spec-converted [:other :fields])
        opts {:refs refs
              :fields-info fields-info
              :object-set object-set
              :typemappingfunc mappingfunc}]
    {(simple-name spec-key)
     (merge (dissoc (:other spec-converted) :args)
            {:fields
             (apply merge
                    (apply conj
                           (map
                            #(create-field % (assoc opts :wrapfunc not-null-wrapper))
                            (:req spec-object))
                           (map
                            #(create-field % (assoc opts :wrapfunc identity))
                            (:opt spec-object))))})}))


(defn convert-to-object-tuples [app-schema]
  (map convert-spec-object-tuple-to-data
       (:objects app-schema)))


(defn create-query-args [fields opts]
  (let [args-info (:args-info opts)
        filtered (filter #(contains? args-info %) fields)
        converted (into {} (map #(create-field % opts) filtered))]
    converted))

(defn create-lacinia-id-query [[spec-key spec-object] object-set mappingfunc]
  (let [type-name (name spec-key)
        query-name (.toLowerCase type-name)
        opts {:refs (:ref spec-object)
              :fields-info (get-in spec-object [:other :fields])
              :args-info (get-in spec-object [:other :args])
              :object-set object-set
              :typemappingfunc mappingfunc
              :wrapfunc identity }
        all-fields (apply conj (get-in spec-object [:object :req])
                          (get-in spec-object [:object :opt]))]
    {(keyword query-name)
     {:type (simple-name spec-key)
      :description (str "Access a " type-name)
      :args (create-query-args all-fields opts)
      :resolve (keyword "query" query-name) }}))

(defn convert-to-graphql [refactored-object-tuples typemappingfunc]
  (let [object-set (set (map first refactored-object-tuples))
        lacinia-objects (into {} (map #(create-lacinia-object % object-set typemappingfunc)
                                      refactored-object-tuples))
        lacinia-queries (into {} (map #(create-lacinia-id-query % object-set typemappingfunc) refactored-object-tuples))]
    {:objects lacinia-objects :queries lacinia-queries}))
