(ns session.handler.session
  (:require [ataraxy.response :as response]
            [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            duct.database.sql
            [integrant.core :as ig]
            [ring.util.response :as ring]
            [session.view.session :as view]
            [struct.core :as st]))

;Boundaries
(defprotocol User
  (create-user [db db-params])
  (fetch-user [db params]))

(extend-protocol User
  duct.database.sql.Boundary
  (create-user [{db :spec} params]
    (try
    (let [password (params :password)
          pw-hash (hashers/derive password)
          db-params (assoc params :password pw-hash)
          results (jdbc/insert! db :users db-params)]
      [nil (-> results ffirst val) ])
    (catch Exception e
      (let [[text table column] (re-find #"UNIQUE constraint failed:\s(\w*)\.(\w*)" (.getMessage e))]
        (if (= column "email")
          [{:email "This email has already been taken"} nil]
          (throw e))))))

  (fetch-user [{db :spec} {:keys [email]}]
    (println (jdbc/query db ["SELECT * FROM users where email = ?" email]))
    (let [result (first (jdbc/query db ["SELECT * FROM users where email = ?" email]))]
      (if result [nil result] [true nil]))))

;Validations
(def user-schema
  [[:email st/required st/string]
   [:password st/required st/string]])

(defn validate-user [params]
  (st/validate params user-schema {:strip true}))


(defn validate-password [user {:keys [password]}]
  (if (hashers/check password (user :password))
    [nil true]
    [true nil]))

;Initialise Keys
(defmethod ig/init-key :session.handler.session/new-user [_ _]
  (fn [{:keys [flash]}]
    (let [data flash]
      [::response/ok (view/new-user data)])))

(defmethod ig/init-key :session.handler.session/create-user [_ {:keys [db]}]
  (fn [{:keys [params]}]
    (let [[errors params] (validate-user params)
          [errors result] (if-not errors (create-user db params) [errors nil])]
    (if errors
      (-> (ring/redirect "/user/new")
          (assoc :flash (assoc params :errors errors)))
      (do
        (ring/redirect "/user/new"))))))

(defmethod ig/init-key :session.handler.session/logon [_ _]
  (fn [{:keys [flash]}]
    (let [data flash]
      [::response/ok (view/logon data)])))

(defmethod ig/init-key :session.handler.session/logoff [_ _]
  (fn [{:keys [session]}]
    (assoc (ring/redirect "/logon") :session (assoc session :identity nil))))

(defmethod ig/init-key :session.handler.session/create [_ {:keys [db]}]
  (fn [{:keys [params session]}]
    (let [[errors user] (fetch-user db params)
          [errors valid-password] (if-not errors (validate-password user params) [errors nil])]
      (if errors
        (-> (ring/redirect "/logon")
            (assoc :flash (assoc params :errors {:email "Invalid Email or Password"})))
        (-> (ring/redirect "/welcome")
            (assoc  :session (assoc session :identity user)))))))

(defmethod ig/init-key :session.handler.session/welcome [_ _]
  (fn [request]
      [::response/ok (view/welcome)]))
