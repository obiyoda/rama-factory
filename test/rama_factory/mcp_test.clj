(ns rama-factory.mcp-test
  (:require [clojure.test :refer [deftest is testing]]
            [rama-factory.io :as fio]
            [rama-factory.mcp :as mcp]))

(defn temp-dir
  []
  (.toFile (java.nio.file.Files/createTempDirectory "rama-factory-mcp-test"
                                                     (make-array java.nio.file.attribute.FileAttribute 0))))

(defn write-fixture!
  [root]
  (let [state-dir (.getPath (clojure.java.io/file root ".rama-factory"))
        factory-path (.getPath (clojure.java.io/file root "factory.edn"))
        personas-path (.getPath (clojure.java.io/file root "personas.edn"))
        projects-path (.getPath (clojure.java.io/file root "projects.edn"))]
    (fio/write-edn!
     factory-path
     {:factory/name "mcp-test"
      :roles {:specifier {:agent :codex}
              :coder {:agent :codex}}
      :workflow {:state-dir state-dir}})
    (fio/write-edn!
     personas-path
     {:personas [{:persona/id :snips
                  :persona/name "Snips"
                  :persona/default-role :coder
                  :persona/runtime :codex
                  :persona/skills [:rama-factory-build]
                  :persona/event-tags {:display-name "Snips"}}]})
    (fio/write-edn!
     projects-path
     {:projects [{:project/id :invoice-app
                  :project/name "Invoice App"
                  :project/type :seed-lab
                  :project/workspace ".rama-workspaces/invoice-app"
                  :project/ports {:web 3101 :assets 5174}
                  :project/seeds [:auth]}]})
    {:factory-path factory-path
     :personas-path personas-path
     :projects-path projects-path
     :challenge-path (.getPath (clojure.java.io/file root "missing-challenge.edn"))}))

(deftest initialize-and-list-tools
  (let [response (mcp/handle-message
                  {"jsonrpc" "2.0"
                   "id" 1
                   "method" "initialize"
                   "params" {"protocolVersion" mcp/protocol-version}})
        tools-response (mcp/handle-message
                        {"jsonrpc" "2.0"
                         "id" 2
                         "method" "tools/list"})]
    (is (= mcp/protocol-version
           (get-in response ["result" "protocolVersion"])))
    (is (= false
           (get-in response ["result" "capabilities" "tools" "listChanged"])))
    (is (contains? (set (map #(get % "name")
                             (get-in tools-response ["result" "tools"])))
                   "factory.claim_next_work"))
    (is (contains? (set (map #(get % "name")
                             (get-in tools-response ["result" "tools"])))
                   "factory.list_projects"))))

(deftest persona-and-skill-tools-return-structured-content
  (let [personas (mcp/call-tool "factory.list_personas" {})
        skill (mcp/call-tool "factory.get_skill" {"id" "rama-factory-build"})]
    (is (= false (get personas "isError")))
    (is (some #(= "Snips" (get % "name"))
              (get-in personas ["structuredContent" "personas"])))
    (is (= false (get skill "isError")))
    (is (re-find #"Rama Factory Build"
                 (get-in skill ["structuredContent" "content"])))))

(deftest project-tools-return-registered-projects
  (let [root (.getPath (temp-dir))
        opts (write-fixture! root)]
    (try
      (let [ctx (mcp/load-context opts)
            projects (mcp/call-tool ctx "factory.list_projects" {})
            project (mcp/call-tool ctx "factory.get_project" {"id" "invoice-app"})]
        (is (= false (get projects "isError")))
        (is (= "invoice-app"
               (get-in projects ["structuredContent" "projects" 0 "project/id"])))
        (is (= "Invoice App"
               (get-in project ["structuredContent" "project" "project/name"]))))
      (finally
        (fio/delete-tree! root)))))

(deftest mcp-work-lifecycle-uses-handoff-queue
  (let [root (.getPath (temp-dir))
        opts (write-fixture! root)]
    (try
      (let [ctx (mcp/load-context opts)
            created (mcp/call-tool ctx
                                   "factory.create_work"
                                   {"from" "specifier"
                                    "to" "coder"
                                    "task" "Implement auth seed"
                                    "priority" 5
                                    "persona_id" "snips"
                                    "project_id" "invoice-app"
                                    "payload" {"artifact" "module.clj"}})
            handoff-id (get-in created ["structuredContent" "handoff" "id"])]
        (testing "create writes a new handoff"
          (is (= false (get created "isError")))
          (is (string? handoff-id))
          (is (= "handoff-created"
                 (get-in created ["structuredContent" "event" "event-type"])))
          (is (= "invoice-app"
                 (get-in created ["structuredContent" "event" "project-id"]))))
        (testing "claim moves handoff to in-process and attributes the claimant"
          (let [claimed (mcp/call-tool ctx
                                       "factory.claim_next_work"
                                       {"role" "coder"
                                        "persona_id" "snips"})]
            (is (= handoff-id
                   (get-in claimed ["structuredContent" "work" "id"])))
            (is (= "snips"
                   (get-in claimed ["structuredContent" "work" "claimed-by" "persona/id"])))
            (is (= "handoff-accepted"
                   (get-in claimed ["structuredContent" "event" "event-type"])))))
        (testing "complete moves handoff to completed"
          (let [completed (mcp/call-tool ctx
                                         "factory.complete_work"
                                         {"role" "coder"
                                          "handoff_id" handoff-id
                                          "status" "done"
                                          "validation" "clojure -M:test"
                                          "persona_id" "snips"})]
            (is (= "done"
                   (get-in completed ["structuredContent" "work" "result" "status"])))
            (is (= "Snips"
                   (get-in completed ["structuredContent" "work" "result" "completed-by" "persona/name"])))
            (is (= ["work-completed" "validation-passed"]
                   (mapv #(get % "event-type")
                         (get-in completed ["structuredContent" "events"]))))))
        (testing "queue summary reports completed work"
          (let [summary (mcp/call-tool ctx "factory.queue_summary" {})]
            (is (= 1
                   (get-in summary ["structuredContent" "queues" "coder" "completed"]))))))
      (finally
        (fio/delete-tree! root)))))
