(ns journal.query-test
  (:require [journal.query :as sut]
            [app.main :as m]
            [mount.core :as mount]
            [clojure.test :as t :refer [is are deftest testing use-fixtures]]))

(defn app-fixture [f]
  (mount/start)
  (f)
  (mount/stop))

(use-fixtures :once app-fixture)

(deftest ^:integration mutate-and-query
  (testing "Add a person"
    (is (= 1 2))))

