(ns rama-factory.swarm
  (:require [clojure.string :as str]))

(def default-worktrees-dir ".worktrees")

(defn- kw-name
  [value]
  (cond
    (keyword? value) (name value)
    (symbol? value) (name value)
    :else (str value)))

(defn role-order
  [factory]
  (let [configured (get-in factory [:swarm :role-order])]
    (if (seq configured)
      configured
      (keys (:roles factory)))))

(defn role-row
  [factory role]
  (let [role-config (get-in factory [:roles role])]
    (when-not role-config
      (throw (ex-info "Unknown swarm role."
                      {:role role
                       :known-roles (set (keys (:roles factory)))})))
    {:role role
     :agent (or (:agent role-config) :codex)
     :worktree (or (:worktree role-config) :master)
     :receive-mode (or (:receive-mode role-config) :task)
     :owns (:owns role-config)}))

(defn rows
  [factory]
  (mapv #(role-row factory %) (role-order factory)))

(defn render-config
  [factory]
  (str
   "# Generated from factory/factory.edn\n"
   "# Format: window <role> <agent> <worktree> [task|batch]\n"
   (str/join
    "\n"
    (for [{:keys [role agent worktree receive-mode]} (rows factory)]
      (format "window %s %s %s %s"
              (kw-name role)
              (kw-name agent)
              (kw-name worktree)
              (kw-name receive-mode))))
   "\n"))

(defn main-worktree?
  [worktree]
  (#{"master" "main" "none"} (kw-name worktree)))

(defn worktree-path
  [factory worktree]
  (if (main-worktree? worktree)
    "."
    (str (or (get-in factory [:swarm :worktrees-dir]) default-worktrees-dir)
         "/"
         (kw-name worktree))))

(defn branch-name
  [worktree]
  (when-not (main-worktree? worktree)
    (str "swarmforge-" (kw-name worktree))))

(defn worktree-plan
  [factory]
  {:driver (get-in factory [:swarm :driver] :devenv)
   :transport (get-in factory [:swarm :transport] :git-worktree-handoff)
   :worktrees-dir (or (get-in factory [:swarm :worktrees-dir])
                      default-worktrees-dir)
   :roles
   (mapv (fn [{:keys [role agent worktree receive-mode owns]}]
           {:role role
            :agent agent
            :worktree worktree
            :path (worktree-path factory worktree)
            :branch (branch-name worktree)
            :receive-mode receive-mode
            :owns owns})
         (rows factory))})
