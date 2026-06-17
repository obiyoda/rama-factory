(ns rama-factory.persona
  (:require [clojure.set :as set]
            [rama-factory.io :as fio]))

(def default-personas-path "factory/personas.edn")

(def known-seed-skills
  #{:auth-extension
    :factory-dashboard})

(def required-persona-keys
  #{:persona/id
    :persona/name
    :persona/default-role
    :persona/runtime
    :persona/skills})

(def supported-runtimes
  #{:codex :claude :local :external})

(defn load-personas
  ([]
   (load-personas default-personas-path))
  ([path]
   (:personas (fio/read-edn path))))

(defn personas-by-id
  [personas]
  (into {} (map (juxt :persona/id identity) personas)))

(defn persona
  [personas id]
  (get (personas-by-id personas) id))

(defn display-name
  [persona]
  (or (get-in persona [:persona/event-tags :display-name])
      (:persona/name persona)
      (some-> (:persona/id persona) name)))

(defn- problem
  [check message data]
  {:check check :message message :data data})

(defn validate-personas
  [factory personas]
  (let [role-ids (set (keys (:roles factory)))
        skill-ids (set/union #{:rama-factory-build} known-seed-skills)
        persona-ids (map :persona/id personas)]
    (vec
     (concat
      (when (empty? personas)
        [(problem :personas "Factory should define at least one named persona." {})])
      (when-let [duplicates (seq (->> persona-ids frequencies (keep (fn [[id n]] (when (> n 1) id))) set))]
        [(problem :persona-ids "Persona ids must be unique." {:duplicates duplicates})])
      (mapcat
       (fn [persona]
         (let [missing (set/difference required-persona-keys (set (keys persona)))
               role (:persona/default-role persona)
               runtime (:persona/runtime persona)
               skills (set (:persona/skills persona))
               missing-skills (set/difference skills skill-ids)]
           (cond-> []
             (seq missing)
             (conj (problem :persona-shape
                            "Persona is missing required keys."
                            {:persona (:persona/id persona)
                             :missing missing}))

             (and role (not (contains? role-ids role)))
             (conj (problem :persona-role
                            "Persona default role must reference a configured factory role."
                            {:persona (:persona/id persona)
                             :role role}))

             (and runtime (not (contains? supported-runtimes runtime)))
             (conj (problem :persona-runtime
                            "Persona runtime is not supported."
                            {:persona (:persona/id persona)
                             :runtime runtime
                             :supported supported-runtimes}))

             (seq missing-skills)
             (conj (problem :persona-skills
                            "Persona skills must reference known repo or seed skills."
                            {:persona (:persona/id persona)
                             :missing missing-skills})))))
       personas)))))

(defn summary
  [personas]
  (mapv (fn [p]
          {:id (:persona/id p)
           :name (display-name p)
           :role (:persona/default-role p)
           :runtime (:persona/runtime p)
           :skills (:persona/skills p)})
        personas))
