(ns rama-factory.app-test
  (:require [clojure.test :refer [deftest is testing]]
            [rama-factory.app :as app]
            [rama-factory.docs.main :as docs]))

(deftest docs-app-config-is-valid
  (testing "the dogfooded docs app uses the default Zodiac runtime"
    (let [config (docs/config)]
      (is (app/valid? (app/validate config)))
      (is (= :zodiac (get-in (app/application config) [:web :runtime]))))))

(deftest app-validation-catches-bad-cookie-secret
  (let [config (assoc-in (docs/config) [:web :cookie-secret] "too-short")
        problems (app/validate config)]
    (is (some #(= :cookie-secret (:check %)) problems))))
