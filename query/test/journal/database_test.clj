(ns journal.database-test
  (:require [journal.database :as sut])
  (:use [clojure.test]))

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
                          {:selections {:Group/name nil}}})))))
