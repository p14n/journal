(ns testing)

(def not-integration #(-> % (meta) (:integration) (not)))

