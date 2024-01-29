(ns ote.app.controller.transport-service
  "Transport operator controls "                            ;; FIXME: Move transport-service related stuff to other file
  (:require [tuck.core :as tuck :refer-macros [define-event]]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [testdouble.cljs.csv :as csv]
            [ote.communication :as comm]
            [ote.db.transport-service :as t-service]
            [ote.db.transport-operator :as t-operator]
            [ote.db.common :as common]
            [ote.time :as time]
            [ote.localization :refer [tr tr-key tr-tree]]
            [ote.ui.form :as form]
            [ote.ui.validation :as validation]
            [ote.app.routes :as routes]
            [ote.app.controller.common :refer [->ServerError]]
            [ote.app.controller.flags :as flags]
            [ote.app.controller.front-page :as fp-controller]
            [ote.app.controller.place-search :as place-search]))

(defn- pre-set-transport-type [app]
  (let [sub-type (get-in app [:transport-service ::t-service/sub-type])
        set-transport-type (fn [app service-type options]
                             (assoc-in app [:transport-service service-type ::t-service/transport-type] options))]
    (cond
      (= sub-type :taxi) (set-transport-type app ::t-service/passenger-transportation #{:road})
      (= sub-type :parking) (set-transport-type app ::t-service/parking #{:road})
      :else app)))

(defn service-type-from-sub-type
  "Returns service type keyword based on sub-type."
  [type]
  (case type
    :taxi :passenger-transportation
    :request :passenger-transportation
    :schedule :passenger-transportation
    :terminal :terminal
    :rentals :rentals
    :parking :parking
    :passenger-transportation))

(define-event CreateServiceNavigate [operator-id sub-type]
  {}
  ;; Set transport-operator and sub-type
  (pre-set-transport-type
    (-> app
        (assoc :transport-operator (->> app :transport-operators-with-services
                                        (map :transport-operator)
                                        (filter #(= (::t-operator/id %) operator-id))
                                        first)
               :transport-service (merge (:transport-service app)
                                         (when sub-type
                                           {::t-service/sub-type sub-type
                                            ::t-service/type (service-type-from-sub-type sub-type)}))))))

(define-event ShowBrokeringServiceDialog []
  {}
  (let [brokerage-selected? (get-in app [:transport-service ::t-service/passenger-transportation ::t-service/brokerage?])]
    (if-not brokerage-selected?
      (assoc-in app [:transport-service :show-brokering-service-dialog?] true)
      app)))

(define-event SelectBrokeringService [select-type]
  {}
  (let [type (get-in app [:transport-service ::t-service/type])
        type-key (t-service/service-key-by-type type)]
    (-> app
        (assoc-in [:transport-service :show-brokering-service-dialog?] false)
        (assoc-in [:transport-service type-key ::t-service/brokerage?] select-type))))

;;; Navigation hook events for new service creation and editing

(defmethod routes/on-navigate-event :new-service [{p :params}]
  (->CreateServiceNavigate (js/parseInt (:operator-id p))
                           (keyword (:sub-type p))))

(defmethod routes/on-navigate-event :transport-service [{p :params}]
  (->CreateServiceNavigate (js/parseInt (:operator-id p)) nil))

(declare ->ModifyTransportService)

(defmethod routes/on-navigate-event :edit-service [{p :params}]
  (->ModifyTransportService (:id p)))

(defn new-transport-service [app]
  (pre-set-transport-type
    (update app :transport-service select-keys #{::t-service/type ::t-service/sub-type})))

(def service-level-keys
  #{::t-service/contact-address
    ::t-service/contact-phone
    ::t-service/contact-email
    ::t-service/homepage
    ::t-service/name
    ::t-service/external-interfaces
    ::t-service/operation-area
    ::t-service/companies
    ::t-service/published
    ::t-service/validate
    ::t-service/re-edit
    ::t-service/parent-id
    ::t-service/brokerage?
    ::t-service/description
    ::t-service/available-from
    ::t-service/available-to
    ::t-service/notice-external-interfaces?
    ::t-service/companies-csv-url
    ::t-service/company-source
    ::t-service/company-csv-filename
    :db-file-key
    :csv-count
    :csv-imported?
    :csv-valid?
    :csv-failed-companies-count
    ::t-service/transport-type})

(defrecord AddPriceClassRow [])
(defrecord AddServiceHourRow [])
(defrecord RemovePriceClassRow [])
(defrecord NavigateToNewService [])

(defrecord ModifyTransportService [id])
(defrecord ModifyTransportServiceResponse [response])
(defrecord GetTransportOperatorResponse [response])
(defrecord OpenTransportServicePage [id])
(defrecord OpenTransportServiceTypePage [])

(defrecord DeleteTransportService [id])
(defrecord ConfirmDeleteTransportService [id])
(defrecord CancelDeleteTransportService [id])
(defrecord DeleteTransportServiceResponse [response])
(defrecord FailedDeleteTransportServiceResponse [response])

(defrecord EditTransportService [form-data])
(defrecord ConfirmSaveTransportService [schemas])
(defrecord SaveTransportService [schemas validate?])
(defrecord CancelSaveTransportService [])
(defrecord SaveTransportServiceResponse [response])
(defrecord FailedTransportServiceResponse [response])
(defrecord CancelTransportServiceForm [admin])
(defrecord OpenCancelRevalidateModal [])
(defrecord CloseCancelReValidateModal [])

(defrecord SelectServiceType [data])
(defrecord SetNewServiceType [type])

(defrecord EnsureCsvFile [])
(defrecord EnsureCsvFileResponse [response])
(defrecord FailedCsvFileResponse [response])

(defrecord EnsureExternalInterfaceUrl [url format])
(defrecord EnsureExternalInterfaceUrlResponse [response url format])
(defrecord FailedExternalInterfaceUrlResponse [])

(defrecord AddImportedCompaniesToService [transport-service-id db-file-key file-input])
(defrecord UploadCSVResponse [response])
(defrecord DeleteCompanyCsvResponse [response])
(defrecord DeleteCompanyCsv [db-file-key])

(defrecord ToggleEditingDialog [])
(defrecord ConfirmEditing [])
(defrecord ReEditResponse [response])
(defrecord BackToValidation [id])
(defrecord BackToValidationResponse [response])
(defrecord OpenChangeToDraftModal [])
(defrecord CloseChangeToDraftModal [])

(declare move-service-level-keys-from-form
         move-service-level-keys-to-form)

(defn- update-service-by-id [app id update-fn & args]
  (update app :transport-service-vector
          (fn [services]
            (map #(if (= (::t-service/id %) id)
                    (apply update-fn % args)
                    %)
                 services))))

(defmulti transform-save-by-type
          "Transform transport service before sending it to the server.
          Dispatches on the type. By default, returns service as is."
          ::t-service/type)

(defmethod transform-save-by-type :rentals [service]
  (-> service
      (update-in [::t-service/rentals ::t-service/pick-up-locations]
                 (fn [pick-up-locations]
                   (map (fn [{hours-and-exceptions ::t-service/service-hours-and-exceptions :as pick-up-location}]
                          (as-> pick-up-location loc
                                (if-let [hours (::t-service/service-hours hours-and-exceptions)]
                                  (assoc loc ::t-service/service-hours hours)
                                  loc)
                                (if-let [exceptions (::t-service/service-exceptions hours-and-exceptions)]
                                  (assoc loc ::t-service/service-exceptions exceptions)
                                  loc)
                                (if-let [info (::t-service/service-hours-info hours-and-exceptions)]
                                  (assoc loc ::t-service/service-hours-info info)
                                  loc)
                                (dissoc loc ::t-service/service-hours-and-exceptions)))
                        pick-up-locations)))
      (update-in [::t-service/rentals ::t-service/vehicle-classes]
                 (fn [vehicle-classes]
                   (mapv (fn [{prices-and-units :price-group :as price-group}]
                           (as-> price-group price
                                 (if-let [prices (::t-service/price-classes prices-and-units)]
                                   (assoc price ::t-service/price-classes prices)
                                   price)
                                 (dissoc price :price-group)))
                         vehicle-classes)))))

(defmethod transform-save-by-type :default [service] service)

(defmulti transform-edit-by-type
          "Transform transport service for editing after receiving it from the server.
          Dispatches on the type. By default, returns service as is."
          ::t-service/type)

(defmethod transform-edit-by-type :rentals [service]
  (-> service
      (update-in [::t-service/rentals ::t-service/pick-up-locations]
                 (fn [pick-up-locations]
                   (mapv (fn [{hours ::t-service/service-hours
                               exceptions ::t-service/service-exceptions
                               info ::t-service/service-hours-info
                               :as pick-up-location}]
                           (-> pick-up-location
                               (assoc ::t-service/service-hours-and-exceptions
                                      {::t-service/service-hours hours
                                       ::t-service/service-exceptions exceptions
                                       ::t-service/service-hours-info info})
                               (dissoc ::t-service/service-hours
                                       ::t-service/service-exceptions)))
                         pick-up-locations)))
      (update-in [::t-service/rentals ::t-service/vehicle-classes]
                 (fn [vehicle-classes]
                   (mapv (fn [{price-classes ::t-service/price-classes
                               :as vehicle-class}]
                           (-> vehicle-class
                               (assoc :price-group
                                      {::t-service/price-classes price-classes})
                               (dissoc ::t-service/price-classes)))
                         vehicle-classes)))))

(defmethod transform-edit-by-type :default [service] service)

(defn- add-service-for-operator [app service]
  ;; Add service for currently selected transport operator and transport-operator-vector
  (as-> app app
        (update app :transport-operators-with-services
                (fn [operators-with-services]
                  (map (fn [operator-with-services]
                         (if (= (get-in operator-with-services [:transport-operator ::t-operator/id])
                                (::t-service/transport-operator-id service))
                           (update operator-with-services :transport-service-vector
                                   (fn [services]
                                     (let [service-idx (first (keep-indexed (fn [i s]
                                                                              (when (= (::t-service/id s)
                                                                                       (::t-service/id service))
                                                                                i)) services))]
                                       (if service-idx
                                         (assoc (vec services) service-idx service)
                                         (conj (vec services) service)))))
                           operator-with-services))
                       operators-with-services)))
        (assoc app :transport-service-vector
                   (some #(when (= (get-in % [:transport-operator ::t-operator/id])
                                   (get-in app [:transport-operator ::t-operator/id]))
                            (:transport-service-vector %))
                         (:transport-operators-with-services app)))))

(defn str-pul-cc->keyword-cc
  "DB stores country-codes as a string. Change pick-up country-code strings to keywords for ui."
  [pick-up-addresses]
  (mapv
    (fn [p]
      (let [p-code (get-in p [::t-service/pick-up-address ::common/country_code])
            p-code (if (nil? p-code)
                     :A
                     (keyword p-code))]
        (if (some? pick-up-addresses)
          (assoc-in p [::t-service/pick-up-address ::common/country_code] p-code)
          p)))
    pick-up-addresses))

(defn keyword-pul-cc->str-cc
  "DB stores country-codes as a string. Change pick-up country-code keyword to strings for DB."
  [pick-up-addresses]
  (mapv
    (fn [p]
      (let [country-code (get-in p [::t-service/pick-up-address ::common/country_code])
            country-code (when (and country-code (not= :A country-code))
                           (name country-code))]
        (assoc-in p [::t-service/pick-up-address ::common/country_code] country-code)))
    pick-up-addresses))

(defn str-cc->keyword-cc
  "DB stores country-codes as a string. Change pick-up country-code strings to keywords for ui."
  [app service]
  (let [key (t-service/service-key-by-type (::t-service/type service))
        country-code (get-in service [key ::t-service/contact-address ::common/country_code])
        country-code (if (nil? country-code)
                       :A
                       (keyword country-code))
        app (if (= :rentals (::t-service/type service))
              (update-in app [:transport-service key ::t-service/pick-up-locations]
                         #(str-pul-cc->keyword-cc %))
              app)]
    (if (some? service)
      (assoc-in app [:transport-service key ::t-service/contact-address ::common/country_code] country-code)
      app)))

(defn keyword-cc->str-cc
  "DB stores country-codes as a string. Change pick-up country-code keyword to strings for DB."
  [service]
  (let [key (t-service/service-key-by-type (::t-service/type service))
        country-code (get-in service [key ::t-service/contact-address ::common/country_code])
        country-code (when (and country-code (not= :A country-code))
                       (name country-code))
        service (if (= :rentals (::t-service/type service))
                  (update-in service [key ::t-service/pick-up-locations]
                             #(keyword-pul-cc->str-cc %))
                  service)]
    (assoc-in service [key ::t-service/contact-address ::common/country_code] country-code)))

(defn toggle-edit-dialog [app]
  (assoc-in app [:transport-service :edit-dialog] (not (get-in app [:transport-service :edit-dialog]))))

(defn validate-or-publish [service validate?]
  (if (flags/enabled? :service-validation)
    (assoc service ::t-service/validate? validate?)
    (assoc service ::t-service/published? validate?)))

(extend-protocol tuck/Event

  AddPriceClassRow
  (process-event [_ app]
    (update-in app [:transport-service ::t-service/passenger-transportation ::t-service/price-classes]
               #(conj (or % []) {::t-service/currency "EUR"})))

  AddServiceHourRow
  (process-event [_ app]
    (update-in app [:transport-service ::t-service/passenger-transportation ::t-service/service-hours]
               #(conj (or % []) {::t-service/from (time/parse-time "08:00")})))

  RemovePriceClassRow
  (process-event [_ app]
    (assoc-in app [:t-service :price-class-open] false))

  SelectServiceType
  ;; Set only service type and sub-type
  (process-event [{data :data} app]
    (let [type (service-type-from-sub-type data)
          app (assoc-in app [:transport-service ::t-service/sub-type] data)
          app (assoc-in app [:transport-service ::t-service/type] type)]
      app))

  NavigateToNewService
  ;; Redirect to add service page
  (process-event [_ app]
    (let [app (new-transport-service app)
          sub-type (get-in app [:transport-service ::t-service/sub-type])]
      (routes/navigate! :new-service {:operator-id (-> app :transport-operator ::t-operator/id str)
                                      :sub-type (name sub-type)})
      app))

  OpenTransportServiceTypePage
  ;; :transport-service :<transport-service-type> needs to be cleaned up before creating a new one
  (process-event [_ app]
    (let [app (new-transport-service app)]
      (routes/navigate! :transport-service {:operator-id (-> app :transport-operator ::t-operator/id str)})
      app))

  ModifyTransportService
  (process-event [{id :id} app]
    (comm/get! (str "transport-service/" id)
               {:on-success (tuck/send-async! ->ModifyTransportServiceResponse)})
    (assoc app :transport-service-loaded? false))

  ModifyTransportServiceResponse
  (process-event [{response :response} app]
    (let [type (::t-service/type response)
          app (assoc app
                :transport-service-loaded? true
                :transport-service (-> response
                                       (update ::t-service/operation-area place-search/operation-area-to-places)
                                       (move-service-level-keys-to-form (t-service/service-key-by-type type))
                                       transform-edit-by-type)
                :transport-operator (->> app :transport-operators-with-services
                                         (map :transport-operator)
                                         (filter #(= (::t-operator/id %)
                                                     (::t-service/transport-operator-id response)))
                                         first)
                :service-operator nil)
          app (str-cc->keyword-cc app (:transport-service app))]

      ;; Get operator data
      (comm/get! (str "t-operator/" (get-in app [:transport-service ::t-service/transport-operator-id]))
                 {:on-success (tuck/send-async! ->GetTransportOperatorResponse)})
      app))

  GetTransportOperatorResponse
  (process-event [{response :response} app]
    (assoc app :service-operator response))

  EnsureCsvFile
  (process-event [_ app]
    (let [url (get-in app [:transport-service ::t-service/passenger-transportation ::t-service/companies-csv-url])]
      (when (and url (not (empty? url)))
        (comm/post! (str "check-company-csv")
                    {:url url}
                    {:on-success (tuck/send-async! ->EnsureCsvFileResponse)
                     :on-failure (tuck/send-async! ->FailedCsvFileResponse)}))
      (update-in app [:transport-service ::t-service/passenger-transportation] dissoc :csv-count)))

  EnsureCsvFileResponse
  (process-event [{response :response} app]
    (assoc-in app [:transport-service ::t-service/passenger-transportation :csv-count] response))

  FailedCsvFileResponse
  (process-event [{response :response} app]
    (assoc-in app [:transport-service ::t-service/passenger-transportation :csv-count] response))

  EnsureExternalInterfaceUrl
  (process-event [{url :url format :format} app]
    (let [on-success (tuck/send-async! ->EnsureExternalInterfaceUrlResponse url format)
          on-failure (tuck/send-async! ->FailedExternalInterfaceUrlResponse)]
      (update-in app [:transport-service (t-service/service-key-by-type (::t-service/type (:transport-service app)))
                      ::t-service/external-interfaces]
                 (fn [external-interfaces]
                   (mapv (fn [{eif ::t-service/external-interface eif-format ::t-service/format :as row}]
                           (if (and (= (::t-service/url eif) url) (= (first eif-format) format))
                             (do
                               (when-let [validation-timeout (:eif-validation-timeout row)]
                                 (.clearTimeout js/window validation-timeout))

                               ;; Debounce the validation to prevent unnecessary validation requests as they
                               ;; can hog server resources.
                               (assoc row :eif-validation-timeout
                                          (.setTimeout
                                            js/window
                                            #(comm/post! (str "check-external-api") {:url url :format format}
                                                         {:on-success on-success
                                                          :on-failure on-failure})
                                            1000)))
                             row))
                         external-interfaces)))))

  EnsureExternalInterfaceUrlResponse
  (process-event [{url :url format :format response :response :as e} app]
    (update-in app [:transport-service (t-service/service-key-by-type (::t-service/type (:transport-service app)))
                    ::t-service/external-interfaces]
               (fn [external-interfaces]
                 (mapv (fn [{eif ::t-service/external-interface eif-format ::t-service/format :as row}]
                         (if (and (= (::t-service/url eif) url) (= (first eif-format) format))
                           (-> row
                               (assoc ::t-service/external-interface (assoc eif :url-status response))
                               (dissoc :eif-validation-timeout))
                           row))
                       external-interfaces))))

  FailedExternalInterfaceUrlResponse
  (process-event [{response :response} app]
    app)

  UploadCSVResponse
  (process-event [{response :response} app]
    (let [valid? (if (> (:failed-count response) 0) false true)]

      (if valid?
        (update-in app [:transport-service ::t-service/passenger-transportation] assoc
                   ::t-service/companies (:companies response)
                   ::t-service/company-csv-filename (:filename response)
                   :csv-imported? true
                   :csv-valid? true
                   :csv-failed-companies-count 0
                   :db-file-key (:db-file-key response))
        (update-in app [:transport-service ::t-service/passenger-transportation] assoc
                   ::t-service/company-csv-filename (:filename response)
                   ::t-service/companies {}
                   :csv-imported? true
                   :csv-valid? false
                   :csv-failed-companies-count (:failed-count response)
                   :db-file-key (:db-file-key response)))))

  DeleteCompanyCsvResponse
  (process-event [{response :response} app]
    (update-in app [:transport-service ::t-service/passenger-transportation]
               #(-> %
                    (dissoc :db-file-key
                            :csv-valid?
                            :csv-imported?
                            :csv-failed-companies-count
                            :csv-valid-companies-count)
                    (assoc ::t-service/company-csv-filename nil
                           :ote.db.transport-service/companies {}))))

  DeleteCompanyCsv
  (process-event [{db-file-key :db-file-key} app]
    (comm/delete! (str "/transport-service/delete-csv") {:file-key db-file-key}
                  {:on-success (tuck/send-async! ->DeleteCompanyCsvResponse)
                   :on-failure (tuck/send-async! ->ServerError)})
    app)

  AddImportedCompaniesToService
  (process-event [{transport-service-id :transport-service-id db-file-key :db-file-key file-input :file-input} app]
    ;; Send csv file to server to parse its content
    (let [url (str "transport-service/upload-company-csv/"
                   (or transport-service-id 0) "/" (ote.util.text/rand-str 30))]
      (comm/upload! url file-input
                    {:on-success (tuck/send-async! ->UploadCSVResponse)
                     :on-failure (tuck/send-async! ->ServerError)})
      app))

  ;; Use this when navigating outside of OTE. Above methods won't work from NAP.
  OpenTransportServicePage
  (process-event [{id :id} app]
    (set! (.-location js/window) (str "/ote/#/edit-service/" id))
    app)

  DeleteTransportService
  (process-event [{id :id} app]
    (update-service-by-id
      app id
      assoc :show-delete-modal? true))

  CancelDeleteTransportService
  (process-event [{id :id} app]
    (update-service-by-id
      app id
      dissoc :show-delete-modal?))

  ConfirmDeleteTransportService
  (process-event [{id :id} app]
    (comm/post! "transport-service/delete" {:id id}
                {:on-success (tuck/send-async! ->DeleteTransportServiceResponse)
                 :on-failure (tuck/send-async! ->FailedDeleteTransportServiceResponse)})
    app)

  DeleteTransportServiceResponse
  (process-event [{response :response} app]
    (let [filtered-map (filter #(not= (:ote.db.transport-service/id %) (int response)) (get app :transport-service-vector))
          app (assoc app :transport-service-vector filtered-map
                         :flash-message (tr [:common-texts :delete-service-success])
                         :services-changed? true)]
      (fp-controller/get-transport-operator-data app)
      app))

  FailedDeleteTransportServiceResponse
  (process-event [{response :response} app]
    (assoc app :flash-message-error (tr [:common-texts :delete-service-error])))

  CancelSaveTransportService
  (process-event [_ app]
    ;; Close modal
    (assoc-in app [:transport-service :show-confirm-save-dialog?] false))

  ConfirmSaveTransportService
  (process-event [{schemas :schemas} app]
    (assoc-in app [:transport-service :show-confirm-save-dialog?] true))

  SaveTransportService
  (process-event [{:keys [schemas validate?]} {service :transport-service
                                               operator :transport-operator :as app}]
    (let [key (t-service/service-key-by-type (::t-service/type service))
          operator-id (if (nil? (::t-service/transport-operator-id service))
                        (::t-operator/id operator)
                        (::t-service/transport-operator-id service))
          ;; Service validation flag changes if service is saved as public or validate
          service (validate-or-publish service validate?)
          service-data
          (-> service
              (update key (comp (partial form/prepare-for-save schemas)
                                form/without-form-metadata))
              (dissoc :transport-service-type-subtype
                      :select-transport-operator
                      :show-brokering-service-dialog?
                      :show-confirm-save-dialog?
                      :show-confirm-cancel-dialog?
                      :edit-dialog
                      :csv-failed-companies-count
                      :csv-imported?
                      :csv-valid-companies-count
                      :csv-valid?)
              (keyword-cc->str-cc)
              (move-service-level-keys-from-form key)
              (assoc ::t-service/transport-operator-id operator-id)
              (update ::t-service/operation-area place-search/place-references)
              (update ::t-service/external-interfaces
                      (fn [d]
                        (mapv #(dissoc %
                                       :eif-validation-timeout
                                       :tis-vaco) d)))
              transform-save-by-type)]
      ;; Disable post if concurrent save event is in progress
      (if (not (:service-save-in-progress app))
        (do
          (comm/post! "transport-service" service-data
                      {:on-success (tuck/send-async! ->SaveTransportServiceResponse)
                       :on-failure (tuck/send-async! ->FailedTransportServiceResponse)})
          (-> app
              (assoc :service-save-in-progress true)))
        app)))

  SaveTransportServiceResponse
  (process-event [{response :response} app]
    (let [app (add-service-for-operator
                (assoc app :flash-message (tr [:common-texts :transport-service-saved]))
                response)]
      (routes/navigate! :own-services)
      (-> app
          (assoc :service-save-in-progress false
                 :services-changed? true)
          (dissoc :transport-service
                  ;; Remove navigation prompt message only if save was successful.
                  :before-unload-message))))

  FailedTransportServiceResponse
  (process-event [{response :response} app]
    (assoc app :service-save-in-progress false
               :flash-message-error (tr [:common-texts :save-failed])))

  EditTransportService
  (process-event [{form-data :form-data} {ts :transport-service :as app}]
    (let [key (t-service/service-key-by-type (::t-service/type ts))
          unsaved-key (if (get-in app [:transport-service key ::t-service/re-edit])
                        [:dialog :navigation-prompt :unsaved-validated-data]
                        [:dialog :navigation-prompt :unsaved-data])]
      (-> app
          (update-in [:transport-service key] merge form-data)
          (assoc :before-unload-message unsaved-key))))

  CancelTransportServiceForm
  (process-event [{admin :admin} app]
    (if admin
      (routes/navigate! :admin)
      (routes/navigate! :own-services))
    app)

  OpenCancelRevalidateModal
  (process-event [_ app]
    (assoc-in app [:transport-service :show-cancel-revalidate-dialog?] true))

  CloseCancelReValidateModal
  (process-event [_ app]
    (assoc-in app [:transport-service :show-cancel-revalidate-dialog?] false))

  SetNewServiceType
  (process-event [_ app]
    ;; This is needed when directly loading a new service URL to set the type
    (let [sub-type (keyword (get-in app [:params :sub-type]))]
      (-> app
          (assoc-in [:transport-service ::t-service/sub-type] sub-type)
          (assoc-in [:transport-service ::t-service/type] (service-type-from-sub-type sub-type))
          (pre-set-transport-type))))

  ToggleEditingDialog
  (process-event [_ app]
    (toggle-edit-dialog app))

  ReEditResponse
  (process-event [{response :response} app]
    ;; Assume that everything is ok because this requires 200 ok response
    (let [sub-service (keyword (str "ote.db.transport-service/" (name (get-in app [:transport-service ::t-service/type]))))]
      (-> app
          (toggle-edit-dialog)
          (assoc-in [:transport-service sub-service ::t-service/validate] nil)
          (assoc-in [:transport-service sub-service ::t-service/re-edit] response)
          (assoc :transport-service-loaded? true)
          (assoc :before-unload-message [:dialog :navigation-prompt :unsaved-validated-data])
          ;; Give entire function because otherwise the caller wont be able to use it
          (assoc :before-unload-fn (ote.app.controller.transport-service/->BackToValidation (get-in app [:transport-service ::t-service/id]))))))

  ;; When service is in validation we need to enable editing for users.
  ConfirmEditing
  (process-event [_ app]
    (comm/post! (str "transport-service/" (get-in app [:transport-service ::t-service/id]) "/re-edit-service") {}
                {:on-success (tuck/send-async! ->ReEditResponse)
                 :on-failure (tuck/send-async! ->ServerError)})
    (assoc app :transport-service-loaded? false))

  BackToValidationResponse
  (process-event [{response :response} app]
    ; In rare case, when user doesn't want to edit service which is in re-edit state, the state is
    ; changed before and this will update own-service page service lists.
    (comm/post! "/transport-operator/data" {}
                {:on-success (tuck/send-async! ote.app.controller.own-services/->LoadOperatorDataResponse)
                 :on-failure (tuck/send-async! ->ServerError)})

    (routes/navigate! :own-services)

    (-> app
        (dissoc :navigation-prompt-open?
                :before-unload-message
                :before-unload-fn
                :navigation-confirm)
        (assoc :transport-service-loaded? true)))

  BackToValidation
  (process-event [{id :id} app]
    (comm/post! (str "transport-service/" id "/back-to-validation") {}
                {:on-success (tuck/send-async! ->BackToValidationResponse)
                 :on-failure (tuck/send-async! ->ServerError)})
    (assoc app :transport-service-loaded? false))

  OpenChangeToDraftModal
  (process-event [_ app]
    (assoc-in app [:transport-service :show-confirm-cancel-dialog?] true))

  CloseChangeToDraftModal
  (process-event [_ app]
    (assoc-in app [:transport-service :show-confirm-cancel-dialog?] false)))

(defn move-service-level-keys-from-form
  "The form only sees the type specific level, move keys that are stored in the
  transport-service level there."
  [service from]
  (reduce (fn [service key]
            (as-> service s
                  (if (contains? (get service from) key)
                    (assoc s key (get-in service [from key]))
                    s)
                  (update s from dissoc key)))
          service
          service-level-keys))

(defn move-service-level-keys-to-form
  "Reverse of `move-service-level-keys-from-form`."
  [service to]
  (reduce (fn [service key]
            (as-> service s
                  (if (contains? service key)
                    (assoc-in s [to key] (get service key))
                    s)
                  (dissoc s key)))
          service
          service-level-keys))

(defn is-service-owner?
  "Admin can see services that they don't own. So we need to know, if user is a service owner"
  [app]
  (let [service-operator-id (get-in app [:transport-service ::t-service/transport-operator-id])
        first-matching-item (some
                              #(= service-operator-id (get-in % [:transport-operator ::t-operator/id]))
                              (get app :transport-operators-with-services))]
    (if (not (nil? first-matching-item))
      true
      false)))

(defn read-companies-csv! [e! file-input transport-service-id db-file-key]
  (e! (->AddImportedCompaniesToService transport-service-id db-file-key file-input)))

(defn service-state [validate re-edit published is-child?]
  (if (flags/enabled? :service-validation)
    (cond
      (some? re-edit) :re-edit
      (some? published) :public
      (and (not is-child?) (some? validate)) :validation    ;; First time in validation
      (and is-child? (some? validate)) :re-validation       ;; Service is already at least once validated
      :else :draft)
    (if (some? published)
      :public
      :draft)))

(defn in-readonly? [in-validation? admin-validating-id service-id]
  (if (flags/enabled? :service-validation)
    ;; If flag is enabled, there is a change that service is in readonly state
    (and
      in-validation?
      (not= service-id admin-validating-id))
    ;; If flag is not set service could not be in readonly state
    false))