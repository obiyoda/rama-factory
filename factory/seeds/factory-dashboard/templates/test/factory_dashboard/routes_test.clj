(ns {{app_ns}}.factory-dashboard.routes-test
  (:require [clojure.test :refer [deftest is]]
            [{{app_ns}}.factory-dashboard.routes :as routes]))

(deftest dashboard-page-renders-factory-floor
  (let [response (routes/dashboard-page {:factory-dashboard/demo? true})]
    (is (= 200 (:status response)))
    (is (re-find #"Factory Floor" (:body response)))
    (is (re-find #"basecoat-css@0.3.11" (:body response)))
    (is (re-find #"class=\"card\"" (:body response)))
    (is (re-find #"class=\"table\"" (:body response)))))

(deftest api-snapshot-renders-edn
  (let [response (routes/api-snapshot {:factory-dashboard/demo? true})]
    (is (= 200 (:status response)))
    (is (re-find #":counts" (:body response)))))
