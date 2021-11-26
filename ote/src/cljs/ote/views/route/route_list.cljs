(ns ote.views.route.route-list
  "List own routes"
  (:require
    [reagent.core :as r]
    [ote.localization :refer [tr tr-key]]
    [ote.localization :refer [selected-language]]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-react-material-ui.icons :as ic]
    [stylefy.core :as stylefy]

    [ote.time :as time]
    [ote.db.transport-operator :as t-operator]
    [ote.db.transit :as transit]
    [ote.db.modification :as modification]
    [ote.db.transport-service :as t-service]
    [ote.util.transport-operator-util :as op-util]

    [ote.app.controller.front-page :as fp]
    [ote.app.controller.route.route-list :as route-list]
    [ote.app.controller.transport-operator :as to]
    [ote.ui.common :as common]
    [ote.ui.form-fields :as form-fields]
    [ote.ui.buttons :as buttons]
    [ote.ui.page :as page]

    [ote.style.dialog :as style-dialog]
    [ote.style.buttons :as style-buttons]
    [ote.style.base :as style-base]
    [ote.theme.colors :as colors]))

(defn- delete-route-action [e! {::transit/keys [route-id name]
                                :keys [show-delete-modal?]
                                :as route}]
  [:span
   [ui/icon-button (merge {:id (str "delete-route-" route-id)
                           :href "#"
                           :on-click #(do
                                        (.preventDefault %)
                                        (e! (route-list/->OpenDeleteRouteModal route-id)))}
                          (stylefy/use-style {::stylefy/manual [[:&:hover [:svg {:color (str colors/primary-button-background-color " !important")}]]]}))
    [ic/action-delete]]
   (when show-delete-modal?
     [ui/dialog
      {:open true
       :actionsContainerStyle style-dialog/dialog-action-container
       :title (tr [:route-list-page :delete-dialog-header])
       :actions [(r/as-element
                   [buttons/cancel
                    {:on-click #(do
                                  (.preventDefault %)
                                  (e! (route-list/->CancelDeleteRoute route-id)))}
                    (tr [:buttons :cancel])])
                 (r/as-element
                   [buttons/delete
                    {:on-click  #(do
                                   (.preventDefault %)
                                   (e! (route-list/->ConfirmDeleteRoute route-id)))}
                    (tr [:buttons :delete])])]}

      (str (tr [:route-list-page :delete-dialog-remove-route]) (t-service/localized-text-with-fallback @selected-language name))])])

(defn- route-table [e! routes empty-row-text]
  [ui/table {:style style-base/basic-table}
   [ui/table-header {:adjust-for-checkbox false
                     :display-select-all false}
    [ui/table-row {:selectable false :style {:border-bottom (str "1px solid" colors/gray650)}}
     [ui/table-header-column {:class "table-header" :style {:width "18%"}} (tr [:route-list-page :route-list-table-name])]
     [ui/table-header-column {:class "table-header" :style {:width "15%"}} (tr [:route-list-page :route-list-table-modified])]
     [ui/table-header-column {:class "table-header" :style {:width "15%"}} (tr [:route-list-page :route-list-table-created])]
     [ui/table-header-column {:class "table-header" :style {:width "11%"}} (tr [:route-list-page :route-list-table-actions])]]]
   [ui/table-body {:display-row-checkbox false}
    (if empty-row-text
      [ui/table-row {:key (str empty-row-text) :selectable false :display-border false}
       [ui/table-row-column {:style {:width "100%"}} empty-row-text]
       [ui/table-row-column ""]
       [ui/table-row-column ""]
       [ui/table-row-column ""]]
     (doall
       (map-indexed
         (fn [i {::transit/keys [route-id name]
                 ::modification/keys [created modified] :as row}]
           ^{:key (str "route-" i)}
           [ui/table-row {:key (str "route-" i) :selectable false :display-border false :style {:border-bottom (str "1px solid" colors/gray650)}}
            [ui/table-row-column {:style {:width "18%"}}
             [:a {:href (str "/#/edit-route/" route-id)
                  :on-click #(do
                               (.preventDefault %)
                               (e! (fp/->ChangePage :edit-route {:id route-id})))} (t-service/localized-text-with-fallback @selected-language name)]]
            [ui/table-row-column {:style {:width "15%"}} (time/format-timestamp-for-ui modified)]
            [ui/table-row-column {:style {:width "15%"}} (time/format-timestamp-for-ui created)]
            [ui/table-row-column {:style {:width "11%" :padding-left "10px"}}
             [ui/icon-button
              (merge {:href "#"
                      :on-click #(do
                                   (.preventDefault %)
                                   (e! (fp/->ChangePage :edit-route {:id route-id})))}
                     (stylefy/use-style {::stylefy/manual [[:&:hover [:svg {:color (str colors/primary-button-background-color " !important")}]]]}))
              [ic/content-create]]
             [delete-route-action e! row]]])
         routes)))]])

(defn service-linked-to-route [service services-with-route]
  (let [my-list (some
                  (fn [service-with-route]
                    (= (:id service-with-route)
                       (::t-service/id service)))
                  services-with-route)]
    (if my-list true false)))

(defn- link-services-to-routes [e! services services-with-route has-public-routes]
  (if has-public-routes
    [:div
     [:h4 (tr [:route-list-page :header-link-public-routes-to-service])]
     [:p {:style (merge
                   {:margin-top "0px"}
                   style-base/small-text)}
      (tr [:route-list-page :select-services-to-link-interfaces])]

     (when (empty? services-with-route)
       [:div
        [:span
         {:style (merge
                   {:margin-top "0px"}
                   style-base/small-text
                   style-base/icon-with-text)}
         [ic/alert-warning {:style {:color colors/negative-button
                                    :margin-right "0.5rem"
                                    :margin-bottom "0.25rem"}}]
         [:p
          (tr [:route-list-page :unlinked-routes])]]])

     (doall
       (for [{::t-service/keys [id name] :as s} services]
         ^{:key (str "link-service-id-" name "-" id)}
         [:div
          [ui/checkbox {:label name
                        :id (str "checkbox-" name "-" id)
                        :checked (service-linked-to-route s services-with-route)
                        :on-click #(e! (route-list/->ToggleLinkInterfaceToService
                                         id
                                         (service-linked-to-route s services-with-route)))}]]))]
    [:div
     [:h4 (tr [:route-list-page :header-link-interface-to-service])]
     [:p (tr [:route-list-page :desc-link-interface-to-service])]]))

(defn list-routes [e! public-routes draft-routes]
  [:div {:style {:padding-top "20px"}}
   [:div {:style {:padding-bottom "1rem"}}
    [:h4 (tr [:route-list-page :header-route-drafts])]
    [:p {:style (merge
                  {:margin-top "0px"}
                  style-base/small-text)}
     (tr [:route-list-page :desc-route-drafts])]
    [:div.drafts
     [route-table e! draft-routes (when (empty? draft-routes)
                                    (tr [:route-list-page :no-draft-routes]))]]]
   [:div {:style {:padding-bottom "1rem"}}
    [:h4 (tr [:route-list-page :header-public-routes])]
    [:p {:style (merge
                  {:margin-top "0px"}
                  style-base/small-text)}
     (tr [:route-list-page :desc-public-routes])]
    [:div.public
     [route-table e! public-routes (when (empty? public-routes)
                                     (tr [:route-list-page :no-public-routes]))]]]])

(defn routes [e! {operator :transport-operator
                  operators :transport-operators-with-services
                  :as app}]
  (let [routes (get-in app [:routes :routes-vector])
        services-with-route (get-in app [:routes :route-used-in-services])
        public-routes (filter (fn [r]
                                (true? (::transit/published? r)))
                              routes)
        draft-routes (filter (fn [r]
                               (false? (::transit/published? r)))
                             routes)
        operator-services (to/current-operator-services app)
        schedule-services (filter
                            (fn [{::t-service/keys [sub-type] :as s}]
                              (or (= :schedule sub-type) false))
                            operator-services)
        fake-operator {::t-operator/id 987654321
                       ::t-operator/name (tr [:common-texts :no-operators])}]
    [:div
     [page/page-controls "" (tr [:route-list-page :header-route-list])
      [:div
       [:h4 {:style {:margin "0"}}
        (tr [:field-labels :select-transport-operator])]
       (if operator
        [form-fields/field
         {:element-id "select-operator-at-searoute-frontpage"
          :name :select-transport-operator
          :type :selection
          :show-option #(::t-operator/name %)
          :update! #(e! (to/->SelectOperator %))
          :options (mapv to/take-operator-api-keys (mapv :transport-operator operators))
          :auto-width? true
          :class-name "mui-select-button"}
         (to/take-operator-api-keys operator)]
        [form-fields/field
         {:element-id "empty-operator-select-searoute-frontpage"
          :name :select-transport-operator
          :type :selection
          :show-option #(::t-operator/name %)
          :options [fake-operator]
          :auto-width? true
          :class-name "mui-select-button"}
         fake-operator])]]

     (if operator
      [:div.container
       [:h2 (:ote.db.transport-operator/name operator)]
       [:a (merge {:href (str "#/new-route/")
                   :id "new-route-button"
                   :on-click #(do
                                (.preventDefault %)
                                (e! (route-list/->CreateNewRoute)))}
                  (stylefy/use-style style-buttons/primary-button))
        (tr [:buttons :add-new-route])]
       [list-routes e! public-routes draft-routes]
       (when schedule-services
         [link-services-to-routes e! schedule-services services-with-route (not (empty? public-routes))])
       (if (not (empty? public-routes))
         (let [loc (.-location js/document)
               url (str (.-protocol loc) "//" (.-host loc) (.-pathname loc)
                        "export/gtfs/" (::t-operator/id operator))]
           [:div {:style {:padding-top "1rem"}}
            [:h4 (tr [:route-list-page :header-sea-route-interface])]
            [:p {:style (merge
                          {:margin-top "0px"}
                          style-base/small-text)}
             (tr [:route-list-page :desc-sea-route-interface])]
            [common/linkify url (op-util/gtfs-file-name operator)]])
         [:div
          [:h4 (tr [:route-list-page :header-download-gtfs])]
          [:p (tr [:route-list-page :desc-download-gtfs])]])]
      [:div.container
       [:p (tr [:route-list-page :add-operator-and-service])]
       [common/back-link-with-event :own-services (tr [:front-page :move-to-services-page])]])]))
