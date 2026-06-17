(ns rama-factory.generator-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rama-factory.cli :as cli]
            [rama-factory.generator :as generator]
            [rama-factory.io :as fio])
  (:import [java.nio.file Files]))

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory "rama-factory-generator"
                                      (into-array java.nio.file.attribute.FileAttribute []))))

(defn- path
  [root & parts]
  (.getPath (apply io/file root parts)))

(deftest app-name-is-normalized-for-clojure
  (is (= "invoice_app" (generator/app-path "Invoice App")))
  (is (= "invoice-app" (generator/app-ns "Invoice App"))))

(deftest creates-starter-app
  (let [root (temp-dir)]
    (try
      (let [result (generator/create-app! "Invoice App" (path root "invoice-app") {})]
        (is (= "invoice-app" (get-in result [:app :app/name])))
        (is (.exists (io/file (:root result) "devenv.nix")))
        (is (.exists (io/file (:root result) "deps.edn")))
        (is (.exists (io/file (:root result) "rama-factory.edn")))
        (is (.exists (io/file (:root result) "factory/skills/rama-factory-build/SKILL.md")))
        (is (str/includes? (slurp (io/file (:root result) "deps.edn"))
                           "com.rpl/rama")))
      (finally
        (fio/delete-tree! root)))))

(deftest auth-seed-installs-into-starter-app
  (let [root (temp-dir)]
    (try
      (let [app-root (path root "invoice-app")]
        (generator/create-app! "Invoice App" app-root {})
        (let [result (generator/install-extension! app-root
                                                   "auth"
                                                   (generator/factory-seed-root :auth)
                                                   {})
              config (fio/read-edn (path app-root "rama-factory.edn"))
              module-source (slurp (path app-root "src/invoice_app/auth/module.clj"))]
          (is (= [:auth] (:extensions config)))
          (is (= 10 (count (:written result))))
          (is (.exists (io/file app-root "test/invoice_app/auth/module_test.clj")))
          (is (.exists (io/file app-root "factory/skills/auth-extension/SKILL.md")))
          (is (str/includes? module-source "invoice-app.auth.module"))
          (is (not (str/includes? module-source "{{app_ns}}")))))
      (finally
        (fio/delete-tree! root)))))

(deftest cli-commands-create-and-install
  (let [root (temp-dir)]
    (try
      (let [app-root (path root "crm-app")
            create-output (with-out-str
                            (cli/new-command ["CRM App" app-root]))
            _ (is (str/includes? create-output "created app crm-app"))
            add-output (with-out-str
                         (cli/add-command ["auth"
                                           "--from"
                                           (generator/factory-seed-root :auth)
                                           "--target"
                                           app-root]))]
        (is (str/includes? add-output "added auth"))
        (is (.exists (io/file app-root "src/crm_app/auth/routes.clj"))))
      (finally
        (fio/delete-tree! root)))))

(deftest factory-dashboard-seed-installs-into-starter-app
  (let [root (temp-dir)]
    (try
      (let [app-root (path root "factory-floor")]
        (generator/create-app! "Factory Floor" app-root {})
        (let [result (generator/install-extension! app-root
                                                   "factory-dashboard"
                                                   (generator/factory-seed-root :factory-dashboard)
                                                   {})
              config (fio/read-edn (path app-root "rama-factory.edn"))
              module-source (slurp (path app-root "src/factory_floor/factory_dashboard/module.clj"))
              web-source (slurp (path app-root "src/factory_floor/web.clj"))]
          (is (= [:factory-dashboard] (:extensions config)))
          (is (= 10 (count (:written result))))
          (is (.exists (io/file app-root "test/factory_floor/factory_dashboard/module_test.clj")))
          (is (.exists (io/file app-root "factory/skills/factory-dashboard/SKILL.md")))
          (is (str/includes? module-source "factory-floor.factory-dashboard.module"))
          (is (str/includes? web-source ":factory-dashboard"))))
      (finally
        (fio/delete-tree! root)))))

(deftest auth-seed-package-is-valid
  (let [seed (generator/validate-seed! (generator/factory-seed-root :auth))]
    (is (= :auth (:seed/id seed)))
    (is (= 10 (count (:seed/templates seed))))))

(deftest factory-dashboard-seed-package-is-valid
  (let [seed (generator/validate-seed! (generator/factory-seed-root :factory-dashboard))]
    (is (= :factory-dashboard (:seed/id seed)))
    (is (= 10 (count (:seed/templates seed))))))
