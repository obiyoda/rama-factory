(ns rama-factory.web
  (:require [rama-factory.app :as app]
            [zodiac.core :as zodiac]))

(defn html
  ([doc]
   (zodiac/html-response doc))
  ([status doc]
   (zodiac/html-response status doc)))

(defn json
  ([data]
   (zodiac/json-response data))
  ([status data]
   (zodiac/json-response status data)))

(defn redirect
  ([location]
   (redirect 303 location))
  ([status location]
   {:status status
    :headers {"Location" location}
    :body ""}))

(defn url-for
  ([name-or-path]
   (zodiac/url-for name-or-path))
  ([name-or-path args]
   (zodiac/url-for name-or-path args))
  ([name-or-path args query-params]
   (zodiac/url-for name-or-path args query-params)))

(defn- extension-fns
  [extensions]
  (->> extensions
       (keep :zodiac/extension)
       vec))

(defn zodiac-options
  [config overrides]
  (let [{:keys [routes request-context extensions web] :as application}
        (app/application config)
        validation (app/validate application)]
    (when-not (app/valid? validation)
      (throw (ex-info "Invalid Rama Factory web application."
                      {:validation validation})))
    (merge
     {:routes routes
      :extensions (extension-fns extensions)
      :request-context (merge {:rama-factory/application
                               (select-keys application
                                            [:app/id :app/title :app/description :features])}
                              request-context)
      :cookie-secret (:cookie-secret web)
      :cookie-name (:cookie-name web)
      :port (:port web)
      :reload-per-request? (:reload-per-request? web)}
     overrides)))

(defn start!
  ([config]
   (start! config {}))
  ([config overrides]
   (zodiac/start (zodiac-options config overrides))))

(defn stop!
  [system]
  (zodiac/stop system))
