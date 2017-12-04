(ns journal.spectool.graphql
  (:use [journal.spectool.shared]))

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

(defn list-field-type? [t]
  (and (coll? t)
       (= (first t) (symbol "list"))))

(defn create-query-args [fields opts]
  (let [field-maps (map #(create-field % opts) fields)
        single-only (remove #(list-field-type? (-> % vals first :type)) field-maps)]
    (into {} single-only)))

(defn create-lacinia-query [[spec-key spec-object] object-set mappingfunc]
  (let [type-name (name spec-key)
        query-name (.toLowerCase type-name)
        opts {:refs (:ref spec-object)
              :fields-info (get-in spec-object [:other :fields])
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

(defn to-lacinia-map [object-set typemappingfunc]
  (fn [lacinia-function refactored-object-tuples]
    (into {} (map #(lacinia-function % object-set typemappingfunc)
                  refactored-object-tuples))))

(defn create-lacinia-mutation [[spec-key spec-object] object-set mappingfunc id-field]
  (let [type-name (name spec-key)
        opts {:refs (:ref spec-object)
              :fields-info (get-in spec-object [:other :fields])
              :object-set object-set
              :typemappingfunc mappingfunc
              :wrapfunc identity }
        add-name (str "add" type-name)
        change-name (str "change" type-name)
        req-fields (get-in spec-object [:object :req])
        opt-fields (get-in spec-object [:object :opt])
        all-fields (apply conj req-fields opt-fields)]
    {(keyword add-name)
     {:type (keyword type-name) :resolve (keyword "mutation" add-name)
      :args (merge (create-query-args (remove #(= id-field %) req-fields) (assoc opts
                                                        :wrapfunc not-null-wrapper))
                   (create-query-args opt-fields opts))}
     (keyword change-name)
     {:type (keyword type-name) :resolve (keyword "mutation" change-name)
      :args (create-query-args all-fields opts)}}))


(defn convert-to-graphql [refactored-object-tuples typemappingfunc id-field]
  (let [object-set (set (map first refactored-object-tuples))
        lacinia-map-function (to-lacinia-map object-set typemappingfunc)
        lacinia-objects (lacinia-map-function
                         create-lacinia-object refactored-object-tuples)
        lacinia-queries (lacinia-map-function
                         create-lacinia-query refactored-object-tuples)
        lacinia-mutations (reduce merge {} (map #(create-lacinia-mutation
                                          % object-set typemappingfunc id-field)
                                        refactored-object-tuples))]
    {:objects lacinia-objects
     :queries lacinia-queries
     :mutations lacinia-mutations}))


