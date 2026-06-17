(ns rama-factory.handoff-test
  (:require [clojure.test :refer [deftest is testing]]
            [rama-factory.handoff :as handoff]
            [rama-factory.io :as fio]))

(defn temp-dir
  []
  (.toFile (java.nio.file.Files/createTempDirectory "rama-factory-handoff-test"
                                                     (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest handoff-lifecycle
  (let [root (.getPath (temp-dir))]
    (try
      (handoff/ensure-role-dirs! root [:specifier :coder])
      (let [delivered (handoff/deliver!
                       root
                       {:from :specifier
                        :to :coder
                        :priority 10
                        :type :artifact-handoff
                        :task "bank-transfer/phase-1"
                        :payload {:artifact "PLAN.md"}})]
        (testing "delivery writes recipient queue and sender audit"
          (is (= 1 (count (handoff/queue root :coder :new))))
          (is (= 1 (count (handoff/queue root :specifier :sent)))))
        (testing "accept moves the oldest task to in-process"
          (let [accepted (handoff/accept-next! root :coder)]
            (is (= (:id delivered) (:id accepted)))
            (is (= 0 (count (handoff/queue root :coder :new))))
            (is (= 1 (count (handoff/queue root :coder :in-process)))))
          (testing "complete moves in-process work to completed"
            (let [completed (handoff/complete! root :coder (:id delivered) {:status :done})]
              (is (= :done (get-in completed [:result :status])))
              (is (= 0 (count (handoff/queue root :coder :in-process))))
              (is (= 1 (count (handoff/queue root :coder :completed))))))))
      (finally
        (fio/delete-tree! root)))))

