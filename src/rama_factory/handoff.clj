(ns rama-factory.handoff
  (:require [clojure.string :as str]
            [rama-factory.io :as fio]))

(def handoff-states
  [:new :in-process :completed :outbox :sent :failed])

(defn role-dir
  [state-dir role state]
  (fio/file state-dir "handoffs" (name role) (name state)))

(defn ensure-role-dirs!
  [state-dir roles]
  (doseq [role roles
          state handoff-states]
    (fio/ensure-dir! (role-dir state-dir role state))))

(defn now
  []
  (java.time.Instant/now))

(defn timestamp
  []
  (.format java.time.format.DateTimeFormatter/ISO_INSTANT (now)))

(defn sortable-timestamp
  []
  (-> (timestamp)
      (str/replace "-" "")
      (str/replace ":" "")
      (str/replace "." "")))

(defn- sequence-file
  [state-dir role]
  (fio/file state-dir "handoffs" (name role) "sequence.edn"))

(defn next-sequence!
  [state-dir role]
  (let [path (sequence-file state-dir role)
        current (if (.exists (clojure.java.io/file path))
                  (Long/parseLong (str/trim (slurp path)))
                  0)
        next-value (inc current)]
    (fio/ensure-parent! path)
    (spit path (str next-value))
    next-value))

(defn handoff-id
  [role sequence]
  (format "%s_%06d_from_%s" (sortable-timestamp) sequence (name role)))

(defn handoff-file-name
  [{:keys [id from to priority]}]
  (format "%02d_%s_to_%s.edn"
          (or priority 50)
          id
          (->> to (map name) (str/join "_"))))

(defn deliver!
  [state-dir {:keys [from to priority type task payload] :as draft}]
  (let [recipients (if (sequential? to) to [to])
        sequence (next-sequence! state-dir from)
        id (handoff-id from sequence)
        base (merge {:id id
                     :from from
                     :to (vec recipients)
                     :priority (or priority 50)
                     :type (or type :artifact-handoff)
                     :task task
                     :created-at (timestamp)
                     :payload payload}
                    (dissoc draft :to))]
    (doseq [recipient recipients]
      (let [handoff (assoc base
                           :recipient recipient
                           :enqueued-at (timestamp))
            target (fio/file (role-dir state-dir recipient :new)
                             (handoff-file-name handoff))]
        (fio/write-edn! target handoff)))
    (fio/write-edn! (fio/file (role-dir state-dir from :sent)
                              (handoff-file-name base))
                    base)
    base))

(defn- read-handoff-file
  [file]
  (assoc (fio/read-edn file) :file (.getPath file)))

(defn queue
  [state-dir role state]
  (->> (fio/list-files (role-dir state-dir role state))
       (map read-handoff-file)
       vec))

(defn queue-summary
  [state-dir roles]
  (into {}
        (for [role roles]
          [role (into {}
                      (for [state [:new :in-process :completed :sent :failed]]
                        [state (count (queue state-dir role state))]))])))

(defn accept-next!
  [state-dir role]
  (when-let [file (first (fio/list-files (role-dir state-dir role :new)))]
    (let [handoff (-> (fio/read-edn file)
                      (assoc :accepted-at (timestamp)))
          target (fio/file (role-dir state-dir role :in-process)
                           (.getName file))]
      (fio/write-edn! file handoff)
      (fio/move! file target)
      (assoc handoff :file (.getPath (clojure.java.io/file target))))))

(defn- matching-handoff-file
  [state-dir role id state]
  (some (fn [file]
          (when (= id (:id (fio/read-edn file)))
            file))
        (fio/list-files (role-dir state-dir role state))))

(defn complete!
  [state-dir role id result]
  (if-let [file (matching-handoff-file state-dir role id :in-process)]
    (let [handoff (-> (fio/read-edn file)
                      (assoc :completed-at (timestamp)
                             :result result))
          target (fio/file (role-dir state-dir role :completed)
                           (.getName file))]
      (fio/write-edn! file handoff)
      (fio/move! file target)
      (assoc handoff :file (.getPath (clojure.java.io/file target))))
    (throw (ex-info "No in-process handoff found."
                    {:role role :id id :state-dir state-dir}))))

