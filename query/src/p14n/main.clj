(ns p14n.main
  (:require [mount.core :refer [defstate start stop]]
            [p14n.http :refer [http]]))

(defn -main [& args]
  (start))
