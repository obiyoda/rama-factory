(ns rama-factory.model
  (:require [clojure.set :as set]))

(def required-challenge-keys
  #{:challenge/id :challenge/title :challenge/namespace :contract :rama :acceptance})

(defn- problem
  [check message data]
  {:check check :message message :data data})

(defn duplicate-values
  [xs]
  (->> xs
       frequencies
       (keep (fn [[x n]] (when (> n 1) x)))
       set))

(defn validate-factory
  [factory]
  (let [roles (set (keys (:roles factory)))
        phases (get-in factory [:workflow :phases])
        owners (set (map :owner phases))
        phase-ids (map :id phases)
        missing-owners (set/difference owners roles)]
    (cond-> []
      (empty? roles)
      (conj (problem :roles "Factory must define at least one role." {}))

      (empty? phases)
      (conj (problem :phases "Factory must define a Rama development phase list." {}))

      (seq missing-owners)
      (conj (problem :phase-owners
                     "Every phase owner must be a configured role."
                     {:missing missing-owners}))

      (seq (duplicate-values phase-ids))
      (conj (problem :phase-ids
                     "Phase ids must be unique."
                     {:duplicates (duplicate-values phase-ids)}))

      (some #(nil? (:artifact %)) phases)
      (conj (problem :phase-artifacts
                     "Every phase must name its output artifact."
                     {:phases (filter #(nil? (:artifact %)) phases)})))))

(defn- validate-operation
  [op]
  (cond-> []
    (nil? (:id op))
    (conj (problem :operation-id "Operation must have an id." op))

    (nil? (:kind op))
    (conj (problem :operation-kind "Operation must declare :command or :query." op))))

(defn- validate-pstate
  [pstate]
  (cond-> []
    (nil? (:id pstate))
    (conj (problem :pstate-id "PState must have an id." pstate))

    (nil? (:partition-key pstate))
    (conj (problem :partition-alignment
                   "PState must declare a partition key."
                   pstate))

    (and (:may-grow? pstate)
         (not (:subindex? pstate))
         (not (:bounded? pstate)))
    (conj (problem :growth-control
                   "Growing PState collections must be subindexed or explicitly bounded."
                   pstate))))

(defn- validate-query
  [query]
  (cond-> []
    (nil? (:id query))
    (conj (problem :query-id "Query must have an id." query))

    (nil? (:route query))
    (conj (problem :partition-alignment
                   "Query must declare how it routes to the task owning the PState keys."
                   query))

    (nil? (:expected-io query))
    (conj (problem :read-path-cost
                   "Query should estimate its read path cost."
                   query))))

(defn validate-challenge
  [challenge]
  (let [missing (set/difference required-challenge-keys (set (keys challenge)))
        operations (get-in challenge [:contract :operations])
        depots (get-in challenge [:rama :depots])
        pstates (get-in challenge [:rama :pstates])
        queries (get-in challenge [:rama :queries])
        topology-faults (keep :fault-tolerance (get-in challenge [:rama :topologies]))]
    (vec
     (concat
      (when (seq missing)
        [(problem :challenge-shape
                  "Challenge is missing required top-level keys."
                  {:missing missing})])
      (when (empty? operations)
        [(problem :contract "Challenge contract must define operations." {})])
      (mapcat validate-operation operations)
      (when (empty? depots)
        [(problem :depots "Rama challenge must define at least one depot." {})])
      (when (empty? pstates)
        [(problem :pstates "Rama challenge must define at least one PState." {})])
      (mapcat validate-pstate pstates)
      (when (empty? queries)
        [(problem :queries "Rama challenge must define at least one query." {})])
      (mapcat validate-query queries)
      (when (empty? topology-faults)
        [(problem :fault-tolerance
                  "At least one topology must state its retry/fault-tolerance behavior."
                  {})])))))

(defn validate
  [factory challenge]
  {:factory (validate-factory factory)
   :challenge (validate-challenge challenge)})

(defn valid?
  [validation]
  (every? empty? (vals validation)))

(defn phase-owner
  [factory phase-id]
  (some (fn [phase] (when (= phase-id (:id phase)) (:owner phase)))
        (get-in factory [:workflow :phases])))

(defn phase-sequence
  [factory]
  (vec (get-in factory [:workflow :phases])))

