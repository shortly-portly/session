(ns session.view.session
  (:require [session.view.bootstrap :as bs]))

(defn new-user [data]
  (bs/layout
   [:div
    [:h1 "Create User"]

    (bs/form "/user/new"
             data
             [:div
              (bs/text-input "Email" :email data)
              (bs/text-input "Password" :password data)
              (bs/submit-btn "Create User")])]))

(defn welcome []
  (bs/layout
   [:div
    [:h1 "Welcome"]]))

(defn logon [data]
  (bs/layout
   [:div
    [:h1 "Logon"]

    (bs/form "/logon"
             data
             [:div
              (bs/text-input "Email" :email data)
              (bs/text-input "Password" :password data)
              (bs/submit-btn "Logon")])]))
