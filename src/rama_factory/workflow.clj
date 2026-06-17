(ns rama-factory.workflow
  (:require [clojure.string :as str]
            [rama-factory.artifacts :as artifacts]
            [rama-factory.handoff :as handoff]
            [rama-factory.io :as fio]
            [rama-factory.model :as model]))

(defn run-path
  [factory run-id]
  (fio/file (get-in factory [:workflow :run-dir]) run-id))

(defn phase-path
  [factory run-id phase]
  (fio/file (run-path factory run-id)
            (format "%02d-%s"
                    (inc (or (:index phase) 0))
                    (-> (:id phase) name (str/replace "phase-" "phase-")))))

(defn indexed-phases
  [factory]
  (map-indexed (fn [idx phase] (assoc phase :index idx))
               (model/phase-sequence factory)))

(defn artifact-path
  [factory run-id phase]
  (fio/file (phase-path factory run-id phase) (:artifact phase)))

(defn run-manifest
  [factory challenge run-id]
  {:run/id run-id
   :factory/name (:factory/name factory)
   :challenge/id (:challenge/id challenge)
   :challenge/title (:challenge/title challenge)
   :phases (mapv #(select-keys % [:id :label :owner :artifact :gate])
                 (indexed-phases factory))})

(defn write-phase-artifact!
  [factory challenge run-id phase]
  (let [path (artifact-path factory run-id phase)]
    (fio/write-text! path (artifacts/artifact-content factory challenge phase))
    path))

(defn handoff-payload
  [run-id phase artifact-path]
  {:run-id run-id
   :phase (:id phase)
   :artifact (.getPath (clojure.java.io/file artifact-path))
   :gate (:gate phase)})

(defn simulate-run!
  [factory challenge run-id]
  (let [validation (model/validate factory challenge)
        roles (keys (:roles factory))
        state-dir (get-in factory [:workflow :state-dir])]
    (when-not (model/valid? validation)
      (throw (ex-info "Cannot simulate invalid factory/challenge."
                      {:validation validation})))
    (handoff/ensure-role-dirs! state-dir roles)
    (fio/write-edn! (fio/file (run-path factory run-id) "run.edn")
                    (run-manifest factory challenge run-id))
    (let [phases (vec (indexed-phases factory))
          artifacts (mapv (fn [phase]
                            {:phase (:id phase)
                             :owner (:owner phase)
                             :path (write-phase-artifact! factory challenge run-id phase)})
                          phases)
          handoffs (mapv (fn [[from-phase to-phase]]
                            (handoff/deliver!
                             state-dir
                             {:from (:owner from-phase)
                              :to (:owner to-phase)
                              :priority 50
                              :type :artifact-handoff
                              :task (str (name (:challenge/id challenge)) "/"
                                         (name (:id to-phase)))
                              :payload (handoff-payload
                                        run-id
                                        to-phase
                                        (artifact-path factory run-id to-phase))}))
                          (partition 2 1 phases))]
      {:run-id run-id
       :run-path (.getPath (clojure.java.io/file (run-path factory run-id)))
       :artifacts artifacts
       :handoffs handoffs
       :queues (handoff/queue-summary state-dir roles)})))
