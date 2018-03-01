(set-env!
  :project 'journal-command
  :version "0.0.1-SNAPSHOT"
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[;; App
                  [org.clojure/clojure    "1.9.0-RC1"]
                  [org.clojure/core.async "0.4.474"]
                  [org.onyxplatform/onyx "0.12.7"]

                  ;;Dev
                  [org.clojure/tools.namespace "0.2.11"     :scope "provided"]
                  [org.clojure/tools.nrepl "0.2.12"         :scope "provided"]
                  [boot/core              "2.7.2"           :scope "provided"]
                  ])
