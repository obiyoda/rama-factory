(ns rama-factory.cli
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [rama-factory.generator :as generator]
            [rama-factory.handoff :as handoff]
            [rama-factory.io :as fio]
            [rama-factory.mcp :as mcp]
            [rama-factory.model :as model]
            [rama-factory.persona :as persona]
            [rama-factory.projects :as projects]
            [rama-factory.swarm :as swarm]
            [rama-factory.workflow :as workflow]))

(def default-factory "factory/factory.edn")
(def default-challenge "factory/challenges/bank-transfer.edn")
(def default-personas "factory/personas.edn")
(def default-projects projects/default-projects-path)
(def supported-seeds #{"auth" "factory-dashboard"})

(declare usage)

(defn load-inputs
  []
  {:factory (fio/read-edn default-factory)
   :challenge (fio/read-edn default-challenge)
   :personas (persona/load-personas default-personas)})

(defn print-validation
  [validation]
  (if (model/valid? validation)
    (println "valid")
    (do
      (println "invalid")
      (pprint/pprint validation))))

(defn validate-command
  []
  (let [{:keys [factory challenge personas]} (load-inputs)]
    (print-validation (model/validate factory challenge personas))))

(defn simulate-command
  [run-id]
  (let [{:keys [factory challenge]} (load-inputs)
        result (workflow/simulate-run! factory challenge run-id)]
    (println "simulated" (:run-id result))
    (println "run:" (:run-path result))
    (println "artifacts:")
    (doseq [{:keys [phase owner path]} (:artifacts result)]
      (println " -" (name phase) "->" (name owner) (.getPath (clojure.java.io/file path))))
    (println "handoffs:" (count (:handoffs result)))
    (println "queues:")
    (pprint/pprint (:queues result))))

(defn queue-command
  []
  (let [{:keys [factory]} (load-inputs)]
    (pprint/pprint
     (handoff/queue-summary
      (get-in factory [:workflow :state-dir])
      (keys (:roles factory))))))

(defn swarm-config-command
  []
  (let [{:keys [factory]} (load-inputs)]
    (print (swarm/render-config factory))))

(defn swarm-plan-command
  []
  (let [{:keys [factory]} (load-inputs)]
    (pprint/pprint (swarm/worktree-plan factory))))

(defn accept-command
  [role-name]
  (let [{:keys [factory]} (load-inputs)
        accepted (handoff/accept-next!
                  (get-in factory [:workflow :state-dir])
                  (keyword role-name))]
    (if accepted
      (pprint/pprint accepted)
      (println "NO_TASK"))))

(defn complete-command
  [role-name id]
  (let [{:keys [factory]} (load-inputs)]
    (pprint/pprint
     (handoff/complete!
      (get-in factory [:workflow :state-dir])
      (keyword role-name)
      id
      {:status :done}))))

(defn- option-value
  [args option-name]
  (some (fn [[k v]]
          (when (= k option-name) v))
        (partition 2 1 args)))

(defn- flag?
  [args flag-name]
  (boolean (some #{flag-name} args)))

(defn- option-values
  [args option-name]
  (->> (partition 2 1 args)
       (keep (fn [[k v]]
               (when (= k option-name) v)))
       vec))

(defn- kw-option
  [args option-name]
  (some-> (option-value args option-name) keyword))

(defn- seed-options
  [args]
  (mapv keyword (option-values args "--seed")))

(defn new-command
  [args]
  (let [positionals (remove #(str/starts-with? % "--") args)]
    (if-let [app-name (first positionals)]
    (let [target (or (second positionals) app-name)
          result (generator/create-app! app-name target {:force? (flag? args "--force")})]
      (println "created app" (get-in result [:app :app/name]))
      (println "root:" (:root result))
      (println "files:" (count (:written result))))
    (usage))))

(defn make-extension-command
  [args]
  (let [extension (first args)]
    (if (contains? supported-seeds extension)
      (let [seed-root (or (option-value args "--target")
                          (generator/factory-seed-root extension))
            seed (generator/validate-seed! seed-root)]
        (println extension "seed ready")
        (println "seed:" seed-root)
        (println "templates:" (count (:seed/templates seed))))
      (usage))))

(defn add-command
  [args]
  (let [extension (first args)
        seed-root (or (option-value args "--from")
                      (generator/factory-seed-root extension))
        target-root (or (option-value args "--target") ".")]
    (if (and (contains? supported-seeds extension) seed-root)
      (let [result (generator/install-extension! target-root
                                                 extension
                                                 seed-root
                                                 {:force? (flag? args "--force")})]
        (println "added" extension)
        (println "seed:" seed-root)
        (println "files:" (count (:written result)))
        (println "config:" (:config result)))
      (usage))))

(defn personas-command
  []
  (let [{:keys [personas]} (load-inputs)]
    (pprint/pprint (persona/summary personas))))

(defn persona-command
  [persona-id]
  (let [{:keys [personas]} (load-inputs)
        id (keyword persona-id)]
    (if-let [found (persona/persona personas id)]
      (pprint/pprint found)
      (println "NO_PERSONA"))))

(defn project-list-command
  []
  (pprint/pprint (projects/project-summary (projects/read-registry default-projects))))

(defn project-show-command
  [project-id]
  (let [registry (projects/read-registry default-projects)]
    (if-let [project (projects/project registry project-id)]
      (pprint/pprint project)
      (println "NO_PROJECT"))))

(defn project-validate-command
  []
  (let [validation (projects/validate-registry (projects/read-registry default-projects))]
    (if (projects/valid? validation)
      (println "valid")
      (do
        (println "invalid")
        (pprint/pprint validation)))))

(defn project-new-command
  [args]
  (if-let [project-id (first args)]
    (let [seed-ids (seed-options args)
          result (projects/create-project!
                  project-id
                  (cond-> {:projects-path default-projects
                           :type (or (kw-option args "--type") :seed-lab)
                           :workspace (option-value args "--workspace")
                           :repo (option-value args "--repo")
                           :force? (flag? args "--force")}
                    (seq seed-ids) (assoc :seeds seed-ids)))]
      (println "created project" (name (get-in result [:project :project/id])))
      (println "registry:" (:projects-path result))
      (println "workspace:" (get-in result [:project :project/workspace]))
      (println "repo:" (or (get-in result [:project :project/repo]) "")))
    (usage)))

(defn lab-new-command
  [args]
  (if-let [lab-id (first args)]
    (let [seed-ids (or (seq (seed-options args))
                       [(or (kw-option args "--seed") :factory-dashboard)])
          result (projects/create-lab!
                  lab-id
                  {:projects-path default-projects
                   :project-id (or (kw-option args "--project") (keyword lab-id))
                   :seed-ids seed-ids
                   :force? (flag? args "--force")
                   :commit? (not (flag? args "--no-commit"))})]
      (println "created lab" (name (get-in result [:lab :lab/id])))
      (println "project:" (name (get-in result [:project :project/id])))
      (println "workspace:" (get-in result [:project :project/workspace]))
      (println "seeds:" (str/join ", " (map name (get-in result [:project :project/seeds]))))
      (when-let [commit (:commit result)]
        (println "baseline commit:" (:commit commit)))
      (println "lab:" (:lab-path result)))
    (usage)))

(defn lab-validate-command
  [lab-id]
  (let [report (projects/validate-lab!
                lab-id
                {:projects-path default-projects})]
    (println (if (:valid? report) "valid" "invalid"))
    (println "lab:" (name (:lab/id report)))
    (println "project:" (name (:project/id report)))
    (println "workspace:" (:workspace report))
    (println "report:" (:report-path report))
    (doseq [{:keys [command exit]} (:commands report)]
      (println "-" command "=>" exit))))

(defn usage
  []
  (println (str/join
            "\n"
            ["Usage:"
             "  clojure -M:factory new <app-name> [target-dir] [--force]"
             "  clojure -M:factory make:extension <auth|factory-dashboard>"
             "  clojure -M:factory add <auth|factory-dashboard> --from <seed-path> [--target <app-dir>] [--force]"
             "  clojure -M:factory project-list"
             "  clojure -M:factory project-show <project-id>"
             "  clojure -M:factory project-new <project-id> [--type <seed-lab|example|business-app>] [--workspace <path>] [--repo <path>] [--seed <seed-id>] [--force]"
             "  clojure -M:factory project-validate"
             "  clojure -M:factory lab-new <lab-id> [--project <project-id>] [--seed <seed-id>] [--force] [--no-commit]"
             "  clojure -M:factory lab-validate <lab-id>"
             "  clojure -M:factory validate"
             "  clojure -M:factory simulate [run-id]"
             "  clojure -M:factory swarm-config"
             "  clojure -M:factory swarm-plan"
             "  clojure -M:factory personas"
             "  clojure -M:factory persona <persona-id>"
             "  clojure -M:factory mcp-tools"
             "  clojure -M:factory queue"
             "  clojure -M:factory accept <role>"
             "  clojure -M:factory complete <role> <handoff-id>"])))

(defn -main
  [& args]
  (try
    (case (first args)
      "new" (new-command (rest args))
      "make:extension" (make-extension-command (rest args))
      "add" (add-command (rest args))
      "project-list" (project-list-command)
      "project-show" (if-let [id (second args)]
                       (project-show-command id)
                       (usage))
      "project-new" (project-new-command (rest args))
      "project-validate" (project-validate-command)
      "lab-new" (lab-new-command (rest args))
      "lab-validate" (if-let [id (second args)]
                       (lab-validate-command id)
                       (usage))
      "validate" (validate-command)
      "simulate" (simulate-command (or (second args) "demo-bank-transfer"))
      "swarm-config" (swarm-config-command)
      "swarm-plan" (swarm-plan-command)
      "personas" (personas-command)
      "persona" (if-let [id (second args)]
                  (persona-command id)
                  (usage))
      "mcp-tools" (pprint/pprint mcp/tool-definitions)
      "queue" (queue-command)
      "accept" (if-let [role (second args)]
                 (accept-command role)
                 (usage))
      "complete" (if (and (second args) (nth args 2 nil))
                   (complete-command (second args) (nth args 2))
                   (usage))
      (usage))
    (finally
      (shutdown-agents))))
