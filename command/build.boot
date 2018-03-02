(set-env!
  :project 'journal-command
  :version "0.0.1-SNAPSHOT"
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[;; App
                  [org.clojure/clojure    "1.9.0-RC1"]
                  [org.clojure/core.async "0.4.474"]
                  [org.onyxplatform/onyx "0.12.7"]
                  [mount "0.1.10"]

                  ;;Dev
                  [org.clojure/tools.namespace "0.2.11"     :scope "provided"]
                  [org.clojure/tools.nrepl "0.2.12"         :scope "provided"]
                  [boot/core              "2.7.2"           :scope "provided"]
                  ])

(require '[clojure.tools.namespace.repl :refer [set-refresh-dirs refresh] :as tn]
         '[mount.core :refer [start stop]])

(deftask dev []
  (set-env! :source-paths #(conj % "src"))

  (apply tn/set-refresh-dirs (get-env :directories))
  (load-data-readers!)

  (require 'command.system)
  (in-ns 'command.system))

(deftask reset []
  (stop)
  (tn/refresh :after 'mount.core/start))
