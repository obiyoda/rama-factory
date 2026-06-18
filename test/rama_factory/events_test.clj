(ns rama-factory.events-test
  (:require [clojure.test :refer [deftest is testing]]
            [rama-factory.events :as events]
            [rama-factory.io :as fio]))

(defn temp-dir
  []
  (.toFile (java.nio.file.Files/createTempDirectory "rama-factory-events-test"
                                                     (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest event-log-is-append-only-by-stable-id
  (let [root (.getPath (temp-dir))]
    (try
      (let [first-event (events/append!
                         root
                         {:event-id "evt-1"
                          :event-type :handoff-created
                          :project-id "factory-floor"
                          :run-id "run-a"
                          :work-id "work-a"
                          :role "coder"
                          :persona-id "snips"
                          :persona-name "Snips"
                          :phase "implementation"
                          :status "queued"
                          :message "Queued work"
                          :occurred-at 1})
            duplicate (events/append!
                       root
                       {:event-id "evt-1"
                        :event-type :handoff-created
                        :run-id "run-a"
                        :work-id "work-a"
                        :role "coder"
                        :persona-id "snips"
                        :persona-name "Snips"
                        :phase "implementation"
                        :status "changed"
                        :message "Changed"
                        :occurred-at 2})]
        (testing "the existing event wins for stable event ids"
          (is (= "queued" (:status duplicate)))
          (is (= "factory-floor" (:project-id duplicate)))
          (is (= (:file first-event) (:file duplicate))))
        (testing "events are listed in occurrence order"
          (events/append! root {:event-id "evt-2"
                                :event-type :validation-passed
                                :run-id "run-a"
                                :work-id "work-a"
                                :occurred-at 2})
          (is (= ["evt-1" "evt-2"]
                 (mapv :event-id (events/list-events root))))))
      (finally
        (fio/delete-tree! root)))))
