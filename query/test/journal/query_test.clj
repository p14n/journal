(ns journal.query-test
  (:require [journal.query :as sut]
            [app.main :as m]
            [mount.core :as mount]
            [clojure.test :as t :refer [is are deftest testing use-fixtures]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(defn app-fixture [f]
  (mount/start)
  (f)
  (mount/stop))

(use-fixtures :once app-fixture)

(defn perform-query [query]
  (let [reqbody (str (json/write-str {:query query :variables nil}))
        v (println reqbody)
        opts {:headers {"Content-Type" "application/json"
                        "Content-Length" (.length reqbody)}
              :body reqbody}
        {:keys [body] :as resp} @(http/post "http://localhost:8085/graphql" opts)
        x (println body)]
    (assoc resp :body (json/read-str body :key-fn keyword))))

(deftest ^:integration mutate-and-query
  (let [personid (atom nil)
        groupid (atom nil)]
    (testing "Add a person"
      (let [resp  (perform-query
                   "mutation {
                      addPerson(email: \"dean@p14n.com\", firstname:\"Dean\"){
                        ID
                      }
                    }")]
        (swap! personid (fn [c] (get-in resp [:body :data :addPerson :ID])))
        (is (= 200 (resp :status)))))
    (testing "Add a Group"
      (let [resp  (perform-query
                   "mutation {
                      addGroup(name: \"hullo\"){
                        ID
                      }
                    }")]
        (swap! groupid (fn [c] (get-in resp [:body :data :addGroup :ID])))
        (is (= 200 (resp :status)))))
    (testing "Add a Group to a Person"
      (let [resp  (perform-query (str
                                  "mutation {
                                     addGroupToPerson(group:"
                                      @groupid ",person:" @personid "){
                                      ID
                                     }
                                   }"))]
        (is (= 200 (resp :status)))))
    (testing "Query both"
      (let [resp  (perform-query
                   "query {
                      person {
                        ID
                        groups {
                          name
                        }
                      }
                    }")
            theperson (first (get-in resp [:body :data :person]))]
        (is (= 200 (resp :status)))
        (is (= @personid (theperson :ID)))
        (is (= "hullo" (:name (first (theperson :groups)))))))))
