(ns {{app_ns}}.factory-dashboard.routes
  (:require [{{app_ns}}.factory-dashboard.client :as dashboard]
            [{{app_ns}}.factory-dashboard.views :as views]))

(defn- html
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn- edn-response
  [data]
  {:status 200
   :headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str data)})

(defn demo-snapshot
  []
  {:run {:run-id "demo-auth-run"
         :status "passed"
         :message "InProcessCluster tests passed."}
   :timeline (mapv (fn [{:keys [event-id] :as event}]
                     [event-id
                      (-> event
                          (update :event-type name)
                          (dissoc :event-id :run-id :work-id :artifact :occurred-at))])
                   dashboard/demo-events)
   :counts {:run-created 1
            :handoff-created 1
            :handoff-accepted 1
            :artifact-written 1
            :validation-passed 1}})

(defn snapshot
  [request]
  (if-let [clients (:factory-dashboard/clients request)]
    (dashboard/snapshot clients (or (get-in request [:params "run-id"])
                                    "demo-auth-run"))
    (demo-snapshot)))

(defn dashboard-page
  [request]
  (html (views/dashboard (snapshot request))))

(defn api-snapshot
  [request]
  (edn-response (snapshot request)))

(def routes
  [["/factory" {:get dashboard-page}]
   ["/api/factory/snapshot" {:get api-snapshot}]])
