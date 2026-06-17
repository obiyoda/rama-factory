(ns {{app_ns}}.auth.module
  (:use [com.rpl.rama]
        [com.rpl.rama.path]))

(defrecord Registration [event-id user-id email password-hash occurred-at])
(defrecord SessionCreated [event-id session-id user-id occurred-at expires-at])
(defrecord SessionRevoked [event-id session-id occurred-at])

(defn present?
  [x]
  (not (nil? x)))

(defmodule AuthModule
  [setup topologies]
  (declare-depot setup *registration-depot (hash-by :email))
  (declare-depot setup *session-depot (hash-by :session-id))
  (declare-depot setup *session-revocation-depot (hash-by :session-id))

  (let [s (stream-topology topologies "auth")]
    (declare-pstate s $$users
                    {String
                     (fixed-keys-schema {:user-id String
                                         :email String
                                         :password-hash String
                                         :created-at Long})})
    (declare-pstate s $$users-by-email
                    {String
                     (fixed-keys-schema {:user-id String
                                         :event-id String})})
    (declare-pstate s $$sessions
                    {String
                     (fixed-keys-schema {:user-id String
                                         :active? Boolean
                                         :created-at Long
                                         :expires-at Long
                                         :revoked-at Long})})
    (declare-pstate s $$sessions-by-user
                    {String
                     (map-schema String
                                 (fixed-keys-schema {:active? Boolean
                                                     :created-at Long
                                                     :expires-at Long
                                                     :revoked-at Long})
                                 {:subindex? true})})
    (declare-pstate s $$login-audit
                    {String
                     (map-schema String
                                 (fixed-keys-schema {:type clojure.lang.Keyword
                                                     :session-id String
                                                     :occurred-at Long})
                                 {:subindex? true})})

    (<<sources s
      (source> *registration-depot :> {:keys [*event-id *user-id *email *password-hash *occurred-at]})
      (local-select> (keypath *email) $$users-by-email :> {*current-event-id :event-id
                                                           :as *current-registration})
      (<<if (or> (nil? *current-registration)
                 (= *current-event-id *event-id))
        (local-transform> [(keypath *email)
                           (multi-path [:user-id (termval *user-id)]
                                       [:event-id (termval *event-id)])]
                          $$users-by-email)
        (|hash *user-id)
        (local-transform> [(keypath *user-id)
                           (multi-path [:user-id (termval *user-id)]
                                       [:email (termval *email)]
                                       [:password-hash (termval *password-hash)]
                                       [:created-at (termval *occurred-at)])]
                          $$users)
        (local-transform> [(keypath *user-id *event-id)
                           (termval {:type :registered
                                     :occurred-at *occurred-at})]
                          $$login-audit)
        (ack-return> *user-id))

      (source> *session-depot :> {:keys [*event-id *session-id *user-id *occurred-at *expires-at]})
      (local-transform> [(keypath *session-id)
                         (multi-path [:user-id (termval *user-id)]
                                     [:active? (termval true)]
                                     [:created-at (termval *occurred-at)]
                                     [:expires-at (termval *expires-at)])]
                        $$sessions)
      (|hash *user-id)
      (local-transform> [(keypath *user-id *session-id)
                         (multi-path [:active? (termval true)]
                                     [:created-at (termval *occurred-at)]
                                     [:expires-at (termval *expires-at)])]
                        $$sessions-by-user)
      (local-transform> [(keypath *user-id *event-id)
                         (termval {:type :session-created
                                   :session-id *session-id
                                   :occurred-at *occurred-at})]
                        $$login-audit)
      (ack-return> *session-id)

      (source> *session-revocation-depot :> {:keys [*event-id *session-id *occurred-at]})
      (local-select> (keypath *session-id) $$sessions :> {*user-id :user-id
                                                          :as *session})
      (<<if (nil? *session)
        (ack-return> nil)
        (else>)
        (local-transform> [(keypath *session-id)
                           (multi-path [:active? (termval false)]
                                       [:revoked-at (termval *occurred-at)])]
                          $$sessions)
        (|hash *user-id)
        (local-transform> [(keypath *user-id *session-id)
                           (multi-path [:active? (termval false)]
                                       [:revoked-at (termval *occurred-at)])]
                          $$sessions-by-user)
        (local-transform> [(keypath *user-id *event-id)
                           (termval {:type :session-revoked
                                     :session-id *session-id
                                     :occurred-at *occurred-at})]
                          $$login-audit)
        (ack-return> *session-id))))

  (<<query-topology topologies "find-user-by-email"
    [*email :> *user]
    (|hash *email)
    (local-select> (keypath *email) $$users-by-email :> {*user-id :user-id
                                                         :as *lookup})
    (<<if (nil? *lookup)
      (identity nil :> *user)
      (else>)
      (|hash *user-id)
      (local-select> (keypath *user-id) $$users :> *user))
    (|origin))

  (<<query-topology topologies "get-current-user"
    [*session-id :> *user]
    (|hash *session-id)
    (local-select> (keypath *session-id) $$sessions :> {*user-id :user-id
                                                        *active? :active?
                                                        :as *session})
    (<<if (and> (present? *session) *active?)
      (|hash *user-id)
      (local-select> (keypath *user-id) $$users :> *user)
      (else>)
      (identity nil :> *user))
    (|origin))

  (<<query-topology topologies "list-user-sessions"
    [*user-id :> *sessions]
    (|hash *user-id)
    (local-select> (keypath *user-id) $$sessions-by-user :> *sessions)
    (|origin)))
