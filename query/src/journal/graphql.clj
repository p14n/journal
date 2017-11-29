(ns journal.graphql
  (:require  [clojure.java.io :as io]
             [clojure.edn :as edn]
             [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
             [com.walmartlabs.lacinia.schema :as schema]
             [com.walmartlabs.lacinia :refer [execute]]))

(defn read-edn-from-file [name]
  (with-open [in (java.io.PushbackReader.
                  (io/reader name))]
    (let [edn-seq (repeatedly (partial edn/read {:eof :theend} in))]
      (apply str (take-while (partial not= :theend) edn-seq)))))

(defn schema-from-file [file]
  (read-string
   (read-edn-from-file file)))

(defn graphql-schema-from-edn-file[file resolver-map]
  (-> (schema-from-file file)
      (attach-resolvers resolver-map)
      schema/compile))
