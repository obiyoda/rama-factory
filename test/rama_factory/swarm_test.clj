(ns rama-factory.swarm-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rama-factory.io :as fio]
            [rama-factory.swarm :as swarm]))

(def factory (fio/read-edn "factory/factory.edn"))

(deftest renders-swarmforge-compatible-window-config
  (let [config (swarm/render-config factory)]
    (testing "role order follows the compact SwarmForge flow"
      (is (str/includes? config "window specifier codex master task"))
      (is (< (.indexOf config "window specifier")
             (.indexOf config "window coder")
             (.indexOf config "window refactorer")
             (.indexOf config "window architect"))))
    (testing "batch receive mode is preserved"
      (is (str/includes? config "window architect codex architect batch")))))

(deftest worktree-plan-keeps-main-role-in-root
  (let [plan (swarm/worktree-plan factory)
        roles (into {} (map (juxt :role identity) (:roles plan)))]
    (is (= :devenv (:driver plan)))
    (is (= "." (get-in roles [:specifier :path])))
    (is (= ".worktrees/coder" (get-in roles [:coder :path])))
    (is (= "swarmforge-coder" (get-in roles [:coder :branch])))))
