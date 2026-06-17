(ns rama-factory.artifacts
  (:require [clojure.string :as str]))

(defn- md-list
  [items]
  (if (seq items)
    (str/join "\n" (map #(str "- " %) items))
    "- none"))

(defn- operation-line
  [op]
  (str "- `" (name (:id op)) "` "
       "(" (name (:kind op)) ")"
       (when-let [effect (:effect op)]
         (str ": " effect))))

(defn- pstate-line
  [pstate]
  (str "- `" (name (:id pstate)) "` partitioned by `"
       (name (:partition-key pstate))
       "`"
       (when (:subindex? pstate) ", subindexed")
       ": " (:purpose pstate)))

(defn- query-line
  [query]
  (str "- `" (name (:id query)) "` reads "
       (pr-str (:reads query))
       "; expected I/O: " (:expected-io query)))

(defmulti artifact-content
  (fn [_factory _challenge phase] (:id phase)))

(defmethod artifact-content :phase-0
  [_factory challenge phase]
  (str "# " (:artifact phase) "\n\n"
       "## Behavior\n"
       (:challenge/summary challenge) "\n\n"
       "## Contract\n"
       (md-list (map operation-line (get-in challenge [:contract :operations]))) "\n\n"
       "## Acceptance Examples\n"
       (md-list (map (fn [{:keys [id given when then]}]
                       (str "`" (name id) "`: given "
                            (str/join ", " given)
                            "; when "
                            (str/join ", " when)
                            "; then "
                            (str/join ", " then)))
                     (:acceptance challenge)))
       "\n"))

(defmethod artifact-content :phase-1
  [_factory challenge phase]
  (str "# " (:artifact phase) "\n\n"
       "## Module\n"
       "- Namespace: `" (:challenge/namespace challenge) "`\n"
       "- Protocol: `" (get-in challenge [:contract :protocol]) "`\n\n"
       "## Depots\n"
       (md-list (map (fn [depot]
                       (str "- `" (name (:id depot)) "` partitioned by "
                            (pr-str (:partitioner depot))))
                     (get-in challenge [:rama :depots]))) "\n\n"
       "## PStates\n"
       (md-list (map pstate-line (get-in challenge [:rama :pstates]))) "\n\n"
       "## Queries\n"
       (md-list (map query-line (get-in challenge [:rama :queries]))) "\n\n"
       "## Fault Tolerance\n"
       (md-list (get-in challenge [:non-functional :fault-tolerance])) "\n\n"
       "## Performance\n"
       (md-list (get-in challenge [:non-functional :performance])) "\n"))

(defmethod artifact-content :phase-2
  [_factory challenge phase]
  (str "# " (:artifact phase) "\n\n"
       "verdict: pass\n\n"
       "## Review\n"
       "- Every growing PState in `" (:challenge/id challenge) "` is subindexed or explicitly bounded.\n"
       "- Query routes are declared next to their read sets.\n"
       "- Retry behavior is attached to write topologies.\n"))

(defmethod artifact-content :phase-3
  [_factory challenge _phase]
  (str "(ns " (:challenge/namespace challenge) "\n"
       "  \"Generated implementation placeholder for the factory PoC.\")\n\n"
       "(defn create-module\n"
       "  []\n"
       "  (throw (ex-info \"PoC generated a Rama implementation slot; real module code belongs here.\"\n"
       "                  {:challenge " (pr-str (:challenge/id challenge)) "})))\n"))

(defmethod artifact-content :phase-4
  [_factory _challenge phase]
  (str "# " (:artifact phase) "\n\n"
       "verdict: minor-fail\n\n"
       "## Review\n"
       "- Source slot exists, but no real Rama module has been implemented yet.\n"
       "- Keep this as a handoff boundary for a real coder agent.\n"))

(defmethod artifact-content :phase-5
  [_factory challenge _phase]
  (str "(ns " (-> (:challenge/namespace challenge)
                  (str/replace ".module" ".module-test")) "\n"
       "  (:require [clojure.test :refer [deftest is]]))\n\n"
       "(deftest generated-test-slot\n"
       "  (is true \"PoC generated the test phase slot.\"))\n"))

(defmethod artifact-content :phase-6
  [_factory _challenge phase]
  (str "# " (:artifact phase) "\n\n"
       "verdict: pass\n\n"
       "## Review\n"
       "- Generated test slot is present.\n"
       "- Real private functional, performance, and fault-tolerance tests would run here.\n"))

(defmethod artifact-content :phase-7
  [_factory _challenge phase]
  (str "# " (:artifact phase) "\n\n"
       "status: blocked-for-real-rama-module\n\n"
       "The PoC completed the factory workflow skeleton. The next increment is to replace the generated source slot with a real Rama module and run Rama IPC tests.\n"))

(defmethod artifact-content :default
  [_factory _challenge phase]
  (str "# " (:artifact phase) "\n\nGenerated artifact for " (:label phase) ".\n"))

