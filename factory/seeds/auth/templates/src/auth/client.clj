(ns {{app_ns}}.auth.client
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [{{app_ns}}.auth.module :as module]))

(defn module-name
  []
  (get-module-name module/AuthModule))

(defn clients
  [cluster-manager]
  (let [module-name (module-name)]
    {:registration-depot (foreign-depot cluster-manager module-name "*registration-depot")
     :session-depot (foreign-depot cluster-manager module-name "*session-depot")
     :session-revocation-depot (foreign-depot cluster-manager module-name "*session-revocation-depot")
     :sessions (foreign-pstate cluster-manager module-name "$$sessions")
     :sessions-by-user (foreign-pstate cluster-manager module-name "$$sessions-by-user")
     :login-audit (foreign-pstate cluster-manager module-name "$$login-audit")
     :find-user-by-email (foreign-query cluster-manager module-name "find-user-by-email")
     :get-current-user (foreign-query cluster-manager module-name "get-current-user")
     :list-user-sessions (foreign-query cluster-manager module-name "list-user-sessions")}))

(defn now-ms
  []
  (System/currentTimeMillis))

(defn uuid
  []
  (str (java.util.UUID/randomUUID)))

(declare find-user-by-email session)

(defn register!
  [clients {:keys [event-id user-id email password-hash occurred-at]
            :or {occurred-at (now-ms)}}]
  (let [event-id (or event-id (uuid))
        user-id (or user-id (uuid))
        {acked-user-id "auth"} (foreign-append!
                                (:registration-depot clients)
                                (module/->Registration event-id user-id email password-hash occurred-at))]
    (when-not acked-user-id
      (throw (ex-info "Email is already registered."
                      {:email email})))
    (find-user-by-email clients email)))

(defn create-session!
  [clients {:keys [event-id session-id user-id occurred-at expires-at]
            :or {occurred-at (now-ms)}}]
  (let [event-id (or event-id (uuid))
        session-id (or session-id (uuid))
        expires-at (or expires-at (+ occurred-at (* 1000 60 60 24 30)))
        {acked-session-id "auth"} (foreign-append!
                                   (:session-depot clients)
                                   (module/->SessionCreated event-id
                                                            session-id
                                                            user-id
                                                            occurred-at
                                                            expires-at))]
    (when-not acked-session-id
      (throw (ex-info "Session was not created."
                      {:user-id user-id :session-id session-id})))
    (assoc (session clients session-id) :session-id session-id)))

(defn revoke-session!
  [clients {:keys [event-id session-id occurred-at]
            :or {occurred-at (now-ms)}}]
  (let [event-id (or event-id (uuid))]
    (foreign-append!
     (:session-revocation-depot clients)
     (module/->SessionRevoked event-id session-id occurred-at))
    (some-> (session clients session-id)
            (assoc :session-id session-id))))

(defn find-user-by-email
  [clients email]
  (foreign-invoke-query (:find-user-by-email clients) email))

(defn current-user
  [clients session-id]
  (foreign-invoke-query (:get-current-user clients) session-id))

(defn list-user-sessions
  [clients user-id]
  (into {} (foreign-select [(keypath user-id) ALL] (:sessions-by-user clients))))

(defn session
  [clients session-id]
  (foreign-select-one (keypath session-id) (:sessions clients)))

(defn login-audit
  [clients user-id]
  (into {} (foreign-select [(keypath user-id) ALL] (:login-audit clients))))
