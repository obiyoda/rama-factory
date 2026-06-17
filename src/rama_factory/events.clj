(ns rama-factory.events
  (:require [clojure.string :as str]
            [rama-factory.io :as fio]))

(def default-run-id "mcp-local")

(defn now-ms
  []
  (.toEpochMilli (java.time.Instant/now)))

(defn event-dir
  [state-dir]
  (fio/file state-dir "events"))

(defn- value-at
  [m k]
  (or (get m k)
      (get m (name k))
      (get m (str ":" (name k)))))

(defn- safe-id
  [event-id]
  (-> (str event-id)
      (str/replace #"[^A-Za-z0-9._-]+" "_")
      (str/replace #"(^_+|_+$)" "")))

(defn normalize-event
  [event]
  (let [occurred-at (or (value-at event :occurred-at) (now-ms))
        event-type (or (value-at event :event-type) :factory-event)
        run-id (or (value-at event :run-id) default-run-id)
        work-id (or (value-at event :work-id) run-id)
        role (or (value-at event :role) "system")
        persona-id (or (value-at event :persona-id) "system")
        persona-name (or (value-at event :persona-name) "System")
        phase (or (value-at event :phase) "factory")
        artifact (or (value-at event :artifact) "")
        status (or (value-at event :status) "recorded")
        message (or (value-at event :message) "")
        event-id (or (value-at event :event-id)
                     (str (name (keyword event-type)) "-" work-id "-" occurred-at))]
    {:event-id (str event-id)
     :event-type (keyword event-type)
     :run-id (str run-id)
     :work-id (str work-id)
     :role (str role)
     :persona-id (str persona-id)
     :persona-name (str persona-name)
     :phase (str phase)
     :artifact (str artifact)
     :status (str status)
     :message (str message)
     :occurred-at (long occurred-at)}))

(defn event-file
  [state-dir event-id]
  (fio/file (event-dir state-dir) (str (safe-id event-id) ".edn")))

(defn append!
  [state-dir event]
  (let [{:keys [event-id] :as normalized} (normalize-event event)
        target (event-file state-dir event-id)]
    (if (.exists (clojure.java.io/file target))
      (assoc (fio/read-edn target) :file (.getPath (clojure.java.io/file target)))
      (do
        (fio/write-edn! target normalized)
        (assoc normalized :file (.getPath (clojure.java.io/file target)))))))

(defn append-many!
  [state-dir events]
  (mapv #(append! state-dir %) events))

(defn list-events
  [state-dir]
  (->> (fio/list-files (event-dir state-dir))
       (map fio/read-edn)
       (map normalize-event)
       (sort-by (juxt :occurred-at :event-id))
       vec))
