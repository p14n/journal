(ns p14n.spectool
  (:require [clojure.spec.alpha :as spec]
            [spec-tools.visitor :as visitor]
            [p14n.spec :as ps]))

(defn apply-to-vals [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

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

(defn convert-and-refactor [spec-key]
  (->> spec-key
       (convert-spec-object)
       (refactor-spec-output spec-key)))

(defn move-refs-to-top [refactored]
  (let [objects-and-queries (vals refactored)
        refs (apply merge (map :ref (flatten objects-and-queries)))
        remove-refs (fn [v] (map #(dissoc % :ref) v))
        refs-removed (apply-to-vals refactored remove-refs)]
    (merge refs-removed {:refs refs})))

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

(defn create-field [refs field object-set wrapfunc]
  {field {:type (graphql-type refs field object-set wrapfunc)}})

(defn not-null-wrapper [x]
  `(~(symbol "not-null") ~x))

(defn create-lacinia-object [refs spec-object object-set]
  { (:def spec-object)
   {:description ""
    :fields (apply merge
                   (apply conj
                          (map #(create-field refs % object-set not-null-wrapper) (:req spec-object))
                          (map #(create-field refs % object-set identity) (:opt spec-object))))}})

(defn merge-maps-in-coll-value [m k]
  (let [mval (m k)
        merged (apply merge mval)]
    (merge m {k merged})))

(defn convert-to-graphql [app-schema]
  (let [refactored (apply-to-vals app-schema #(map convert-and-refactor %))
        refs-moved (move-refs-to-top refactored)
        object-set (set (map #(:def %) (:objects refs-moved)))
        new-objects (apply merge (map #(create-lacinia-object (:refs refs-moved) % object-set)
                                       (:objects refs-moved)))]
    (merge refs-moved {:objects new-objects})))
