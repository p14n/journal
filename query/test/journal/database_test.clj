(ns journal.database-test
  (:require [journal.database :as sut])
  (:use [clojure.test]))

;;(deftest replacing-ids)
(deftest change-keys-to-name-only
  (testing "All keys should have namespaces removed"
    (is (= { :x 1 :y { :z 2} }
           (sut/keyname-only { :blah/x 1 :blah/y { :blah/z 2} })))))

(deftest transaction-creation
  (testing "Check transaction data is correctly created"
    (is (= { :Person/email "dean@p14n.com" }
           (sut/to-tx-data "Person" {:email "dean@p14n.com"})))))

(def sample-selection-tree
  {:Person/email nil,
   :Person/firstname nil,
   :Person/groups
   {:selections {:Group/name nil}}})

(deftest query-creation-from-selection-tree
  (testing "Check selection tree coverts to pull query"
    (is (= [[:Person/email :as :email]
            [:Person/firstname :as :firstname]
            {[:Person/groups :as :groups] [[:Group/name :as :name]]}]
           (sut/to-query sample-selection-tree #(do (nil? %)))))))

(deftest replacing-id
  (testing "Check that the :db/id is replaced with :ID in results"
    (is (= [{:name "a" :ID 1 :body { :type "b" :ID 2}}]
           (sut/replace-id-in-results
            [{:name "a" :db/id 1 :body { :type "b" :db/id 2}}]
            #(= % :db/id))))))

(deftest creating-where-clause
  (testing "The args get converted into a datalog where clause"
    (is (= '([?e :Person/email "dean"] [?e 1])
           (sut/to-where "Person" { :email "dean" :ID "1"} (symbol "?e") #(= % :ID))))))

(deftest creates-query-and-lookup
  (testing "Creates a query and where clause from selection tree and args"
    (is (= {:pattern '[:find
                       (pull
                        ?e
                        [[:Person/email :as :email]
                         [:Person/firstname :as :firstname]
                         {[:Person/groups :as :groups] [[:Group/name :as :name]]}])
                       :where
                       [?e :Person/email "wut"]] :lookup nil}
           (sut/query-from-selection "Person" "email" sample-selection-tree
                                     {:email "wut"} #(= % "ID")))))
  (testing "Creates a query and default where clause from selection tree and no args"
    (is (= {:pattern '[:find
                       (pull
                        ?e
                        [[:Person/email :as :email]
                         [:Person/firstname :as :firstname]
                         {[:Person/groups :as :groups] [[:Group/name :as :name]]}])
                       :where
                       [?e :Person/email]] :lookup nil}
           (sut/query-from-selection "Person" "email" sample-selection-tree
                                     {} #(= % "ID")))))
  (testing "Creates a lookup based on ID param"
    (is (= {:lookup 1,
            :pattern [[:Person/email :as :email]
                      [:Person/firstname :as :firstname]
                      {[:Person/groups :as :groups] [[:Group/name :as :name]]}]}
           (sut/query-from-selection "Person" "email" sample-selection-tree
                                     {"ID" "1"} #(= % "ID"))))))

