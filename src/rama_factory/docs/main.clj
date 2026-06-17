(ns rama-factory.docs.main
  (:gen-class)
  (:require [rama-factory.docs.routes :as routes]
            [rama-factory.io :as fio]
            [rama-factory.web :as web]))

(def config-path
  "apps/docs/app.edn")

(defn config
  []
  (assoc (fio/read-edn config-path)
         :routes #'routes/routes))

(defn- parse-port
  [args]
  (when-let [port (first args)]
    (Integer/parseInt port)))

(defn -main
  [& args]
  (let [port (parse-port args)
        system (web/start! (config) (cond-> {}
                                      port (assoc :port port)))]
    (println "Rama Factory docs running")
    (println "URL:" (str "http://localhost:" (or port (get-in (config) [:web :port]))))
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(web/stop! system)))
    @(promise)))
