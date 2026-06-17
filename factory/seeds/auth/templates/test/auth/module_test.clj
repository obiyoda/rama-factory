(ns {{app_ns}}.auth.module-test
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama.test :as rtest]
            [{{app_ns}}.auth.client :as auth]
            [{{app_ns}}.auth.module :as module]))

(deftest auth-module-register-session-and-revoke
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/AuthModule {:tasks 4 :threads 2})
    (let [clients (auth/clients ipc)
          alice (auth/register! clients {:event-id "register-alice-1"
                                         :user-id "user-alice"
                                         :email "alice@example.com"
                                         :password-hash "hash-a"
                                         :occurred-at 1})]
      (is (= "alice@example.com" (:email alice)))
      (is (= "user-alice" (:user-id (auth/find-user-by-email clients "alice@example.com"))))

      (testing "retries with the same registration event are idempotent"
        (is (= "user-alice"
               (:user-id (auth/register! clients {:event-id "register-alice-1"
                                                  :user-id "user-alice"
                                                  :email "alice@example.com"
                                                  :password-hash "hash-a"
                                                  :occurred-at 1})))))

      (testing "a different registration event cannot claim the same email"
        (is (thrown? Exception
                     (auth/register! clients {:event-id "register-alice-2"
                                             :user-id "user-other"
                                             :email "alice@example.com"
                                             :password-hash "hash-b"
                                             :occurred-at 2}))))

      (let [session (auth/create-session! clients {:event-id "session-alice-1"
                                                   :session-id "session-1"
                                                   :user-id "user-alice"
                                                   :occurred-at 3
                                                   :expires-at 999})]
        (is (= true (:active? session)))
        (is (= "alice@example.com"
               (:email (auth/current-user clients "session-1"))))
        (is (= true (get-in (auth/list-user-sessions clients "user-alice")
                            ["session-1" :active?])))

        (auth/revoke-session! clients {:event-id "revoke-alice-1"
                                       :session-id "session-1"
                                       :occurred-at 4})
        (is (nil? (auth/current-user clients "session-1")))
        (is (= false (:active? (auth/session clients "session-1"))))
        (is (= #{:registered :session-created :session-revoked}
               (set (map :type (vals (auth/login-audit clients "user-alice"))))))))))
