(ns rama-factory.product-spine-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rama-factory.io :as fio]))

(deftest product-docs-exist
  (testing "the repo has a product spine"
    (doseq [path ["docs/charter.md" "docs/roadmap.md"]]
      (is (.exists (clojure.java.io/file path)))))
  (testing "the charter states the core product thesis"
    (let [charter (slurp "docs/charter.md")]
      (is (str/includes? charter "Clojure"))
      (is (str/includes? charter "Rama"))
      (is (str/includes? charter "Laravel-level developer experience")))))

(deftest auth-extension-manifest-is-parseable
  (let [manifest (fio/read-edn "factory/extensions/auth.edn")]
    (is (= :auth (:extension/id manifest)))
    (is (= :draft (:extension/status manifest)))
    (is (contains? (set (:extension/provides manifest)) :sessions))
    (is (seq (get-in manifest [:rama :pstates])))
    (is (seq (:validators manifest)))
    (is (seq (:skills manifest)))))

(deftest local-skill-pack-is-discoverable
  (let [skill-path "factory/skills/rama-factory-build/SKILL.md"
        skill (slurp skill-path)]
    (is (.exists (clojure.java.io/file skill-path)))
    (is (str/includes? skill "name: rama-factory-build"))
    (is (str/includes? skill "references/roles.md"))
    (is (.exists (clojure.java.io/file
                  "factory/skills/rama-factory-build/references/extensions.md")))))

(deftest named-personas-are-discoverable
  (let [personas (:personas (fio/read-edn "factory/personas.edn"))
        names (set (map :persona/name personas))]
    (is (contains? names "Snips"))
    (is (contains? names "ArchitectAlice"))
    (is (every? :persona/skills personas))))

(deftest factory-floor-project-lab-is-discoverable
  (let [projects (:projects (fio/read-edn "factory/projects.edn"))
        lab (fio/read-edn "factory/seed-labs/factory-floor.edn")]
    (is (some #(= :factory-floor (:project/id %)) projects))
    (is (= :factory-floor (:lab/id lab)))
    (is (= :factory-floor (:project/id lab)))
    (is (= ".rama-workspaces/factory-floor" (:lab/workspace lab)))))
