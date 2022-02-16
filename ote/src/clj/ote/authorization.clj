(ns ote.authorization
  "Common helpers for authorization. Does database checks based on user info."
  (:require [specql.core :as specql]
            [ote.db.transport-operator :as t-operator]
            [specql.op :as op]
            [taoensso.timbre :as log]
            [ote.nap.users :as users]
            [ote.components.http :as http]))

(defn transit-authority-authorization-response [user]
  (when-not (get-in user [:user :transit-authority?])
    (log/info  "Not authorized. Bad role. transit-authority-authorization-response: id=" (get-in user [:user :id]))
    (http/transit-response "Not authorized. Bad role." 403)))

(defn admin? [user]
  (boolean (get-in user [:user :admin?])))

(defn user-id [user]
  (get-in user [:user :id]))

(defn user-transport-operators
  "Returns set of transport-operators the user belongs to (based on CKAN group membership)."
  [db {groups :groups :as user}]
  (into #{}
    (map ::t-operator/id)
    (specql/fetch db ::t-operator/transport-operator #{::t-operator/id}
      {::t-operator/ckan-group-id (op/in (map :id groups))})))

(defn with-group-check
  [db user ckan-group-id body-fn]
  (let [allowed? (some
                   #(= (:id %) ckan-group-id)
                   (:groups user))
        is-admin? (get-in user [:user :admin?])
        access-denied (do
                        (log/warn "User " user " tried to access transport-operator-id " ckan-group-id
                          ", allowed transport operators: " ckan-group-id)
                        {:status 403 :body "Forbidden"})]
    (cond
      (and
        (not is-admin?)
        (not allowed?))
      (access-denied)

      :else
      (body-fn))))

(defn with-transport-operator-check
  "Check that user has access (belongs to) the given transport operator.
  Runs body-fn if user has access, otherwise returns an HTTP error response and logs a warning."
  [db user transport-operator-id body-fn]
  (let [allowed-operators (user-transport-operators db user)
        is-admin? (get-in user [:user :admin?])
        access-denied #(do
                         (log/warn "User " user " tried to access transport-operator-id " transport-operator-id
                           ", allowed transport operators: " allowed-operators)
                         {:status 403 :body "Forbidden"})]
    (cond
      (nil? transport-operator-id)
      (access-denied)

      (and (not is-admin?)
        (not (contains? allowed-operators transport-operator-id)))
      (access-denied)

      :else
      (body-fn))))

(defn is-author?
  [db user transport-operator-id]
  (let [allowed-operators (user-transport-operators db user)]
    (contains? allowed-operators transport-operator-id)))

(defn with-transit-authority-check
  [db user body-fn]
  (if-not (users/has-group-attribute? db {:user-id         (user-id user)
                                          :group-attribute "transit-authority?"})
    (do
      (log/warn "User" user "tried to access backend that is restricted to transport authority users.")
      {:status 403 :body "Forbidden"})
    (body-fn)))
