(ns rama-factory.test-runner
  (:require [clojure.test :as test]
            [rama-factory.app-test]
            [rama-factory.docs-test]
            [rama-factory.events-test]
            [rama-factory.generator-test]
            [rama-factory.handoff-test]
            [rama-factory.mcp-test]
            [rama-factory.model-test]
            [rama-factory.product-spine-test]
            [rama-factory.swarm-test]
            [rama-factory.workflow-test]))

(defn -main
  [& _args]
  (let [{:keys [fail error]} (test/run-tests
                              'rama-factory.app-test
                              'rama-factory.docs-test
                              'rama-factory.events-test
                              'rama-factory.generator-test
                              'rama-factory.model-test
                              'rama-factory.mcp-test
                              'rama-factory.product-spine-test
                              'rama-factory.swarm-test
                              'rama-factory.handoff-test
                              'rama-factory.workflow-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
