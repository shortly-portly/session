(ns session.middleware
  (:require [buddy.auth.backends :as backend]
            [buddy.auth.middleware :as buddy]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.session :refer [session-backend]]
            [ring.util.response :as ring]
            [integrant.core :as ig]))

(defn wrap-authenticated? [handler]
  (fn [request]
    (if-not (authenticated? request)
      (throw-unauthorized)
      (handler request))))

(defn unauthorized-handler [request metadata]
  (ring/redirect "/logon"))


(defmethod ig/init-key :session.middleware/secure [_ _]
  (fn [handler]
    (-> handler
    (wrap-authenticated?)
    (buddy/wrap-authorization (session-backend  {:unauthorized-handler unauthorized-handler}))
    (buddy/wrap-authentication (session-backend  {:unauthorized-handler unauthorized-handler})))))
