(ns {{app_ns}}.auth.routes
  (:require [{{app_ns}}.auth.client :as auth]
            [{{app_ns}}.auth.views :as views]))

(defn- html
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn- json
  ([data]
   (json 200 data))
  ([status data]
   {:status status
    :headers {"Content-Type" "application/edn; charset=utf-8"}
    :body (pr-str data)}))

(defn- redirect
  [location session]
  {:status 303
   :headers {"Location" location}
   :session session
   :body ""})

(defn- params
  [request]
  (merge (:params request) (:form-params request)))

(defn demo-password-hash
  [password]
  ;; Replace this with a real password hashing strategy before production use.
  (str "demo$" password))

(defn login-page
  [_request]
  (html (views/login-page)))

(defn register-page
  [_request]
  (html (views/register-page)))

(defn register-submit
  [{:auth/keys [clients] :keys [session] :as request}]
  (let [{:strs [email password]} (params request)
        user (auth/register! clients {:email email
                                      :password-hash (demo-password-hash password)})]
    (redirect "/"
              (assoc session :auth/session-id
                     (:session-id (auth/create-session!
                                   clients
                                   {:user-id (:user-id user)}))))))

(defn login-submit
  [{:auth/keys [clients] :keys [session] :as request}]
  (let [{:strs [email password]} (params request)
        user (auth/find-user-by-email clients email)]
    (if (= (:password-hash user) (demo-password-hash password))
      (redirect "/"
                (assoc session :auth/session-id
                       (:session-id (auth/create-session!
                                     clients
                                     {:user-id (:user-id user)}))))
      (json 401 {:error :invalid-credentials}))))

(defn logout-submit
  [{:auth/keys [clients] :keys [session]}]
  (when-let [session-id (:auth/session-id session)]
    (auth/revoke-session! clients {:session-id session-id}))
  (redirect "/" (dissoc session :auth/session-id)))

(defn api-current-user
  [{:auth/keys [clients] :keys [session]}]
  (if-let [session-id (:auth/session-id session)]
    (if-let [user (auth/current-user clients session-id)]
      (json {:user (dissoc user :password-hash)})
      (json 401 {:error :unauthenticated}))
    (json 401 {:error :unauthenticated})))

(def routes
  [["/login" {:get login-page
              :post login-submit}]
   ["/register" {:get register-page
                 :post register-submit}]
   ["/logout" {:post logout-submit}]
   ["/api/auth/me" {:get api-current-user}]])
