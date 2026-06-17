(ns {{app_ns}}.factory-dashboard.routes-test
  (:require [clojure.test :refer [deftest is]]
            [{{app_ns}}.factory-dashboard.routes :as routes]))

(deftest dashboard-page-renders-demo-snapshot
  (let [response (routes/dashboard-page {})]
    (is (= 200 (:status response)))
    (is (re-find #"Factory Floor" (:body response)))
    (is (re-find #"validation-passed" (:body response)))))

(deftest api-snapshot-renders-edn
  (let [response (routes/api-snapshot {})]
    (is (= 200 (:status response)))
    (is (re-find #":validation-passed" (:body response)))))
