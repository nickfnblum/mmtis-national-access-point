(ns ote.views.admin.users
  "Admin User List"
  (:require [cljs-react-material-ui.reagent :as ui]
            [ote.ui.form-fields :as form-fields]
            [ote.app.controller.admin :as admin-controller]
            [clojure.string :as str]
            [ote.localization :refer [tr tr-key]]
            [ote.ui.common :refer [linkify]]
            [ote.ui.buttons :as buttons]
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as r]
            [ote.style.dialog :as style-dialog]
            [ote.theme.colors :as colors]
            [stylefy.core :as stylefy]
            [ote.style.base :as style-base]
            [ote.theme.colors :as colors]
            [ote.app.controller.front-page :as fp]))

(defn- edit-user-action [e! {:keys [id username show-edit-dialog?] :as user}]
  [ui/icon-button (merge {:href (str "#/user/" id)
                          :on-click #(do
                                       (.preventDefault %)
                                       (e! (fp/->ChangePage :user-edit {:id id})))}
                    (stylefy/use-style {::stylefy/manual [[:&:hover [:svg {:color (str colors/primary-button-background-color " !important")}]]]}))
   [ic/content-create]])

(defn- delete-user-action [e! {:keys [id show-delete-modal? other-members] :as user}]
  [:span
   [ui/icon-button (merge {:id (str "delete-user-" id)
                           :href "#"
                           :on-click #(do
                                        (.preventDefault %)
                                        (e! (admin-controller/->OpenDeleteUserModal id)))}
                     (stylefy/use-style {::stylefy/manual [[:&:hover [:svg {:color (str colors/primary-button-background-color " !important")}]]]}))
    [ic/action-delete]]
   (when show-delete-modal?
     (let [admin-list (mapv #(if (> (count (:members %)) 0) true false) other-members)]
       [ui/dialog
        {:open true
         :actionsContainerStyle style-dialog/dialog-action-container
         :title "Poista käyttäjä"
         :actions [(r/as-element
                     [ui/flat-button
                      {:label (tr [:buttons :cancel])
                       :primary true
                       :on-click #(do
                                    (.preventDefault %)
                                    (e! (admin-controller/->CancelDeleteUser id)))}])
                   (if (some false? admin-list)
                     nil
                     (r/as-element
                       [ui/raised-button
                        {:label (tr [:buttons :delete])
                         :icon (ic/action-delete-forever)
                         :secondary true
                         :primary true
                         :on-click #(do
                                      (.preventDefault %)
                                      (e! (admin-controller/->ConfirmDeleteUser id)))}]))]}
        [:div
         (if (some false? admin-list)
           [:p "Ei voi poistaa käyttäjää."]
           [:div
            [:p "Oletko varma, että haluat poistaa käyttäjän?"]
            [:p (str "Käyttäjän id: " id)]

            [form-fields/field {:name        :ensured-id
                                :type        :string
                                :full-width? true
                                :label       "Anna varmistukseksi käyttäjän id"
                                :update!     #(e! (admin-controller/->EnsureUserId id %))}
             (:ensured-id user)]])]]))])


(def groups-header-style {:height "1em" :padding "5px 0 5px 0"})
(def groups-row-style {:height "2em"
                       :padding 0
                       :word-break "break-all"
                       :white-space "normal"})

(defn groups-list [groups]
  [:div {:style {:border-left "1px solid rgb(224, 224, 224)"}}
   (if (seq groups)
     [:div {:style {:padding "0 0 15px 10px"}}

      [ui/table {:selectable   false
                 :fixed-header true
                 :body-style   {:overflow-y "auto"
                                :max-height "100px"}}
       [ui/table-header {:adjust-for-checkbox false
                         :display-select-all  false}
        [ui/table-row {:style groups-header-style}
         [ui/table-header-column {:style groups-header-style} "Nimi"]
         [ui/table-header-column {:style groups-header-style} "ID"]]]
       [ui/table-body {:display-row-checkbox false
                       :show-row-hover       true}

        (doall
          (for [{:keys [title name]} groups]
            ^{:key (str "group-" name)}
            [ui/table-row
             {:selectable false
              :style groups-row-style}
             [ui/table-row-column
              {:style groups-row-style}
              title]
             [ui/table-row-column
              {:style groups-row-style}
              name]]))]]]
     [:div {:style {:padding-left "10px"
                    :line-height  "48px"}}
      "Ei palveluntuottajia."])])

(defn users-page-controls [e! app]
  [:div {:style {:margin-bottom "25px"}}
   [form-fields/field {:style   {:margin-right "1rem"}
                       :type    :string :label "Nimen tai sähköpostiosoitteen osa"
                       :update! #(e! (admin-controller/->UpdateUserFilter %))
                       :on-enter #(e! (admin-controller/->SearchUsers))}
    (get-in app [:admin :user-listing :user-filter])]

   [buttons/save
    {:on-click  #(e! (admin-controller/->SearchUsers))
     :disabled (str/blank? filter)}
    "Hae käyttäjiä"]])

(defn user-listing [e! app]
  (let [{:keys [loading? results]} (get-in app [:admin :user-listing])]
    [:div
     (if loading?
       [:span "Ladataan käyttäjiä..."]
       [:div {:style {:margin-bottom "10px"}} "Hakuehdoilla löytyi " (count results) " käyttäjää."])

     (when (seq results)
       [:div
        [ui/table {:selectable false
                   :style style-base/basic-table}
         [ui/table-header
          {:class "table-header-wrap"
           :adjust-for-checkbox false
           :display-select-all false
           :selectable false}

          [ui/table-row
           [ui/table-header-column
            {:class "table-header-wrap"}
            "Sähköposti"]
           [ui/table-header-column
            {:class "table-header-wrap"}
            "Nimi"]
           [ui/table-header-column
            {:class "table-header-wrap"}
            "Palveluntuottajat"]
           [ui/table-header-column
            {:class "table-header-wrap"}
            "Toiminnot"]]]

         [ui/table-body {:display-row-checkbox false}
          (doall
            (for [{:keys [id name email groups] :as user} results]
              ^{:key (str "user-" id)}
              [ui/table-row {:style {:border-bottom "3px solid rgb(224, 224, 224)"}
                             :selectable false}
               [ui/table-row-column
                (stylefy/use-style style-base/table-col-style-wrap)
                email]
               [ui/table-row-column
                (stylefy/use-style style-base/table-col-style-wrap)
                name ]
               [ui/table-row-column
                {:style {:padding 0}}
                [groups-list groups]]
               [ui/table-row-column
                [delete-user-action e! user]
                [edit-user-action e! user]]]))]]])]))
