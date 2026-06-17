(ns rama-factory.cli
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [rama-factory.generator :as generator]
            [rama-factory.handoff :as handoff]
            [rama-factory.io :as fio]
            [rama-factory.model :as model]
            [rama-factory.persona :as persona]
            [rama-factory.swarm :as swarm]
            [rama-factory.workflow :as workflow]))

(def default-factory "factory/factory.edn")
(def default-challenge "factory/challenges/bank-transfer.edn")
(def default-personas "factory/personas.edn")
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

(defn usage
  []
  (println (str/join
            "\n"
            ["Usage:"
             "  clojure -M:factory new <app-name> [target-dir] [--force]"
             "  clojure -M:factory make:extension <auth|factory-dashboard>"
             "  clojure -M:factory add <auth|factory-dashboard> --from <seed-path> [--target <app-dir>] [--force]"
             "  clojure -M:factory validate"
             "  clojure -M:factory simulate [run-id]"
             "  clojure -M:factory swarm-config"
             "  clojure -M:factory swarm-plan"
             "  clojure -M:factory personas"
             "  clojure -M:factory persona <persona-id>"
             "  clojure -M:factory queue"
             "  clojure -M:factory accept <role>"
             "  clojure -M:factory complete <role> <handoff-id>"])))

(defn -main
  [& args]
  (case (first args)
    "new" (new-command (rest args))
    "make:extension" (make-extension-command (rest args))
    "add" (add-command (rest args))
    "validate" (validate-command)
    "simulate" (simulate-command (or (second args) "demo-bank-transfer"))
    "swarm-config" (swarm-config-command)
    "swarm-plan" (swarm-plan-command)
    "personas" (personas-command)
    "persona" (if-let [id (second args)]
                (persona-command id)
                (usage))
    "queue" (queue-command)
    "accept" (if-let [role (second args)]
               (accept-command role)
               (usage))
    "complete" (if (and (second args) (nth args 2 nil))
                 (complete-command (second args) (nth args 2))
                 (usage))
    (usage)))
