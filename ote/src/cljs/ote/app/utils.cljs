(ns ote.app.utils
  (:import [goog.async Debouncer]))

(defn debounce
  "Calls function after interval, and replaces previous call if called during interval."
  [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))


(def email-regex #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defn user-logged-in? [app]
  (not-empty (get-in app [:user])))

(defn user-operates-service-type? [app service-type]
  (some #(= (:ote.db.transport-service/sub-type %) service-type) (:transport-service-vector app)))