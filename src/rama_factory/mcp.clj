(ns rama-factory.mcp
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [rama-factory.events :as events]
            [rama-factory.handoff :as handoff]
            [rama-factory.io :as fio]
            [rama-factory.model :as model]
            [rama-factory.persona :as persona]))

(def protocol-version "2025-11-25")

(def supported-protocols
  #{protocol-version "2025-06-18" "2025-03-26" "2024-11-05"})

(def default-paths
  {:factory-path "factory/factory.edn"
   :challenge-path "factory/challenges/bank-transfer.edn"
   :personas-path "factory/personas.edn"})

(defn- read-edn-if-exists
  [path fallback]
  (let [file (io/file path)]
    (if (.exists file)
      (fio/read-edn (.getPath file))
      fallback)))

(defn- skill-catalog
  []
  [{:id "rama-factory-build"
    :name "Rama Factory Build"
    :source :factory
    :path "factory/skills/rama-factory-build/SKILL.md"}
   {:id "auth-extension"
    :name "Auth Extension"
    :source :seed
    :path "factory/seeds/auth/templates/skills/auth-extension/SKILL.md"}
   {:id "factory-dashboard"
    :name "Factory Dashboard"
    :source :seed
    :path "factory/seeds/factory-dashboard/templates/skills/factory-dashboard/SKILL.md"}])

(defn- existing-skills
  []
  (->> (skill-catalog)
       (filter #(.exists (io/file (:path %))))
       vec))

(defn load-context
  ([] (load-context {}))
  ([opts]
   (let [{:keys [factory-path challenge-path personas-path]}
         (merge default-paths opts)
         factory (read-edn-if-exists factory-path {})
         state-dir (or (get-in factory [:workflow :state-dir])
                       ".rama-factory")]
     {:factory factory
      :challenge (read-edn-if-exists challenge-path {})
      :personas (:personas (read-edn-if-exists personas-path {:personas []}))
      :state-dir state-dir
      :skills (existing-skills)})))

(defn- json-name
  [value]
  (cond
    (keyword? value) (if-let [ns (namespace value)]
                       (str ns "/" (name value))
                       (name value))
    (symbol? value) (str value)
    :else (str value)))

(defn- jsonable
  [value]
  (cond
    (keyword? value) (json-name value)
    (symbol? value) (str value)
    (instance? java.io.File value) (.getPath ^java.io.File value)
    (map? value) (into {}
                       (map (fn [[k v]]
                              [(json-name k) (jsonable v)]))
                       value)
    (set? value) (mapv jsonable (sort-by str value))
    (sequential? value) (mapv jsonable value)
    :else value))

(defn- text
  [value]
  (if (string? value)
    value
    (with-out-str (pprint/pprint value))))

(defn- tool-result
  [value]
  {"content" [{"type" "text"
               "text" (text value)}]
   "structuredContent" (jsonable value)
   "isError" false})

(defn- tool-error-result
  [message data]
  {"content" [{"type" "text"
               "text" message}]
   "structuredContent" (jsonable {:error message
                                  :data data})
   "isError" true})

(defn- no-args-schema
  []
  {"type" "object"
   "additionalProperties" false})

(def tool-definitions
  [{"name" "factory.list_personas"
    "title" "List Agent Personas"
    "description" "List named factory personas with default roles, runtimes, and skills."
    "inputSchema" (no-args-schema)}
   {"name" "factory.get_persona"
    "title" "Get Agent Persona"
    "description" "Return one named persona so an agent can identify who it is and which skills apply."
    "inputSchema" {"type" "object"
                   "properties" {"id" {"type" "string"
                                        "description" "Persona id, for example snips."}}
                   "required" ["id"]
                   "additionalProperties" false}}
   {"name" "factory.list_skills"
    "title" "List Factory Skills"
    "description" "List repo-local and seed-local skills exposed by this factory."
    "inputSchema" (no-args-schema)}
   {"name" "factory.get_skill"
    "title" "Get Factory Skill"
    "description" "Read a known local skill file by skill id."
    "inputSchema" {"type" "object"
                   "properties" {"id" {"type" "string"
                                        "description" "Skill id, for example rama-factory-build."}}
                   "required" ["id"]
                   "additionalProperties" false}}
   {"name" "factory.queue_summary"
    "title" "Queue Summary"
    "description" "Return durable handoff queue counts by role and state."
    "inputSchema" (no-args-schema)}
   {"name" "factory.create_work"
    "title" "Create Work Handoff"
    "description" "Create a durable handoff for a role to pick up later."
    "inputSchema" {"type" "object"
                   "properties" {"from" {"type" "string"
                                          "description" "Sender role, for example specifier."}
                                 "to" {"type" "string"
                                       "description" "Recipient role, for example coder."}
                                 "task" {"type" "string"
                                         "description" "Human-readable task name."}
                                 "priority" {"type" "integer"
                                             "description" "Lower numbers are claimed first."}
                                 "persona_id" {"type" "string"
                                               "description" "Optional persona creating the work."}
                                 "payload" {"type" "object"
                                            "description" "Optional structured task payload."}}
                   "required" ["from" "to" "task"]
                   "additionalProperties" false}}
   {"name" "factory.claim_next_work"
    "title" "Claim Next Work"
    "description" "Move the next handoff for a role from new to in-process."
    "inputSchema" {"type" "object"
                   "properties" {"role" {"type" "string"
                                          "description" "Role claiming work, for example coder."}
                                 "persona_id" {"type" "string"
                                               "description" "Optional persona claiming the work."}}
                   "required" ["role"]
                   "additionalProperties" false}}
   {"name" "factory.complete_work"
    "title" "Complete Work"
    "description" "Move an in-process handoff to completed with result metadata."
    "inputSchema" {"type" "object"
                   "properties" {"role" {"type" "string"}
                                 "handoff_id" {"type" "string"}
                                 "status" {"type" "string"}
                                 "commit" {"type" "string"}
                                 "validation" {"type" "string"}
                                 "notes" {"type" "string"}
                                 "persona_id" {"type" "string"}}
                   "required" ["role" "handoff_id"]
                   "additionalProperties" false}}
   {"name" "factory.validate"
    "title" "Validate Factory"
    "description" "Run Rama Factory model validation over factory, challenge, and persona data."
    "inputSchema" (no-args-schema)}])

(defn- required-arg
  [args key]
  (let [value (get args key)]
    (when (or (nil? value)
              (and (string? value) (str/blank? value)))
      (throw (ex-info "Missing required argument."
                      {:argument key})))
    value))

(defn- ->kw
  [value]
  (cond
    (keyword? value) value
    (string? value) (keyword (str/replace value #"^:" ""))
    :else (keyword (str value))))

(defn- roles
  [ctx]
  (set (keys (get-in ctx [:factory :roles]))))

(defn- require-role!
  [ctx role-name]
  (let [role (->kw role-name)]
    (when-not (contains? (roles ctx) role)
      (throw (ex-info "Unknown factory role."
                      {:role role-name
                       :known-roles (mapv name (roles ctx))})))
    role))

(defn- persona-ref
  [ctx persona-id]
  (when persona-id
    (let [id (->kw persona-id)]
      (or (some-> (persona/persona (:personas ctx) id)
                  (select-keys [:persona/id
                                :persona/name
                                :persona/default-role
                                :persona/runtime
                                :persona/skills
                                :persona/event-tags]))
          (throw (ex-info "Unknown factory persona."
                          {:persona-id persona-id}))))))

(defn- payload-value
  [payload key]
  (or (get payload key)
      (get payload (name key))
      (get payload (str ":" (name key)))))

(defn- persona-event-fields
  [persona]
  {:persona-id (or (some-> (:persona/id persona) name) "system")
   :persona-name (or (:persona/name persona) "System")})

(defn- handoff-event-base
  [{:keys [id task payload persona]} role]
  (let [payload (or payload {})
        persona-fields (persona-event-fields persona)]
    (merge {:run-id (or (payload-value payload :run-id)
                        events/default-run-id)
            :work-id (or (payload-value payload :work-id) id)
            :role (name role)
            :phase (or (payload-value payload :phase) task "handoff")
            :artifact (or (payload-value payload :artifact) "")
            :message (or (payload-value payload :message) task "")}
           persona-fields)))

(defn- record-work-created!
  [ctx handoff]
  (events/append!
   (:state-dir ctx)
   (merge (handoff-event-base handoff (:recipient handoff))
          {:event-id (str "handoff-created-" (:id handoff))
           :event-type :handoff-created
           :status "queued"})))

(defn- record-work-claimed!
  [ctx handoff role claimed-by]
  (events/append!
   (:state-dir ctx)
   (merge (handoff-event-base (cond-> handoff
                                claimed-by (assoc :persona claimed-by))
                              role)
          {:event-id (str "handoff-accepted-" (:id handoff))
           :event-type :handoff-accepted
           :status "in-process"})))

(defn- failed-status?
  [status]
  (#{"failed" "fail" "error" "validation-failed"} (str/lower-case (str status))))

(defn- record-work-completed!
  [ctx handoff role result]
  (let [completed-by (:completed-by result)
        status (:status result)
        base (handoff-event-base (cond-> handoff
                                   completed-by (assoc :persona completed-by))
                                 role)
        completion (merge base
                          {:event-id (str "work-completed-" (:id handoff))
                           :event-type :work-completed
                           :status (or status "done")
                           :message (or (:notes result) (:message base))})
        validation (when (:validation result)
                     (merge base
                            {:event-id (str (if (failed-status? status)
                                              "validation-failed-"
                                              "validation-passed-")
                                            (:id handoff))
                             :event-type (if (failed-status? status)
                                           :validation-failed
                                           :validation-passed)
                             :status (if (failed-status? status) "failed" "passed")
                             :message (:validation result)}))]
    (events/append-many! (:state-dir ctx) (cond-> [completion]
                                            validation (conj validation)))))

(defn- ensure-handoff-state!
  [ctx]
  (handoff/ensure-role-dirs! (:state-dir ctx) (roles ctx)))

(defn- list-personas
  [ctx _args]
  {:personas (persona/summary (:personas ctx))})

(defn- get-persona
  [ctx args]
  {:persona (persona-ref ctx (required-arg args "id"))})

(defn- list-skills
  [ctx _args]
  {:skills (:skills ctx)})

(defn- get-skill
  [ctx args]
  (let [id (required-arg args "id")
        skill (some #(when (= id (:id %)) %) (:skills ctx))]
    (when-not skill
      (throw (ex-info "Unknown factory skill."
                      {:skill-id id
                       :known-skills (mapv :id (:skills ctx))})))
    (assoc skill :content (slurp (:path skill)))))

(defn- queue-summary
  [ctx _args]
  (ensure-handoff-state! ctx)
  {:state-dir (:state-dir ctx)
   :queues (handoff/queue-summary (:state-dir ctx) (roles ctx))})

(defn- parse-priority
  [value]
  (cond
    (nil? value) 50
    (integer? value) value
    (number? value) (int value)
    (string? value) (Long/parseLong value)
    :else 50))

(defn- create-work
  [ctx args]
  (let [from (require-role! ctx (required-arg args "from"))
        to (require-role! ctx (required-arg args "to"))
        persona (persona-ref ctx (get args "persona_id"))
        payload (or (get args "payload") {})]
    (ensure-handoff-state! ctx)
    (let [handoff (handoff/deliver!
                   (:state-dir ctx)
                   {:from from
                    :to to
                    :priority (parse-priority (get args "priority"))
                    :type :artifact-handoff
                    :task (required-arg args "task")
                    :persona persona
                    :payload payload})
          event (record-work-created! ctx (assoc handoff :recipient to))]
      {:handoff handoff
       :event event})))

(defn- claim-next-work
  [ctx args]
  (let [role (require-role! ctx (required-arg args "role"))
        claimed-by (persona-ref ctx (get args "persona_id"))]
    (ensure-handoff-state! ctx)
    (if-let [accepted (handoff/accept-next! (:state-dir ctx) role)]
      (let [work (cond-> accepted
                   claimed-by (assoc :claimed-by claimed-by))
            event (record-work-claimed! ctx work role claimed-by)]
        (when claimed-by
          (fio/write-edn! (:file accepted) (dissoc work :file)))
        {:work work
         :event event})
      {:work nil
       :message "NO_TASK"})))

(defn- complete-work
  [ctx args]
  (let [role (require-role! ctx (required-arg args "role"))
        completed-by (persona-ref ctx (get args "persona_id"))
        result (cond-> {:status (or (get args "status") "done")}
                 (get args "commit") (assoc :commit (get args "commit"))
                 (get args "validation") (assoc :validation (get args "validation"))
                 (get args "notes") (assoc :notes (get args "notes"))
                 completed-by (assoc :completed-by completed-by))]
    (let [completed (handoff/complete! (:state-dir ctx)
                                       role
                                       (required-arg args "handoff_id")
                                       result)
          events (record-work-completed! ctx completed role result)]
      {:work completed
       :events events})))

(defn- validate-factory
  [ctx _args]
  (let [validation (model/validate (:factory ctx)
                                   (:challenge ctx)
                                   (:personas ctx))]
    {:valid (model/valid? validation)
     :validation validation}))

(def tool-handlers
  {"factory.list_personas" list-personas
   "factory.get_persona" get-persona
   "factory.list_skills" list-skills
   "factory.get_skill" get-skill
   "factory.queue_summary" queue-summary
   "factory.create_work" create-work
   "factory.claim_next_work" claim-next-work
   "factory.complete_work" complete-work
   "factory.validate" validate-factory})

(defn call-tool
  ([name args] (call-tool (load-context) name args))
  ([ctx name args]
   (if-let [handler (get tool-handlers name)]
     (try
       (tool-result (handler ctx (or args {})))
       (catch Exception e
         (tool-error-result (.getMessage e) (ex-data e))))
     (throw (ex-info "Unknown tool."
                     {:tool name
                      :known-tools (sort (keys tool-handlers))})))))

(defn- negotiated-protocol
  [requested]
  (if (contains? supported-protocols requested)
    requested
    protocol-version))

(defn- success
  [id result]
  {"jsonrpc" "2.0"
   "id" id
   "result" result})

(defn- failure
  ([id code message] (failure id code message nil))
  ([id code message data]
   {"jsonrpc" "2.0"
    "id" id
    "error" (cond-> {"code" code
                     "message" message}
              data (assoc "data" (jsonable data)))}))

(defn handle-message
  ([message] (handle-message {} message))
  ([opts message]
   (let [id (get message "id")
         method (get message "method")
         params (get message "params")]
     (case method
       "initialize"
       (success id
                {"protocolVersion" (negotiated-protocol
                                    (get params "protocolVersion"))
                 "capabilities" {"tools" {"listChanged" false}}
                 "serverInfo" {"name" "rama-factory"
                               "title" "Rama Factory"
                               "version" "0.1.0"
                               "description" "Local MCP adapter for Rama Factory personas, skills, validation, and handoff queues."}
                 "instructions" "Use factory.get_persona first, then claim role-owned work through factory.claim_next_work."})

       "notifications/initialized"
       nil

       "ping"
       (success id {})

       "tools/list"
       (success id {"tools" tool-definitions})

       "tools/call"
       (let [name (get params "name")
             args (get params "arguments")]
         (try
           (success id (call-tool (load-context opts) name args))
           (catch Exception e
             (failure id -32602 (.getMessage e) (ex-data e)))))

       (when id
         (failure id -32601 (str "Method not found: " method)))))))

(defn encode-message
  [message]
  (str (json/write-str message) "\n"))

(defn decode-message
  [line]
  (json/read-str line))

(defn serve-stdio!
  ([] (serve-stdio! {}))
  ([opts]
   (binding [*out* *err*]
     (println "Rama Factory MCP server started"))
   (let [reader (io/reader System/in)
         writer (io/writer System/out)]
     (loop []
       (when-let [line (.readLine reader)]
         (when-not (str/blank? line)
           (let [response (handle-message opts (decode-message line))]
             (when response
               (.write writer (encode-message response))
               (.flush writer))))
         (recur))))))

(defn -main
  [& _args]
  (serve-stdio!))
