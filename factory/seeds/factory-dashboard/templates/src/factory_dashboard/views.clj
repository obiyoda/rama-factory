(ns {{app_ns}}.factory-dashboard.views
  (:require [clojure.string :as str]))

(def default-vite-origin
  "http://localhost:5173")

(defn- vite-origin
  []
  (or (System/getenv "RAMA_FACTORY_VITE_ORIGIN")
      default-vite-origin))

(defn- asset-tags
  []
  (let [origin (vite-origin)]
    (str "<script type=\"module\" src=\"" origin "/@vite/client\"></script>"
         "<script type=\"module\" src=\"" origin "/assets/app.js\"></script>")))

(defn- escape-html
  [s]
  (-> (str (or s ""))
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#x27;")))

(defn- badge-class
  [value]
  (case (str/lower-case (str value))
    ("live" "passed" "done" "completed") "badge"
    ("demo" "queued" "in-process" "written") "badge-secondary"
    ("failed" "error" "validation-failed") "badge-destructive"
    "badge-outline"))

(defn- badge
  [value]
  (str "<span class=\"" (badge-class value) "\">"
       (escape-html value)
       "</span>"))

(defn- stat-card
  [label value]
  (str "<article class=\"card\">"
       "<header><p class=\"text-sm text-muted-foreground\">" (escape-html label) "</p>"
       "<h2 class=\"font-mono tabular-nums\">" (escape-html value) "</h2></header>"
       "</article>"))

(defn- event-row
  [[event-id event]]
  (str "<tr>"
       "<td class=\"font-mono text-xs\">" (escape-html event-id) "</td>"
       "<td>" (badge (:event-type event)) "</td>"
       "<td class=\"font-mono text-xs\">" (escape-html (:project-id event)) "</td>"
       "<td class=\"font-medium\">" (escape-html (:persona-name event)) "</td>"
       "<td>" (escape-html (:role event)) "</td>"
       "<td>" (escape-html (:phase event)) "</td>"
       "<td>" (badge (:status event)) "</td>"
       "<td>" (escape-html (:message event)) "</td>"
       "</tr>"))

(defn dashboard
  [{:keys [run timeline counts source observed-events ingested-events]}]
  (let [timeline (or timeline [])
        counts (or counts {})]
    (str "<!doctype html><html><head>"
         "<meta charset=\"utf-8\">"
         "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
         "<meta http-equiv=\"refresh\" content=\"3\">"
         "<title>Factory Floor</title>"
         (asset-tags)
         "</head><body class=\"min-h-screen bg-background text-foreground\">"
         "<main class=\"factory-dashboard mx-auto flex max-w-7xl flex-col gap-6 p-6\">"
         "<header class=\"flex flex-col gap-4 border-b pb-5 md:flex-row md:items-end md:justify-between\">"
         "<div><p class=\"text-sm font-medium text-muted-foreground\">Rama Factory</p>"
         "<h1 class=\"text-3xl font-semibold tracking-tight\">Factory Floor</h1>"
         "<p class=\"mt-2 max-w-3xl text-sm text-muted-foreground\">Live agent work flowing through Rama.</p></div>"
         "<div class=\"flex flex-wrap gap-2\">"
         (badge (name (or source :unknown)))
         "<a class=\"btn-outline\" href=\"/api/factory/snapshot\">Snapshot</a>"
         "</div></header>"
         "<section class=\"grid gap-4 md:grid-cols-3\">"
         (stat-card "Observed events" (or observed-events 0))
         (stat-card "Newly ingested" (or ingested-events 0))
         (stat-card "Timeline rows" (count timeline))
         "</section>"
         "<section class=\"card\">"
         "<header><h2>Current run</h2><p>"
         (escape-html (:message run))
         "</p></header>"
         "<section class=\"grid gap-4 text-sm md:grid-cols-3\">"
         "<div><p class=\"text-muted-foreground\">Run</p><p class=\"font-mono break-all\">"
         (escape-html (:run-id run)) "</p></div>"
         "<div><p class=\"text-muted-foreground\">Status</p><p>" (badge (:status run)) "</p></div>"
         "<div><p class=\"text-muted-foreground\">Source</p><p>" (badge (name (or source :unknown))) "</p></div>"
         "</section></section>"
         "<section class=\"grid gap-4 md:grid-cols-4\">"
         (apply str (for [[k v] counts]
                      (stat-card (name k) v)))
         "</section>"
         "<section class=\"card\">"
         "<header><h2>Timeline</h2><p>Named agent activity ordered by occurrence.</p></header>"
         "<section class=\"overflow-x-auto px-0\"><table class=\"table\">"
         "<thead><tr><th>ID</th><th>Event</th><th>Project</th><th>Persona</th><th>Role</th><th>Phase</th><th>Status</th><th>Message</th></tr></thead>"
         "<tbody>" (apply str (map event-row timeline)) "</tbody>"
         "</table></section></section>"
         "</main></body></html>")))
