(ns rama-factory.docs.views
  (:require [clojure.string :as str]
            [rama-factory.docs.content :as content]
            [rama-factory.web :as web]))

(def css
  (str
   ":root{--ink:#18211d;--muted:#59645f;--line:#d8ded8;--paper:#fbfcfa;"
   "--panel:#ffffff;--green:#2f7d57;--blue:#315d9c;--amber:#9f6b1d;--red:#b94336;}"
   "*{box-sizing:border-box}body{margin:0;background:var(--paper);color:var(--ink);"
   "font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
   "letter-spacing:0;line-height:1.55}a{color:inherit}.shell{min-height:100vh;display:grid;"
   "grid-template-columns:280px minmax(0,1fr)}.sidebar{position:sticky;top:0;height:100vh;"
   "border-right:1px solid var(--line);background:#f4f7f2;padding:28px 20px;overflow:auto}"
   ".brand{display:flex;align-items:center;gap:12px;text-decoration:none;margin-bottom:28px}"
   ".mark{width:36px;height:36px;border-radius:8px;background:var(--green);display:grid;"
   "place-items:center;color:white;font-weight:800}.brand-copy strong{display:block;font-size:16px}"
   ".brand-copy span{display:block;color:var(--muted);font-size:12px}.nav-label{margin:26px 0 8px;"
   "font-size:11px;text-transform:uppercase;color:var(--muted);font-weight:750}"
   ".nav{display:grid;gap:4px}.nav a{display:block;text-decoration:none;border-radius:8px;"
   "padding:9px 10px;color:#26302b;font-size:14px}.nav a:hover{background:#e8eee7}"
   ".nav a.active{background:#dce9df;color:#17432e;font-weight:750}.main{min-width:0}"
   ".topbar{height:64px;border-bottom:1px solid var(--line);display:flex;align-items:center;"
   "justify-content:space-between;gap:18px;padding:0 40px;background:rgba(251,252,250,.92);"
   "position:sticky;top:0;z-index:2;backdrop-filter:blur(12px)}.topbar-title{font-weight:750}"
   ".topbar-links{display:flex;gap:10px;flex-wrap:wrap}.chip{border:1px solid var(--line);"
   "border-radius:8px;padding:6px 9px;font-size:12px;text-decoration:none;background:#fff}"
   ".content{max-width:1120px;margin:0 auto;padding:42px 40px 64px}.hero{display:grid;"
   "grid-template-columns:minmax(0,1.05fr) minmax(320px,.95fr);gap:32px;align-items:center;"
   "padding:22px 0 36px;border-bottom:1px solid var(--line)}.kicker{font-size:12px;"
   "text-transform:uppercase;color:var(--green);font-weight:800;margin:0 0 12px}"
   "h1{font-size:clamp(34px,5vw,64px);line-height:1.02;margin:0 0 18px;max-width:860px}"
   ".lede{font-size:19px;color:var(--muted);max-width:740px;margin:0}.actions{display:flex;"
   "gap:10px;flex-wrap:wrap;margin-top:28px}.button{display:inline-flex;align-items:center;"
   "justify-content:center;min-height:38px;border-radius:8px;padding:8px 12px;text-decoration:none;"
   "font-weight:750;font-size:14px}.button.primary{background:var(--ink);color:white}"
   ".button.secondary{border:1px solid var(--line);background:white}.system-map{border:1px solid var(--line);"
   "background:#fff;border-radius:8px;padding:18px;display:grid;gap:12px}.map-row{display:grid;"
   "grid-template-columns:110px minmax(0,1fr);gap:12px;align-items:center}.map-label{font-size:12px;"
   "font-weight:800;color:var(--muted);text-transform:uppercase}.map-nodes{display:flex;gap:8px;"
   "flex-wrap:wrap}.node{border:1px solid var(--line);border-left-width:5px;border-radius:8px;"
   "padding:9px 10px;background:#fbfcfa;font-size:13px;font-weight:700}.node.green{border-left-color:var(--green)}"
   ".node.blue{border-left-color:var(--blue)}.node.amber{border-left-color:var(--amber)}"
   ".node.red{border-left-color:var(--red)}.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));"
   "gap:14px;margin-top:28px}.card{border:1px solid var(--line);background:var(--panel);"
   "border-radius:8px;padding:18px;text-decoration:none;min-height:178px;display:flex;flex-direction:column;"
   "justify-content:space-between}.card:hover{border-color:#afbbb1}.card small{color:var(--green);"
   "font-weight:800;text-transform:uppercase}.card h2{font-size:18px;margin:10px 0 8px}.card p{margin:0;color:var(--muted);"
   "font-size:14px}.doc-header{padding:12px 0 28px;border-bottom:1px solid var(--line)}"
   ".doc-header h1{font-size:clamp(32px,4.5vw,54px)}.doc-layout{display:grid;grid-template-columns:minmax(0,1fr) 250px;"
   "gap:42px}.doc-sections{min-width:0}.doc-section{padding:30px 0;border-bottom:1px solid var(--line)}"
   ".doc-section h2{font-size:24px;margin:0 0 12px}.doc-section p{color:var(--muted);margin:0 0 12px;max-width:780px}"
   ".doc-section ul{margin:0;padding-left:20px;color:var(--muted);max-width:780px}.doc-section li{margin:7px 0}"
   ".toc{position:sticky;top:92px;align-self:start;border-left:3px solid var(--line);padding-left:14px;color:var(--muted);"
   "font-size:13px}.toc a{display:block;text-decoration:none;margin:8px 0}.code{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;"
   "font-size:.95em;background:#eef3ee;border:1px solid #dbe4dc;border-radius:6px;padding:1px 5px}"
   ".api-note{margin-top:34px;border:1px solid var(--line);border-left:5px solid var(--blue);border-radius:8px;"
   "background:white;padding:16px;color:var(--muted)}.status{display:inline-flex;border-radius:8px;"
   "border:1px solid var(--line);padding:4px 8px;font-size:12px;font-weight:750;background:white;color:var(--muted)}"
   "@media(max-width:900px){.shell{grid-template-columns:1fr}.sidebar{position:relative;height:auto;border-right:0;"
   "border-bottom:1px solid var(--line)}.topbar{position:relative;padding:0 22px;height:auto;min-height:58px;"
   "align-items:flex-start;flex-direction:column;padding-top:14px;padding-bottom:14px}.content{padding:30px 22px 46px}"
   ".hero{grid-template-columns:1fr}.grid{grid-template-columns:1fr}.doc-layout{grid-template-columns:1fr}.toc{position:relative;top:0}}"
   "@media(max-width:520px){.map-row{grid-template-columns:1fr}.actions{display:grid}.button{width:100%}}"))

(defn doc-path
  [id]
  (if (= :vision id)
    "/"
    (web/url-for :doc {:id (name id)})))

(defn api-path
  [id]
  (web/url-for :api-doc {:id (name id)}))

(defn nav
  [active-id]
  [:div {:class "sidebar-inner"}
   [:a {:class "brand" :href "/"}
    [:span {:class "mark"} "RF"]
    [:span {:class "brand-copy"}
     [:strong "Rama Factory"]
     [:span "Framework docs"]]]
   [:div {:class "nav-label"} "Guide"]
   [:nav {:class "nav" :aria-label "Documentation"}
    (for [{:keys [id title]} content/pages]
      [:a {:href (doc-path id)
           :class (when (= active-id id) "active")}
       title])]
   [:div {:class "nav-label"} "Runtime"]
   [:nav {:class "nav" :aria-label "Runtime links"}
    [:a {:href "/api/docs"} "JSON index"]
    [:a {:href "/api/docs/zodiac"} "Zodiac API page"]]])

(defn layout
  [{:keys [active-id title]} body]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title (str title " | Rama Factory")]
    [:style css]]
   [:body
    [:div {:class "shell"}
     [:aside {:class "sidebar"} (nav active-id)]
     [:main {:class "main"}
      [:header {:class "topbar"}
       [:div {:class "topbar-title"} "Dogfooded docs app"]
       [:div {:class "topbar-links"}
        [:span {:class "status"} "Zodiac runtime"]
        [:span {:class "status"} "Hypermedia"]
        [:span {:class "status"} "JSON API"]]]
      [:div {:class "content"} body]]]]])

(defn system-map
  []
  [:div {:class "system-map" :aria-label "Rama Factory system map"}
   [:div {:class "map-row"}
    [:div {:class "map-label"} "Edge"]
    [:div {:class "map-nodes"}
     [:span {:class "node green"} "Hypermedia"]
     [:span {:class "node blue"} "SPA API"]
     [:span {:class "node amber"} "Webhooks"]]]
   [:div {:class "map-row"}
    [:div {:class "map-label"} "Framework"]
    [:div {:class "map-nodes"}
     [:span {:class "node green"} "CLI"]
     [:span {:class "node blue"} "Generators"]
     [:span {:class "node amber"} "Extensions"]
     [:span {:class "node red"} "Validation"]]]
   [:div {:class "map-row"}
    [:div {:class "map-label"} "Runtime"]
    [:div {:class "map-nodes"}
     [:span {:class "node green"} "Zodiac"]
     [:span {:class "node blue"} "Ring"]
     [:span {:class "node amber"} "Reitit"]]]
   [:div {:class "map-row"}
    [:div {:class "map-label"} "State"]
    [:div {:class "map-nodes"}
     [:span {:class "node green"} "Depots"]
     [:span {:class "node blue"} "PStates"]
     [:span {:class "node amber"} "Queries"]
     [:span {:class "node red"} "Topologies"]]]])

(defn home-page
  []
  (let [page (content/page :vision)]
    (layout
     {:active-id :vision :title (:title page)}
     [:div
      [:section {:class "hero"}
       [:div
        [:p {:class "kicker"} (:kicker page)]
        [:h1 "Laravel-style DX for Rama applications"]
        [:p {:class "lede"} (:summary page)]
        [:div {:class "actions"}
         [:a {:class "button primary" :href (doc-path :architecture)} "Read architecture"]
         [:a {:class "button secondary" :href (doc-path :auth)} "Auth extension"]
         [:a {:class "button secondary" :href "/api/docs"} "JSON index"]]]
       (system-map)]
      [:section {:class "grid" :aria-label "Documentation sections"}
       (for [{:keys [id title kicker summary]} (rest content/pages)]
         [:a {:class "card" :href (doc-path id)}
          [:div
           [:small kicker]
           [:h2 title]
           [:p summary]]
          [:span {:class "status"} "Open"]])]])))

(defn slug
  [heading]
  (-> heading
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"(^-|-$)" "")))

(defn section-view
  [{:keys [heading body bullets]}]
  [:section {:class "doc-section" :id (slug heading)}
   [:h2 heading]
   (for [paragraph body]
     [:p paragraph])
   (when (seq bullets)
     [:ul
      (for [item bullets]
        [:li item])])])

(defn doc-page
  [{:keys [id title kicker summary sections] :as page}]
  (layout
   {:active-id id :title title}
   [:div
    [:header {:class "doc-header"}
     [:p {:class "kicker"} kicker]
     [:h1 title]
     [:p {:class "lede"} summary]]
    [:div {:class "doc-layout"}
     [:div {:class "doc-sections"}
      (for [section sections]
        (section-view section))
      [:div {:class "api-note"}
       "This page is also available as JSON at "
       [:span {:class "code"} (api-path id)]
       "."]]
     [:aside {:class "toc" :aria-label "Page sections"}
      [:strong "On this page"]
      (for [{:keys [heading]} sections]
        [:a {:href (str "#" (slug heading))} heading])]]]))

(defn not-found-page
  [id]
  (layout
   {:active-id nil :title "Not found"}
   [:section {:class "doc-header"}
    [:p {:class "kicker"} "Missing Page"]
    [:h1 "No docs page found"]
    [:p {:class "lede"} "The docs app could not find a page named "
     [:span {:class "code"} (name id)]
     "."]
    [:div {:class "actions"}
     [:a {:class "button primary" :href "/"} "Back to docs"]]]))
