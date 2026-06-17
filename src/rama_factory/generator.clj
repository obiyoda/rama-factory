(ns rama-factory.generator
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [rama-factory.io :as fio]))

(def rpl-maven-repo "https://nexus.redplanetlabs.com/repository/maven-public-releases")

(defn app-path
  [app-name]
  (-> app-name
      str/lower-case
      (str/replace #"[^a-z0-9-]+" "-")
      (str/replace #"-+" "-")
      (str/replace #"(^-|-$)" "")
      (str/replace "-" "_")))

(defn app-ns
  [app-name]
  (-> app-name
      str/lower-case
      (str/replace #"[^a-z0-9-]+" "-")
      (str/replace #"-+" "-")
      (str/replace #"(^-|-$)" "")))

(defn- titleize
  [app-name]
  (->> (str/split (app-ns app-name) #"-")
       (map str/capitalize)
       (str/join " ")))

(defn app-config
  [app-name]
  {:app/name (app-ns app-name)
   :app/title (titleize app-name)
   :app/ns (app-ns app-name)
   :app/path (app-path app-name)
   :extensions []})

(defn- file
  [root & parts]
  (apply io/file root parts))

(defn- ensure-parent!
  [f]
  (when-let [parent (.getParentFile (io/file f))]
    (.mkdirs parent)))

(defn- write-text!
  [target content {:keys [force?]}]
  (let [target-file (io/file target)]
    (when (and (.exists target-file) (not force?))
      (throw (ex-info "Refusing to overwrite existing file."
                      {:path (.getPath target-file)})))
    (ensure-parent! target-file)
    (spit target-file content)
    (.getPath target-file)))

(defn- render
  [template replacements]
  (reduce-kv
   (fn [s k v]
     (str/replace s (str "{{" (name k) "}}") (str v)))
   template
   replacements))

(defn- replacements
  [{:app/keys [name title ns path]}]
  {:app_name name
   :app_title title
   :app_ns ns
   :app_path path})

(defn- starter-files
  [{:app/keys [name title ns path] :as app}]
  {".gitignore"
   (str ".devenv/\n"
        ".rama-factory/\n"
        ".worktrees/\n"
        "runs/\n"
        "target/\n"
        ".cpcache/\n")

   "README.md"
   (str "# " title "\n\n"
        "A Rama Factory starter app.\n\n"
        "## Commands\n\n"
        "```bash\n"
        "devenv shell\n"
        "devenv up\n"
        "devenv test\n"
        "clojure -M:test\n"
        "```\n\n"
        "Extensions are copied into this app as owned source seeds.\n")

   "deps.edn"
   (str "{:paths [\"src\" \"resources\"]\n"
        " :mvn/repos {\"rpl\" {:url \"" rpl-maven-repo "\"}}\n"
        " :deps {org.clojure/clojure {:mvn/version \"1.12.4\"}\n"
        "        com.rpl/rama {:mvn/version \"1.8.0\"}\n"
        "        com.github.brettatoms/zodiac {:mvn/version \"0.9.98\"}}\n"
        " :aliases\n"
        " {:dev {:main-opts [\"-m\" \"" ns ".main\"]}\n"
        "  :test {:extra-paths [\"test\"]\n"
        "         :main-opts [\"-m\" \"" ns ".test-runner\"]}}}\n")

   "devenv.nix"
   (str "{ pkgs, ... }:\n\n"
        "let\n"
        "  appPort = 3000;\n"
        "in\n"
        "{\n"
        "  languages.clojure.enable = true;\n"
        "  languages.clojure.lsp.enable = true;\n\n"
        "  packages = [ pkgs.curl pkgs.git pkgs.ripgrep pkgs.zsh ];\n\n"
        "  env.RAMA_FACTORY_APP = \"" name "\";\n\n"
        "  tasks.\"app:test\".exec = \"clojure -M:test\";\n"
        "  tasks.\"app:serve\".exec = \"clojure -M:dev ${builtins.toString appPort}\";\n\n"
        "  processes.web = {\n"
        "    exec = \"clojure -M:dev ${builtins.toString appPort}\";\n"
        "    ready.http.get = { port = appPort; path = \"/\"; };\n"
        "  };\n\n"
        "  enterTest = ''\n"
        "    clojure -M:test\n"
        "  '';\n"
        "}\n")

   "rama-factory.edn"
   (with-out-str (prn app))

   "factory/factory.edn"
   (str "{:factory/name \"" name "\"\n"
        " :factory/description \"Rama Factory generated app.\"\n"
        " :roles {:specifier {:agent :codex :worktree :master :receive-mode :task}\n"
        "         :architect {:agent :codex :worktree :architect :receive-mode :batch}\n"
        "         :coder {:agent :codex :worktree :coder :receive-mode :task}\n"
        "         :refactorer {:agent :codex :worktree :refactorer :receive-mode :task}}\n"
        " :swarm {:driver :devenv\n"
        "         :transport :git-worktree-handoff\n"
        "         :worktrees-dir \".worktrees\"\n"
        "         :role-order [:specifier :coder :refactorer :architect]}}\n")

   "factory/extensions/.keep"
   ""

   "factory/personas.edn"
   (str "{:personas\n"
        " [{:persona/id :snips\n"
        "   :persona/name \"Snips\"\n"
        "   :persona/default-role :coder\n"
        "   :persona/runtime :codex\n"
        "   :persona/skills [:rama-factory-build]\n"
        "   :persona/style {:communication :concise :patch-size :small}\n"
        "   :persona/permissions {:can-edit? true :can-run-tests? true :can-create-handoffs? true :can-push? false}\n"
        "   :persona/event-tags {:display-name \"Snips\" :avatar-color \"#2f6fed\"}}\n"
        "  {:persona/id :architect-alice\n"
        "   :persona/name \"ArchitectAlice\"\n"
        "   :persona/default-role :architect\n"
        "   :persona/runtime :codex\n"
        "   :persona/skills [:rama-factory-build]\n"
        "   :persona/style {:communication :direct :review-depth :deep}\n"
        "   :persona/permissions {:can-edit? true :can-run-tests? true :can-create-handoffs? true :can-push? false}\n"
        "   :persona/event-tags {:display-name \"ArchitectAlice\" :avatar-color \"#7c3aed\"}}]}\n")

   "factory/skills/rama-factory-build/SKILL.md"
   (str "---\n"
        "name: rama-factory-build\n"
        "description: Build or modify this Rama Factory app, its extensions, and its agent handoffs.\n"
        "---\n\n"
        "# Rama Factory Build\n\n"
        "Use this skill to preserve the app's explicit Rama state model, devenv workflow, "
        "Zodiac routes, copied extension seeds, and role-based handoff discipline.\n")

   "resources/log4j2.properties"
   (str "appender.console.name = console\n"
        "appender.console.type = Console\n"
        "appender.console.Target = SYSTEM_ERR\n"
        "appender.console.layout.type = PatternLayout\n"
        "appender.console.layout.pattern = %d{HH:mm:ss.SSS} %-5p [%t] %.20c - %m%n%throwable\n"
        "appender.console.immediateFlush=true\n"
        "rootLogger.level=ERROR\n"
        "rootLogger.appenderRefs = console\n"
        "rootLogger.appenderRef.console.ref = console\n"
        "logger.rama.name=rpl.rama\n"
        "logger.rama.level=WARN\n")

   (str "src/" path "/web.clj")
   (str "(ns " ns ".web\n"
        "  (:require [clojure.edn :as edn]\n"
        "            [clojure.java.io :as io]))\n\n"
        "(def extension-route-vars\n"
        "  {:auth \"" ns ".auth.routes/routes\"\n"
        "   :factory-dashboard \"" ns ".factory-dashboard.routes/routes\"})\n\n"
        "(defn app-config\n"
        "  []\n"
        "  (let [f (io/file \"rama-factory.edn\")]\n"
        "    (if (.exists f)\n"
        "      (edn/read-string (slurp f))\n"
        "      {:extensions []})))\n\n"
        "(defn load-routes\n"
        "  [qualified-var]\n"
        "  (try\n"
        "    (let [sym (symbol qualified-var)]\n"
        "      (require (symbol (namespace sym)))\n"
        "      (deref (resolve sym)))\n"
        "    (catch Exception _\n"
        "      [])))\n\n"
        "(defn extension-routes\n"
        "  []\n"
        "  (->> (:extensions (app-config))\n"
        "       (keep extension-route-vars)\n"
        "       (mapcat load-routes)\n"
        "       vec))\n\n"
        "(defn home\n"
        "  [_request]\n"
        "  {:status 200\n"
        "   :headers {\"Content-Type\" \"text/html; charset=utf-8\"}\n"
        "   :body \"<main><h1>" title "</h1><p>Rama Factory starter app.</p></main>\"})\n\n"
        "(def base-routes\n"
        "  [[\"/\" {:get home}]])\n\n"
        "(def routes\n"
        "  (vec (concat base-routes (extension-routes))))\n")

   (str "src/" path "/main.clj")
   (str "(ns " ns ".main\n"
        "  (:require [" ns ".web :as web]\n"
        "            [zodiac.core :as zodiac]))\n\n"
        "(defn -main\n"
        "  [& [port]]\n"
        "  (zodiac/start {:routes web/routes\n"
        "                 :port (parse-long (or port \"3000\"))\n"
        "                 :cookie-secret \"starter-secret12\"\n"
        "                 :cookie-name \"" name "-session\"\n"
        "                 :reload-per-request? true}))\n")

   (str "test/" path "/web_test.clj")
   (str "(ns " ns ".web-test\n"
        "  (:require [clojure.test :refer [deftest is]]\n"
        "            [" ns ".web :as web]))\n\n"
        "(deftest home-renders\n"
        "  (let [response (web/home {})]\n"
        "    (is (= 200 (:status response)))\n"
        "    (is (re-find #\"" title "\" (:body response)))))\n")

   (str "test/" path "/test_runner.clj")
   (str "(ns " ns ".test-runner\n"
        "  (:require [clojure.java.io :as io]\n"
        "            [clojure.string :as str]\n"
        "            [clojure.test :as test]))\n\n"
        "(defn- test-file?\n"
        "  [f]\n"
        "  (and (.isFile f) (str/ends-with? (.getName f) \"_test.clj\")))\n\n"
        "(defn- file->ns\n"
        "  [root f]\n"
        "  (let [root-path (.getCanonicalPath (io/file root))\n"
        "        file-path (.getCanonicalPath f)\n"
        "        rel (subs file-path (inc (count root-path)))\n"
        "        no-ext (subs rel 0 (- (count rel) 4))]\n"
        "    (symbol (-> no-ext\n"
        "                (str/replace java.io.File/separator \".\")\n"
        "                (str/replace \"_\" \"-\")))))\n\n"
        "(defn -main\n"
        "  [& _args]\n"
        "  (let [namespaces (->> (file-seq (io/file \"test\"))\n"
        "                        (filter test-file?)\n"
        "                        (map #(file->ns \"test\" %))\n"
        "                        sort\n"
        "                        vec)]\n"
        "    (doseq [ns-sym namespaces]\n"
        "      (require ns-sym))\n"
        "    (let [{:keys [fail error]} (apply test/run-tests namespaces)]\n"
        "      (System/exit (if (zero? (+ fail error)) 0 1)))))\n")})

(defn create-app!
  ([app-name]
   (create-app! app-name app-name {}))
  ([app-name target-dir opts]
   (let [app (app-config app-name)
         root (io/file target-dir)
         written (for [[path content] (starter-files app)]
                   (write-text! (file root path) content opts))]
     {:app app
      :root (.getPath root)
      :written (vec written)})))

(defn read-app-config
  [root]
  (let [config-file (file root "rama-factory.edn")]
    (when-not (.exists config-file)
      (throw (ex-info "No rama-factory.edn found in target app."
                      {:root (.getPath (io/file root))})))
    (fio/read-edn (.getPath config-file))))

(defn read-seed
  [seed-root]
  (let [seed-file (file seed-root "seed.edn")]
    (when-not (.exists seed-file)
      (throw (ex-info "No seed.edn found for extension seed."
                      {:seed-root (.getPath (io/file seed-root))})))
    (assoc (edn/read-string (slurp seed-file))
           :seed/root (.getPath (io/file seed-root)))))

(defn validate-seed!
  [seed-root]
  (let [{:seed/keys [templates] :as seed} (read-seed seed-root)
        missing (->> templates
                     (map :source)
                     (map #(file seed-root %))
                     (remove #(.exists %))
                     (map #(.getPath %))
                     vec)]
    (when (seq missing)
      (throw (ex-info "Extension seed is missing template files."
                      {:missing missing})))
    seed))

(defn- install-template!
  [app seed-root template replacements opts]
  (let [source (file seed-root (:source template))
        target-path (render (:target template) replacements)
        target (file (:target-root opts) target-path)
        content (render (slurp source) replacements)]
    (write-text! target content opts)))

(defn install-extension!
  [target-root extension-id seed-root opts]
  (let [app (read-app-config target-root)
        seed (validate-seed! seed-root)
        extension-id (keyword extension-id)
        replacements (replacements app)
        templates (:seed/templates seed)
        opts (assoc opts :target-root target-root)
        written (mapv #(install-template! app seed-root % replacements opts) templates)
        config-path (file target-root "rama-factory.edn")
        updated-app (update app :extensions
                            (fn [extensions]
                              (vec (distinct (conj (or extensions []) extension-id)))))]
    (fio/write-edn! (.getPath config-path) updated-app)
    {:app updated-app
     :seed (select-keys seed [:seed/id :seed/version :seed/summary])
     :written written
     :config (.getPath config-path)}))

(defn factory-seed-root
  [extension-id]
  (.getPath (file "factory" "seeds" (name (keyword extension-id)))))
