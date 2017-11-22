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

(defn convert-to-graphql [app-schema]
  (let [refactored (apply-to-vals app-schema #(map convert-and-refactor %))
        refs-moved (move-refs-to-top refactored)]
    refs-moved))
