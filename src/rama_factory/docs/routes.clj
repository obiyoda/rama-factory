(ns rama-factory.docs.routes
  (:require [rama-factory.docs.content :as content]
            [rama-factory.docs.views :as views]
            [rama-factory.web :as web]))

(defn- page-id
  [raw]
  (keyword raw))

(defn- page-response
  [id]
  (if-let [page (content/page id)]
    (views/doc-page page)
    (web/html 404 (views/not-found-page id))))

(defn- api-page-response
  [id]
  (if-let [page (content/page id)]
    (web/json page)
    (web/json 404 {:error :not-found
                   :message "No docs page found."
                   :id id})))

(defn routes
  []
  [""
   ["/" {:name :home
         :get (fn [_] (views/home-page))}]
   ["/docs/:id" {:name :doc
                 :get (fn [{:keys [path-params]}]
                        (page-response (page-id (:id path-params))))}]
   ["/api/docs" {:name :api-docs
                 :zodiac/skip-csrf true
                 :get (fn [_]
                        (web/json {:app :rama-factory-docs
                                   :pages (content/page-summaries)}))}]
   ["/api/docs/:id" {:name :api-doc
                     :zodiac/skip-csrf true
                     :get (fn [{:keys [path-params]}]
                            (api-page-response (page-id (:id path-params))))}]])
