(ns rama-factory.projects-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [rama-factory.io :as fio]
            [rama-factory.projects :as projects]))

(defn- temp-dir
  []
  (.toFile (java.nio.file.Files/createTempDirectory
            "rama-factory-projects-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- path
  [root & parts]
  (.getPath (apply io/file root parts)))

(deftest validates-project-registry
  (let [registry {:projects [{:project/id :factory-floor
                              :project/name "Factory Floor"
                              :project/type :seed-lab
                              :project/workspace ".rama-workspaces/factory-floor"
                              :project/ports {:web 3101 :assets 5174}
                              :project/seeds [:factory-dashboard]}
                             {:project/id :invoice-app
                              :project/name "Invoice App"
                              :project/type :business-app
                              :project/repo "../invoice-app"
                              :project/ports {:web 3101}
                              :project/seeds [:missing-seed]}]}
        validation (projects/validate-registry registry)
        checks (set (map :check validation))]
    (is (contains? checks :project-ports))
    (is (contains? checks :project-seeds))))

(deftest creates-and-validates-seed-lab
  (let [root (.getPath (temp-dir))
        projects-path (path root "factory" "projects.edn")
        seed-labs-dir (path root "factory" "seed-labs")
        workspace (path root ".rama-workspaces" "factory-floor")
        state-dir (path root ".rama-factory")]
    (try
      (let [created (projects/create-lab!
                     :factory-floor
                     {:projects-path projects-path
                      :seed-labs-dir seed-labs-dir
                      :state-dir state-dir
                      :project-id :factory-floor
                      :workspace workspace
                      :seed-ids [:factory-dashboard]
                      :validation-commands ["printf ok"]
                      :commit? false})
            registry (projects/read-registry projects-path)]
        (testing "lab-new creates registry, manifest, starter app, and copied seed"
          (is (= :factory-floor (get-in created [:project :project/id])))
          (is (= [:factory-dashboard]
                 (get-in created [:project :project/seeds])))
          (is (.exists (io/file workspace "rama-factory.edn")))
          (is (.exists (io/file workspace "src/factory_floor/factory_dashboard/routes.clj")))
          (is (.exists (io/file seed-labs-dir "factory-floor.edn")))
          (is (some? (projects/project registry :factory-floor))))
        (testing "lab-validate runs commands and records a report"
          (let [report (projects/validate-lab!
                        :factory-floor
                        {:projects-path projects-path
                         :seed-labs-dir seed-labs-dir
                         :state-dir state-dir})]
            (is (:valid? report))
            (is (= 0 (get-in report [:commands 0 :exit])))
            (is (.exists (io/file (:report-path report))))
            (is (= "factory-floor"
                   (get-in report [:event :project-id]))))))
      (finally
        (fio/delete-tree! root)))))
