(ns journal.database-test
  (:require [journal.database :as sut])
  (:use [clojure.test]))

(deftest transaction-creation
  (testing "Check transaction data is correctly created"
    (is (= (sut/toTxData "Person" {:email "dean@p14n.com"})
           { :Person/email "dean@p14n.com" }))))

