(ns rama-factory.projects
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [rama-factory.events :as events]
            [rama-factory.generator :as generator]
            [rama-factory.io :as fio]))

(def default-projects-path "factory/projects.edn")
(def default-seed-labs-dir "factory/seed-labs")
(def default-workspaces-dir ".rama-workspaces")
(def default-state-dir ".rama-factory")

(def default-validation-commands
  ["devenv tasks run assets:build"
   "devenv shell -- clojure -M:test"])

(def project-types
  #{:seed-lab :example :business-app})

(defn- problem
  [check message data]
  {:check check :message message :data data})

(defn- present?
  [value]
  (and (some? value)
       (not (str/blank? (str value)))))

(defn ->id
  [value]
  (cond
    (keyword? value) value
    (string? value) (keyword (str/replace value #"^:" ""))
    :else (keyword (str value))))

(defn id-name
  [value]
  (name (->id value)))

(defn read-registry
  ([] (read-registry default-projects-path))
  ([path]
   (let [file (io/file path)]
     (if (.exists file)
       (fio/read-edn (.getPath file))
       {:projects []}))))

(defn write-registry!
  ([registry] (write-registry! default-projects-path registry))
  ([path registry]
   (fio/write-edn! path registry)))

(defn projects
  [registry]
  (vec (:projects registry)))

(defn project
  [registry project-id]
  (let [id (->id project-id)]
    (some #(when (= id (:project/id %)) %) (projects registry))))

(defn project-summary
  [registry]
  (->> (projects registry)
       (map #(select-keys % [:project/id
                             :project/name
                             :project/type
                             :project/status
                             :project/workspace
                             :project/repo
                             :project/seeds
                             :project/ports]))
       (sort-by (comp name :project/id))
       vec))

(defn- seed-exists?
  [seed-id]
  (.exists (io/file (generator/factory-seed-root seed-id))))

(defn- validate-project
  [project]
  (let [id (:project/id project)
        type (:project/type project)
        seeds (or (:project/seeds project) [])
        ports (:project/ports project)]
    (cond-> []
      (nil? id)
      (conj (problem :project-id
                     "Project must declare :project/id."
                     project))

      (and (some? id) (not (keyword? id)))
      (conj (problem :project-id
                     "Project id must be a keyword."
                     {:project/id id}))

      (not (contains? project-types type))
      (conj (problem :project-type
                     "Project type must be one of :seed-lab, :example, or :business-app."
                     {:project/id id :project/type type}))

      (and (= :seed-lab type) (not (present? (:project/workspace project))))
      (conj (problem :project-workspace
                     "Seed-lab projects must declare :project/workspace."
                     {:project/id id}))

      (and (= :business-app type) (not (present? (:project/repo project))))
      (conj (problem :project-repo
                     "Business-app projects must declare :project/repo."
                     {:project/id id}))

      (and (not (= :business-app type))
           (not (or (present? (:project/workspace project))
                    (present? (:project/repo project)))))
      (conj (problem :project-path
                     "Project must declare a workspace or repo path."
                     {:project/id id}))

      (not (every? keyword? seeds))
      (conj (problem :project-seeds
                     "Project seed references must be keywords."
                     {:project/id id :project/seeds seeds}))

      (seq (remove seed-exists? seeds))
      (conj (problem :project-seeds
                     "Project references unknown seed ids."
                     {:project/id id
                      :missing (vec (remove seed-exists? seeds))}))

      (and (some? ports)
           (not (map? ports)))
      (conj (problem :project-ports
                     "Project ports must be a map."
                     {:project/id id :project/ports ports}))

      (seq (keep (fn [[k port]]
                   (when-not (integer? port)
                     [k port]))
                 ports))
      (conj (problem :project-ports
                     "Project ports must be integers."
                     {:project/id id
                      :invalid (vec (keep (fn [[k port]]
                                            (when-not (integer? port)
                                              [k port]))
                                          ports))})))))

(defn- duplicate-project-ids
  [registry]
  (->> (projects registry)
       (map :project/id)
       frequencies
       (keep (fn [[id n]] (when (> n 1) id)))
       vec))

(defn- duplicate-ports
  [registry]
  (->> (for [project (projects registry)
             [port-key port] (:project/ports project)
             :when (integer? port)]
         {:project/id (:project/id project)
          :port-key port-key
          :port port})
       (group-by :port)
       (keep (fn [[port entries]]
               (when (> (count entries) 1)
                 {:port port :projects (mapv #(select-keys % [:project/id :port-key])
                                             entries)})))
       vec))

(defn validate-registry
  [registry]
  (vec
   (concat
    (when (not (vector? (:projects registry)))
      [(problem :projects-shape
                "Project registry must contain a :projects vector."
                {:projects (:projects registry)})])
    (mapcat validate-project (projects registry))
    (when-let [duplicates (seq (duplicate-project-ids registry))]
      [(problem :project-ids
                "Project ids must be unique."
                {:duplicates (vec duplicates)})])
    (when-let [ports (seq (duplicate-ports registry))]
      [(problem :project-ports
                "Project ports must not collide."
                {:duplicates (vec ports)})]))))

(defn valid?
  [validation]
  (empty? validation))

(defn- used-ports
  [registry]
  (set (for [project (projects registry)
             [_ port] (:project/ports project)
             :when (integer? port)]
         port)))

(defn- first-free-port
  [used start]
  (first (drop-while used (iterate inc start))))

(defn next-ports
  [registry]
  (let [used (used-ports registry)
        web (first-free-port used 3101)
        assets (first-free-port (conj used web) 5174)]
    {:web web :assets assets}))

(defn default-project
  [registry project-id opts]
  (let [id (->id project-id)
        workspace (or (:workspace opts)
                      (:repo opts)
                      (str default-workspaces-dir "/" (name id)))]
    {:project/id id
     :project/name (or (:name opts) (name id))
     :project/type (or (:type opts) :seed-lab)
     :project/status (or (:status opts) :active)
     :project/workspace workspace
     :project/repo (:repo opts)
     :project/default-branch (or (:default-branch opts) "main")
     :project/ports (or (:ports opts) (next-ports registry))
     :project/seeds (vec (or (:seeds opts) []))
     :project/personas (vec (or (:personas opts) [:snips :architect-alice]))}))

(defn upsert-project
  [registry project]
  (let [id (:project/id project)
        replaced? (volatile! false)
        updated (mapv (fn [existing]
                        (if (= id (:project/id existing))
                          (do
                            (vreset! replaced? true)
                            project)
                          existing))
                      (projects registry))]
    (assoc registry :projects (if @replaced?
                                updated
                                (conj updated project)))))

(defn create-project!
  [project-id opts]
  (let [projects-path (or (:projects-path opts) default-projects-path)
        registry (read-registry projects-path)
        id (->id project-id)]
    (when (and (project registry id) (not (:force? opts)))
      (throw (ex-info "Project already exists."
                      {:project/id id :projects-path projects-path})))
    (let [project (merge (default-project registry id opts)
                         (select-keys opts [:project/name
                                            :project/type
                                            :project/status
                                            :project/workspace
                                            :project/repo
                                            :project/default-branch
                                            :project/ports
                                            :project/seeds
                                            :project/personas]))
          registry' (upsert-project registry project)]
      (write-registry! projects-path registry')
      {:project project
       :registry registry'
       :projects-path projects-path})))

(defn- lab-file
  [seed-labs-dir lab-id]
  (fio/file seed-labs-dir (str (id-name lab-id) ".edn")))

(defn read-lab
  ([lab-id] (read-lab default-seed-labs-dir lab-id))
  ([seed-labs-dir lab-id]
   (fio/read-edn (lab-file seed-labs-dir lab-id))))

(defn write-lab!
  ([lab] (write-lab! default-seed-labs-dir lab))
  ([seed-labs-dir lab]
   (fio/write-edn! (lab-file seed-labs-dir (:lab/id lab)) lab)))

(defn default-lab
  [{:keys [lab-id project seed-ids validation-commands]}]
  {:lab/id (->id lab-id)
   :project/id (:project/id project)
   :lab/title (str (:project/name project) " Dogfood Lab")
   :lab/workspace (:project/workspace project)
   :app/name (name (:project/id project))
   :seeds (mapv (fn [seed-id]
                  {:seed/id (->id seed-id)
                   :seed/source (generator/factory-seed-root seed-id)})
                seed-ids)
   :validation/commands (vec (or validation-commands
                                  default-validation-commands))
   :lab/reports-dir (str default-state-dir "/labs/" (name (->id lab-id)))
   :commits []})

(defn- run-shell
  [dir command]
  (let [{:keys [exit out err]} (shell/sh "bash" "-lc" command :dir dir)]
    {:command command
     :exit exit
     :ok? (zero? exit)
     :out out
     :err err}))

(defn- run-git
  [workspace & args]
  (let [{:keys [exit out err]} (apply shell/sh (concat ["git"] args [:dir workspace]))]
    (when-not (zero? exit)
      (throw (ex-info "Git command failed."
                      {:args args :exit exit :out out :err err :workspace workspace})))
    (str/trim out)))

(defn- git-repo?
  [workspace]
  (.exists (io/file workspace ".git")))

(defn ensure-baseline-commit!
  [workspace message]
  (when-not (git-repo? workspace)
    (run-git workspace "init")
    (run-git workspace "checkout" "-B" "main"))
  (run-git workspace "add" ".")
  (let [status (run-git workspace "status" "--porcelain")]
    (when (present? status)
      (run-git workspace
               "-c" "user.name=Rama Factory"
               "-c" "user.email=rama-factory@example.local"
               "commit" "-m" message)))
  {:commit (run-git workspace "rev-parse" "--short" "HEAD")
   :workspace workspace})

(defn- ensure-project
  [registry project-id seed-ids opts]
  (let [id (->id project-id)]
    (or (project registry id)
        (default-project registry
                         id
                         {:seeds seed-ids
                          :type :seed-lab
                          :workspace (:workspace opts)}))))

(defn create-lab!
  [lab-id opts]
  (let [projects-path (or (:projects-path opts) default-projects-path)
        seed-labs-dir (or (:seed-labs-dir opts) default-seed-labs-dir)
        registry (read-registry projects-path)
        lab-id (->id lab-id)
        project-id (->id (or (:project-id opts) lab-id))
        seed-ids (vec (map ->id (or (:seed-ids opts)
                                    (:seeds opts)
                                    [(or (:seed-id opts) :factory-dashboard)])))
        project (update (ensure-project registry project-id seed-ids opts)
                        :project/seeds
                        #(vec (distinct (concat (or % []) seed-ids))))
        registry' (upsert-project registry project)
        workspace (:project/workspace project)
        force? (:force? opts)
        app (generator/create-app! (name (:project/id project))
                                   workspace
                                   {:force? force?})
        installs (mapv (fn [seed-id]
                         (generator/install-extension! workspace
                                                       (name seed-id)
                                                       (generator/factory-seed-root seed-id)
                                                       {:force? force?}))
                       seed-ids)
        lab (default-lab {:lab-id lab-id
                          :project project
                          :seed-ids seed-ids
                          :validation-commands (:validation-commands opts)})
        commit (when (not= false (:commit? opts))
                 (ensure-baseline-commit! workspace
                                          (str "Baseline " (name lab-id) " seed lab")))]
    (write-registry! projects-path registry')
    (write-lab! seed-labs-dir lab)
    {:project project
     :registry registry'
     :projects-path projects-path
     :lab lab
     :lab-path (.getPath (io/file (lab-file seed-labs-dir lab-id)))
     :app app
     :installs installs
     :commit commit}))

(defn- validation-report-path
  [state-dir lab-id]
  (fio/file state-dir
            "labs"
            (name (->id lab-id))
            "validations"
            (str (events/now-ms) ".edn")))

(defn validate-lab!
  [lab-id opts]
  (let [projects-path (or (:projects-path opts) default-projects-path)
        seed-labs-dir (or (:seed-labs-dir opts) default-seed-labs-dir)
        state-dir (or (:state-dir opts) default-state-dir)
        registry (read-registry projects-path)
        lab (read-lab seed-labs-dir lab-id)
        project (project registry (:project/id lab))
        workspace (or (:lab/workspace lab)
                      (:project/workspace project)
                      (:project/repo project))
        commands (:validation/commands lab)
        results (mapv #(run-shell workspace %) commands)
        valid? (every? :ok? results)
        occurred-at (events/now-ms)
        event-type (if valid? :validation-passed :validation-failed)
        event (events/append!
               state-dir
               {:event-id (str (name event-type) "-" (name (:lab/id lab)) "-" occurred-at)
                :event-type event-type
                :project-id (name (:project/id lab))
                :run-id (name (:lab/id lab))
                :work-id (str (name (:lab/id lab)) "-validation")
                :role "system"
                :persona-id "system"
                :persona-name "System"
                :phase "lab-validation"
                :artifact (str (:lab/id lab))
                :status (if valid? "passed" "failed")
                :message (if valid?
                           "Lab validation passed."
                           "Lab validation failed.")
                :occurred-at occurred-at})
        report {:lab/id (:lab/id lab)
                :project/id (:project/id lab)
                :workspace workspace
                :valid? valid?
                :commands results
                :event (dissoc event :file)}
        report-path (.getPath (io/file (validation-report-path state-dir lab-id)))]
    (fio/write-edn! report-path report)
    (assoc report :report-path report-path)))
