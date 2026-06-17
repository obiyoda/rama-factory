(ns rama-factory.app)

(def default-web
  {:runtime :zodiac
   :port 3000
   :cookie-name "rama-factory-session"
   :cookie-secret "rama-factory-dev"
   :reload-per-request? true})

(def required-keys
  #{:app/id :app/title :routes})

(defn application
  [config]
  (-> config
      (update :features #(set (or % [])))
      (update :extensions #(vec (or % [])))
      (update :web #(merge default-web (or % {})))))

(defn- problem
  [check message data]
  {:check check :message message :data data})

(defn validate
  [config]
  (let [app (application config)
        missing (remove #(contains? app %) required-keys)
        web-runtime (get-in app [:web :runtime])
        cookie-secret (get-in app [:web :cookie-secret])]
    (cond-> []
      (seq missing)
      (conj (problem :app-shape
                     "Application config is missing required keys."
                     {:missing (set missing)}))

      (not= :zodiac web-runtime)
      (conj (problem :web-runtime
                     "The default Rama Factory web runtime is Zodiac."
                     {:runtime web-runtime}))

      (not (or (nil? cookie-secret)
               (= 16 (count cookie-secret))))
      (conj (problem :cookie-secret
                     "Zodiac cookie secrets must be exactly 16 characters."
                     {:length (count cookie-secret)})))))

(defn valid?
  [validation]
  (empty? validation))
