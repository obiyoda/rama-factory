(ns rama-factory.model-test
  (:require [clojure.test :refer [deftest is testing]]
            [rama-factory.io :as fio]
            [rama-factory.model :as model]))

(def factory (fio/read-edn "factory/factory.edn"))
(def challenge (fio/read-edn "factory/challenges/bank-transfer.edn"))
(def personas (:personas (fio/read-edn "factory/personas.edn")))

(deftest valid-factory-and-challenge
  (testing "the checked-in PoC configuration is valid"
    (is (model/valid? (model/validate factory challenge personas)))))

(deftest personas-reference-configured-roles-and-skills
  (let [validation (model/validate factory challenge personas)]
    (is (empty? (:personas validation)))))

(deftest persona-with-missing-role-is-invalid
  (let [broken (assoc-in personas [0 :persona/default-role] :missing-role)
        problems (:personas (model/validate factory challenge broken))]
    (is (some #(= :persona-role (:check %)) problems))))

(deftest persona-with-unknown-skill-is-invalid
  (let [broken (update-in personas [0 :persona/skills] conj :unknown-skill)
        problems (:personas (model/validate factory challenge broken))]
    (is (some #(= :persona-skills (:check %)) problems))))

(deftest missing-phase-owner-is-invalid
  (let [broken (assoc-in factory [:workflow :phases 0 :owner] :missing-role)
        problems (model/validate-factory broken)]
    (is (= #{:phase-owners} (set (map :check problems))))))

(deftest growing-pstate-must-be-subindexed-or-bounded
  (let [broken (assoc-in challenge [:rama :pstates 0 :subindex?] false)
        problems (model/validate-challenge broken)]
    (is (some #(= :growth-control (:check %)) problems))))

(deftest query-route-is-required
  (let [broken (update-in challenge [:rama :queries 0] dissoc :route)
        problems (model/validate-challenge broken)]
    (is (some #(= :partition-alignment (:check %)) problems))))
