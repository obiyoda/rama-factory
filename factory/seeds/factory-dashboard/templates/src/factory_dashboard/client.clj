(ns {{app_ns}}.factory-dashboard.client
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [{{app_ns}}.factory-dashboard.module :as module]))

(defn module-name
  []
  (get-module-name module/FactoryDashboardModule))

(defn clients
  [cluster-manager]
  (let [module-name (module-name)]
    {:factory-events-depot (foreign-depot cluster-manager module-name "*factory-events-depot")
     :runs (foreign-pstate cluster-manager module-name "$$runs")
     :events-by-run (foreign-pstate cluster-manager module-name "$$events-by-run")
     :handoffs-by-role (foreign-pstate cluster-manager module-name "$$handoffs-by-role")
     :artifacts-by-run (foreign-pstate cluster-manager module-name "$$artifacts-by-run")
     :event-counts (foreign-pstate cluster-manager module-name "$$event-counts")}))

(defn append-event!
  [clients event]
  (let [event (module/normalize-event event)
        {acked "factory-dashboard"} (foreign-append! (:factory-events-depot clients) event)]
    acked))

(defn run
  [clients run-id]
  (foreign-select-one (keypath run-id) (:runs clients)))

(defn timeline
  [clients run-id]
  (vec (foreign-select [(keypath run-id) ALL] (:events-by-run clients))))

(defn handoffs
  [clients role]
  (vec (foreign-select [(keypath role) ALL] (:handoffs-by-role clients))))

(defn artifacts
  [clients run-id]
  (vec (foreign-select [(keypath run-id) ALL] (:artifacts-by-run clients))))

(defn event-count
  [clients event-type]
  (or (foreign-select-one (keypath (name event-type)) (:event-counts clients)) 0))

(defn snapshot
  [clients run-id]
  {:run (run clients run-id)
   :timeline (timeline clients run-id)
   :artifacts (artifacts clients run-id)
   :counts {:run-created (event-count clients :run-created)
            :handoff-created (event-count clients :handoff-created)
            :handoff-accepted (event-count clients :handoff-accepted)
            :artifact-written (event-count clients :artifact-written)
            :validation-passed (event-count clients :validation-passed)
            :validation-failed (event-count clients :validation-failed)}})

(def demo-events
  [{:event-id "evt-001"
    :event-type :run-created
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "specifier"
    :phase "implicit-spec"
    :status "created"
    :message "Add auth to the starter app."
    :occurred-at 1}
   {:event-id "evt-002"
    :event-type :handoff-created
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "architect"
    :phase "rama-plan"
    :status "queued"
    :message "Design depots, PStates, queries, and idempotency."
    :occurred-at 2}
   {:event-id "evt-003"
    :event-type :handoff-accepted
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "coder"
    :phase "implementation"
    :status "in-process"
    :message "Implement the copied auth seed."
    :occurred-at 3}
   {:event-id "evt-004"
    :event-type :artifact-written
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "coder"
    :phase "implementation"
    :artifact "src/app/auth/module.clj"
    :status "written"
    :message "Auth Rama module written."
    :occurred-at 4}
   {:event-id "evt-005"
    :event-type :validation-passed
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "refactorer"
    :phase "test-validation"
    :artifact "test/app/auth/module_test.clj"
    :status "passed"
    :message "InProcessCluster tests passed."
    :occurred-at 5}])
