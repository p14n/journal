(ns journal.database-test
  (:require [journal.database :as sut])
  (:use [clojure.test]))

;;(deftest replacing-ids)
(deftest change-keys-to-name-only
  (testing "All keys should have namespaces reomved"
    (is (= { :x 1 :y { :z 2} }
           (sut/keyname-only { :blah/x 1 :blah/y { :blah/z 2} })))))

(deftest transaction-creation
  (testing "Check transaction data is correctly created"
    (is (= { :Person/email "dean@p14n.com" }
           (sut/to-tx-data "Person" {:email "dean@p14n.com"})))))

(deftest query-creation
  (testing "Check selection tree coverts to pull query"
    (is (= [[:Person/email :as :email]
            [:Person/firstname :as :firstname]
            {:Person/groups [[:Group/name :as :name]]}]
           (sut/to-query {:Person/email nil,
                          :Person/firstname nil,
                          :Person/groups
                          {:selections {:Group/name nil}}} #(do (nil? %)))))))
