(ns rama-factory.docs-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rama-factory.docs.content :as content]
            [rama-factory.docs.main :as docs]
            [rama-factory.web :as web]))

(deftest docs-content-has-core-framework-pages
  (let [ids (set (map :id content/pages))]
    (is (every? ids [:vision :architecture :devenv :swarm :zodiac :frontends :extensions :auth :factory-dashboard :roadmap]))))

(deftest docs-app-serves-hypermedia-and-json
  (let [system (web/start! (docs/config)
                           {:start-server? false
                            :reload-per-request? false})
        handler (:zodiac.core/app system)]
    (try
      (testing "home page renders through the framework web stack"
        (let [response (handler {:request-method :get
                                 :uri "/"})]
          (is (= 200 (:status response)))
          (is (str/includes? (:body response)
                             "Laravel-style DX for Rama applications"))
          (is (str/includes? (:body response)
                             "Dogfooded docs app"))))
      (testing "docs pages are available as JSON for SPA clients"
        (let [response (handler {:request-method :get
                                 :uri "/api/docs/zodiac"})]
          (is (= 200 (:status response)))
          (is (str/includes? (:body response) "Zodiac Runtime"))))
      (finally
        (web/stop! system)))))
