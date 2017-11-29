(set-env!
  :project 'journal
  :version "0.0.1-SNAPSHOT"
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[;; App
                  [org.clojure/clojure    "1.9.0-RC1"]
                  [com.datomic/datomic-free "0.9.5407"]
                  [compojure "1.5.1"]
                  [http-kit "2.2.0"] 
                  [com.walmartlabs/lacinia "0.18.0" :exclusions [clojure-future-spec]]
                  [mount "0.1.10"]
                  [org.clojure/data.json "0.2.6"]
                  [ring/ring-json "0.4.0"]

                  ;;Dev
                  [metosin/spec-tools "0.5.1"               :scope "provided"]
                  [org.clojure/tools.namespace "0.2.11"     :scope "provided"]
                  [org.clojure/tools.nrepl "0.2.12"         :scope "provided"]
                  [boot/core              "2.7.2"           :scope "provided"]
                  [adzerk/boot-test "1.0.7"                 :scope "test"]
                  [tolitius/boot-check    "0.1.2"           :scope "test"]])

(require '[tolitius.boot-check :as check]
         '[adzerk.boot-test :as bt]
         '[clojure.tools.namespace.repl :refer [set-refresh-dirs refresh] :as tn]
         '[mount.core :refer [start stop]])

(deftask watch-test
  "Runs tests"
  []
  (set-env! :source-paths #{"src" "test"}
            :resource-paths #{"resources" "test-resources"})
  (comp (watch) (bt/test)))

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)
    (check/with-eastwood)
    (check/with-kibit)
    (check/with-bikeshed)))

(deftask lein []
  (let [en (get-env)
     name (:project en)
     version (:version en)
     deps (:dependencies en)
     proj `(~'defproject ~name ~version :dependencies ~deps)]
    (spit "project.clj" proj)))

(deftask dev []
  (set-env! :source-paths #(conj % "src"))

  (apply tn/set-refresh-dirs (get-env :directories))
  (load-data-readers!)

  (require 'app.main)
  (in-ns 'app.main))

(deftask reset []
   (stop)
  (tn/refresh :after 'mount.core/start))

(deftask write-schema []
  (require 'app.spec)
  (let [write-function (resolve 'app.spec/write-app-schema)]
    (write-function)))
