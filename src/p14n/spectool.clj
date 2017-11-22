(ns p14n.spectool
  (:require [clojure.spec.alpha :as spec]
		  	    [spec-tools.visitor :as visitor]))

(defn apply-to-vals[m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn convert-spec-object[so]
  (let [specs (atom {})]
    (visitor/visit
     so
     (fn [_ spec _ _]
       (if-let [s (spec/get-spec spec)]
         (swap! specs assoc spec (spec/form s))
         @specs)))))

(defn convert-to-graphql[app-schema]
  (into {} (apply-to-vals app-schema #(apply-to-vals % convert-spec-object))))

