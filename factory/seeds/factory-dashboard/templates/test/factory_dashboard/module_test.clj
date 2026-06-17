(ns {{app_ns}}.factory-dashboard.module-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama.test :as rtest]
            [{{app_ns}}.factory-dashboard.client :as dashboard]
            [{{app_ns}}.factory-dashboard.module :as module]))

(deftest factory-dashboard-module-materializes-events
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/FactoryDashboardModule {:tasks 4 :threads 2})
    (let [clients (dashboard/clients ipc)]
      (doseq [event dashboard/demo-events]
        (is (= (:event-id event) (dashboard/append-event! clients event))))
      (let [snapshot (dashboard/snapshot clients "demo-auth-run")]
        (testing "run state is updated by the latest event"
          (is (= "demo-auth-run" (get-in snapshot [:run :run-id])))
          (is (= "passed" (get-in snapshot [:run :status]))))
        (testing "timeline and artifacts are materialized by run"
          (is (= 5 (count (:timeline snapshot))))
          (is (= 5 (count (:artifacts snapshot))))
          (is (= "Snips"
                 (:persona-name (second (first (filter #(= "evt-004" (first %))
                                                        (:timeline snapshot))))))))
        (testing "role handoffs are queryable independently"
          (is (= 2 (count (dashboard/handoffs clients "coder")))))
        (testing "event counts are maintained as fast projections"
          (is (= 1 (get-in snapshot [:counts :run-created])))
          (is (= 1 (get-in snapshot [:counts :handoff-created])))
          (is (= 1 (get-in snapshot [:counts :validation-passed]))))))))
