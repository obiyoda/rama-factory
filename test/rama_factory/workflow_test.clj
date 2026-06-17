(ns rama-factory.workflow-test
  (:require [clojure.test :refer [deftest is testing]]
            [rama-factory.handoff :as handoff]
            [rama-factory.io :as fio]
            [rama-factory.workflow :as workflow]))

(defn temp-dir
  [prefix]
  (.getPath
   (.toFile (java.nio.file.Files/createTempDirectory prefix
                                                     (make-array java.nio.file.attribute.FileAttribute 0)))))

(deftest simulate-run-writes-artifacts-and-handoffs
  (let [root (temp-dir "rama-factory-workflow-test")
        factory (-> (fio/read-edn "factory/factory.edn")
                    (assoc-in [:workflow :state-dir] (fio/file root ".rama-factory"))
                    (assoc-in [:workflow :run-dir] (fio/file root "runs")))
        challenge (fio/read-edn "factory/challenges/bank-transfer.edn")]
    (try
      (let [result (workflow/simulate-run! factory challenge "test-run")]
        (testing "one artifact is generated per phase"
          (is (= 8 (count (:artifacts result))))
          (is (every? #(.exists (clojure.java.io/file (:path %)))
                      (:artifacts result))))
        (testing "handoffs connect adjacent phases"
          (is (= 7 (count (:handoffs result))))
          (is (= 3 (count (handoff/queue (get-in factory [:workflow :state-dir])
                                         :coder
                                         :new)))))
        (testing "run manifest is written"
          (is (.exists (clojure.java.io/file (:run-path result) "run.edn")))))
      (finally
        (fio/delete-tree! root)))))
