(ns {{app_ns}}.factory-dashboard.client
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [{{app_ns}}.factory-dashboard.module :as module]))

(defonce local-system (atom nil))
(defonce ingested-event-ids (atom #{}))

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

(defn- event-log-dir-candidates
  []
  (let [event-dir (System/getenv "RAMA_FACTORY_EVENT_DIR")
        state-dir (System/getenv "RAMA_FACTORY_STATE_DIR")]
    (->> [event-dir
          (when-not (str/blank? state-dir)
            (str state-dir "/events"))
          ".rama-factory/events"
          "../.rama-factory/events"]
         (remove str/blank?)
         distinct
         (map io/file)
         (filter #(.isDirectory %))
         vec)))

(defn- event-file?
  [file]
  (and (.isFile file)
       (str/ends-with? (.getName file) ".edn")))

(defn local-events
  []
  (->> (event-log-dir-candidates)
       (mapcat #(.listFiles %))
       (filter event-file?)
       (map #(edn/read-string (slurp %)))
       (map module/normalize-event)
       (sort-by (juxt :occurred-at :event-id))
       vec))

(defn latest-local-run-id
  []
  (some-> (last (local-events)) :run-id))

(defn ingest-local-events!
  [clients]
  (let [events (local-events)
        pending (remove #(contains? @ingested-event-ids (:event-id %)) events)]
    (doseq [event pending]
      (append-event! clients event)
      (swap! ingested-event-ids conj (:event-id event)))
    {:event-count (count events)
     :ingested (count pending)
     :run-id (some-> (last events) :run-id)}))

(defn local-clients
  []
  (or (:clients @local-system)
      (locking local-system
        (or (:clients @local-system)
            (when-let [create-ipc (requiring-resolve 'com.rpl.rama.test/create-ipc)]
              (let [launch-module! (requiring-resolve 'com.rpl.rama.test/launch-module!)
                    ipc (create-ipc)]
                (launch-module! ipc module/FactoryDashboardModule {:tasks 4 :threads 2})
                (let [clients (clients ipc)]
                  (reset! local-system {:ipc ipc :clients clients})
                  clients)))))))

(defn run
  [clients run-id]
  (foreign-select-one (keypath run-id) (:runs clients)))

(defn- by-occurrence
  [entries]
  (sort-by (fn [[event-id event]]
             [(:occurred-at event) event-id])
           entries))

(defn timeline
  [clients run-id]
  (vec (by-occurrence
        (foreign-select [(keypath run-id) ALL] (:events-by-run clients)))))

(defn handoffs
  [clients role]
  (vec (by-occurrence
        (foreign-select [(keypath role) ALL] (:handoffs-by-role clients)))))

(defn artifacts
  [clients run-id]
  (vec (by-occurrence
        (foreign-select [(keypath run-id) ALL] (:artifacts-by-run clients)))))

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
            :validation-failed (event-count clients :validation-failed)
            :work-completed (event-count clients :work-completed)}})

(def demo-events
  [{:event-id "evt-001"
    :event-type :run-created
    :project-id "factory-floor"
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "specifier"
    :persona-id "spec-sam"
    :persona-name "SpecSam"
    :phase "implicit-spec"
    :status "created"
    :message "Add auth to the starter app."
    :occurred-at 1}
   {:event-id "evt-002"
    :event-type :handoff-created
    :project-id "factory-floor"
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "architect"
    :persona-id "architect-alice"
    :persona-name "ArchitectAlice"
    :phase "rama-plan"
    :status "queued"
    :message "Design depots, PStates, queries, and idempotency."
    :occurred-at 2}
   {:event-id "evt-003"
    :event-type :handoff-accepted
    :project-id "factory-floor"
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "coder"
    :persona-id "snips"
    :persona-name "Snips"
    :phase "implementation"
    :status "in-process"
    :message "Implement the copied auth seed."
    :occurred-at 3}
   {:event-id "evt-004"
    :event-type :artifact-written
    :project-id "factory-floor"
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "coder"
    :persona-id "snips"
    :persona-name "Snips"
    :phase "implementation"
    :artifact "src/app/auth/module.clj"
    :status "written"
    :message "Auth Rama module written."
    :occurred-at 4}
   {:event-id "evt-005"
    :event-type :validation-passed
    :project-id "factory-floor"
    :run-id "demo-auth-run"
    :work-id "work-auth"
    :role "refactorer"
    :persona-id "claudedbob"
    :persona-name "ClaudedBob"
    :phase "test-validation"
    :artifact "test/app/auth/module_test.clj"
    :status "passed"
    :message "InProcessCluster tests passed."
    :occurred-at 5}])
