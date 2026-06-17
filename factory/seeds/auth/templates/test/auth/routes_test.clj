(ns {{app_ns}}.auth.routes-test
  (:require [clojure.test :refer [deftest is]]
            [{{app_ns}}.auth.routes :as routes]))

(deftest login-page-renders
  (let [response (routes/login-page {})]
    (is (= 200 (:status response)))
    (is (re-find #"Login" (:body response)))
    (is (re-find #"email" (:body response)))))

(deftest current-user-api-requires-session
  (let [response (routes/api-current-user {:session {}})]
    (is (= 401 (:status response)))
    (is (re-find #":unauthenticated" (:body response)))))
