(ns testing)

(def not-integration #(-> % (meta) (:integration) (not)))
(def skip-coverage #(-> % (meta) (:skip-coverage) (not)))

