(ns {{app_ns}}.factory-dashboard.views)

(defn- escape-html
  [s]
  (str (or s "")))

(defn- event-row
  [[event-id event]]
  (str "<tr>"
       "<td>" event-id "</td>"
       "<td>" (escape-html (:event-type event)) "</td>"
       "<td>" (escape-html (:persona-name event)) "</td>"
       "<td>" (escape-html (:role event)) "</td>"
       "<td>" (escape-html (:phase event)) "</td>"
       "<td>" (escape-html (:status event)) "</td>"
       "<td>" (escape-html (:message event)) "</td>"
       "</tr>"))

(defn dashboard
  [{:keys [run timeline counts]}]
  (let [timeline (or timeline [])
        counts (or counts {})]
    (str "<main class=\"factory-dashboard\">"
         "<h1>Factory Floor</h1>"
         "<p>Rama-backed view of agent runs, handoffs, artifacts, and validation gates.</p>"
         "<section><h2>Current run</h2>"
         "<dl>"
         "<dt>Run</dt><dd>" (escape-html (:run-id run)) "</dd>"
         "<dt>Status</dt><dd>" (escape-html (:status run)) "</dd>"
         "<dt>Message</dt><dd>" (escape-html (:message run)) "</dd>"
         "</dl></section>"
         "<section><h2>Event counts</h2><ul>"
         (apply str (for [[k v] counts]
                      (str "<li>" (name k) ": " v "</li>")))
         "</ul></section>"
         "<section><h2>Timeline</h2><table>"
         "<thead><tr><th>ID</th><th>Event</th><th>Persona</th><th>Role</th><th>Phase</th><th>Status</th><th>Message</th></tr></thead>"
         "<tbody>" (apply str (map event-row timeline)) "</tbody>"
         "</table></section>"
         "</main>")))
