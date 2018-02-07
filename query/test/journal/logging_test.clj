(ns journal.logging-test
  (:use journal.logging
  		clojure.test)
  (:import journal.logging.TestLogger))


(def logs (atom []))
(def logger (TestLogger. logs))

(defn log-fixture [f]
  (reset! logs [])
  (f))

(use-fixtures :each log-fixture)

(deftest log-at-info
  (testing "Logging at info"
  	(info logger :namespace/component "hi")
    (is (= {:body {:desc "hi"}, :level :info, :service :namespace/component}
           (first @logs)))
  	(info logger :namespace/component {:thing "hi"})
    (is (= {:body {:thing "hi"}, :level :info, :service :namespace/component}
           (second @logs)))))

(deftest log-at-error
  (testing "Logging at error"
  	(error logger :namespace/component "hi")
    (is (= {:body {:desc "hi"}, :level :error, :service :namespace/component}
           (first @logs)))
  	(error logger :namespace/component {:thing "hi"})
    (is (= {:body {:thing "hi"}, :level :error, :service :namespace/component}
           (second @logs)))))

(deftest log-at-debug
  (testing "Logging at debug"
  	(debug logger :namespace/component "hi")
    (is (= {:body {:desc "hi"}, :level :debug, :service :namespace/component}
           (first @logs)))
  	(debug logger :namespace/component {:thing "hi"})
    (is (= {:body {:thing "hi"}, :level :debug, :service :namespace/component}
           (second @logs)))))