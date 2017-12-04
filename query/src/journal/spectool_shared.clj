(ns journal.spectool-shared
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

(defn convert-to-object-tuples [app-schema]
  (map convert-spec-object-tuple-to-data
       (:objects app-schema)))

