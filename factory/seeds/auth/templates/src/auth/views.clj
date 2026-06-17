(ns {{app_ns}}.auth.views
  (:require [clojure.string :as str]))

(defn- page
  [title body]
  (str "<main class=\"auth-page\"><h1>" title "</h1>" body "</main>"))

(defn login-page
  []
  (page
   "Login"
   (str "<form method=\"post\" action=\"/login\">"
        "<label>Email <input name=\"email\" type=\"email\" autocomplete=\"email\"></label>"
        "<label>Password <input name=\"password\" type=\"password\" autocomplete=\"current-password\"></label>"
        "<button type=\"submit\">Login</button>"
        "</form>")))

(defn register-page
  []
  (page
   "Register"
   (str "<form method=\"post\" action=\"/register\">"
        "<label>Email <input name=\"email\" type=\"email\" autocomplete=\"email\"></label>"
        "<label>Password <input name=\"password\" type=\"password\" autocomplete=\"new-password\"></label>"
        "<button type=\"submit\">Create account</button>"
        "</form>")))

(defn current-user-page
  [user]
  (page
   "Current user"
   (if user
     (str "<p>" (str/escape (:email user) {\< "&lt;" \> "&gt;" \& "&amp;"}) "</p>")
     "<p>No user is signed in.</p>")))
