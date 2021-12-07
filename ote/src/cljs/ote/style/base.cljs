(ns ote.style.base
  "Base styles for OTE application. Everything that affects the overall look and feel of the app."
  (:require
    [stylefy.core :as stylefy :refer [use-style use-sub-style]]
    [ote.theme.colors :as colors]
    [ote.theme.screen-sizes :refer [width-xxs width-xs width-sm width-md width-l width-xs]]))

(def body {:margin 0
           :padding 0})

(def mobile-width-px 991)

(def wrapper {:padding-top "20px"})

(def font {:font-family "'Public Sans', sans-serif"})

(def divider {:margin "20px 0px 15px 0px"
              :border-top "1px solid rgb(217, 217, 217)"})

(def inline-block {:display "inline-block"})

(def action-button-container (merge inline-block
                                    {:margin-right "1em"}))

(def gray-text {:color colors/gray650})

(def icon-small {:width 20 :height 20
                 :vertical-align "middle"})

(def icon-medium {:width 24 :height 24
                  :vertical-align "middle"})

(def icon-large {:width 32 :height 32
                 :vertical-align "middle"})

(def icon-with-text {:display "inline-flex"
                     :align-items "center"})

(def blue-link-with-icon {:color colors/primary
                          :text-decoration "none"
                          :border "none"
                          :background "none"
                          :cursor "pointer"
                          :padding "0"
                          :display "inline-flex"
                          ::stylefy/mode {:hover {:border-bottom (str "1px solid " colors/primary)
                                                  :margin-bottom "-1px"
                                                  :color colors/primary-dark}}})

(def gray-link-with-icon (merge blue-link-with-icon {:color colors/gray950
                                                     ::stylefy/mode {:hover {:border-bottom (str "1px solid " colors/gray950)
                                                                             :margin-bottom "-1px"
                                                                             :color colors/gray950}}}))

(def base-link {:color colors/primary
                :text-decoration "none"
                ::stylefy/mode {:hover {:text-decoration "underline"}}})

(def inline-icon {:height "16px"
                  :position "relative"
                  :top "2px"
                  :align-self "center"})

(def base-button
  {:padding-left "1.1em"
   :padding-right "1.1em"
   :text-transform "uppercase"
   :font-size "12px"
   :font-weight "bold"})

(def delete-button (merge base-button {:background-color "rgb(221,0,0)"}))


(def disabled-control {:opacity 0.3
                       :pointer-events "none"})

(def button-label-style {:font-size "12px"
                         :font-weight "bold"
                         :text-transform "uppercase"
                         :color "#FFFFFF"})

(def button-add-row {:margin "15px 0 20px 0"})

(def sticky-footer {:min-height "100%"
                    :display "flex"
                    :flex-direction "column"
                    :justify-content "space-between"})

;; Form elements
(def required-element {:color "#B71C1C"
                       :font-size 14
                       :font-weight "600"})                 ;; currently same as error

(def required-input-element (merge required-element
                                   {:margin-bottom "-1.3rem"}))

(def error-element {:color "#B71C1C"
                    :font-weight "600"})

;; Front page

(def front-page-add-service {:border-right "1px solid grey"})
(def basic-table (merge {:border (str "1px solid " colors/gray650)}
                        {::stylefy/sub-styles {:link {:text-decoration "none"
                                                      ::stylefy/mode {:visited {:text-decoration "none"}
                                                                      :hover {:text-decoration "underline"}}}}}))

(stylefy/class "table-header"
               {:background-color (str colors/gray300 " !important")
                :color (str colors/gray900 " !important")
                :font-weight "bold !important"
                :position "inherit !important"})

(def table-header-wrap-map
  {:background-color (str colors/gray300 " !important")
   :color (str colors/gray900 " !important")
   :font-weight "bold !important"
   :position "inherit !important"
   :white-space "normal !important"
   :overflow-wrap "break-word !important"
   :padding-left "0.5rem !important"
   :padding-right "0 !important"})

(stylefy/class "table-header-wrap"
               table-header-wrap-map)

(stylefy/class "table-header-semi-wrap"
               (merge table-header-wrap-map
                      {:padding-left "0.75rem !important"
                       :padding-right "0.75rem !important"}))

(def header-font {:font-size "18px"
                  :padding-top "20px"
                  :font-weight "600"})

(def small-text {:font-size "0.875rem"
                 ;:font-size: 0.75rem - mobiili tekstikoko
                 :font-weight "400"
                 :line-height "1.5"
                 :color colors/gray950})

(def success-flash-message-body {:background-color "rgba(128, 149, 50, 0.87)"})
(def error-flash-message-body {:max-width "800px" :background-color "rgba(190, 0, 0, 0.87)"})

(def title {:font-weight "bold"})

(defn flex-container [dir]
  {:display "flex" :flex-direction dir})

(defn align-items [dir]
  {:align-items dir})

(defn justify-content [dir]
  {:justify-content dir})

(def wizard-container
  {:border-style "solid"
   :border-width "2px"
   :border-color colors/orange-basic
   :padding "0rem 1.25rem 2rem 1.25rem"})

(def flex-child {:flex 1})

(def item-list-container
  (merge (flex-container "row")
         {:flex-wrap "wrap"}))

(def item-list-row-margin
  {:margin-right "1em"})

(def item-list-item
  (merge inline-block
         {:position "relative"
          :margin-left "0.2em"}))

(def help {:border-radius "0.2em"
           :color "#666666"
           :background-color colors/gray200
           :padding "10px"
           :margin-bottom "10px"
           :align-items "center"})

;; Full width generic help box
(def generic-help (merge help
                         {:background-color "#F5F5F5" :padding "15px"
                          :margin-left "-15px"
                          :margin-right "-15px"
                          :margin-top "-15px"}))

(def help-link-container {:padding "10px 0px 0px 0px"})
(def link-icon-container {:float "left" :padding-right "10px"})
(def link-icon {:color "#06c" :height 18 :width 18})

(def link-color colors/primary-text-color)

(def filters-form
  {:border "solid 1px #0046ad"})

(def language-selection-dropdown
  {:border-top "solid 1px white"
   :font-size "12px"
   :margin-top "5px"
   :color "#fff"
   :padding-top "5px"
   :text-align "center"})

(def language-selection-footer
  {:border-top "solid 1px white"
   :width "100%"
   :color "#fff"
   :display "inline-block"
   :margin-top "5px"
   :padding-top "5px"})

(def language-flag
  {:padding-right "10px"})

(def section-margin {:margin-top "1em"})

(def placeholder {:color "#a0a0a0"})

(def footer-copyright {:text-align "center"
                       :margin-top "24px"
                       ;; Decrement footer bottom padding from logo height
                       :margin-bottom (str (- 48 20) "px")
                       ::stylefy/sub-styles
                       {:logo {:height "48px"
                               :width "48px"
                               :margin-bottom "8px"}}})

(def disabled-color {:color "rgba(0, 0, 0, 0.247059)"})
(def checkbox-label {:float "left"
                     :position "relative"
                     :display "block"
                     :width "calc(100% - 38px)"
                     :line-height "24px"
                     :font-family "Public Sans, sans-serif"})
(def checkbox-label-with-width (assoc checkbox-label :width "260px"))

(def mobile-extra-padding {::stylefy/media {{:max-width (str mobile-width-px "px")} {:padding-top "20px"}}})

(def checkbox-addition {:padding-left "1.25rem"
                        :padding-bottom "2px"})

(def icon-labeled-container
  {:margin-top "0.5rem"
   :margin-bottom "0.5rem"
   :display "flex"
   :flex-direction "row"
   :flex-wrap "wrap"
   :justify-content "flex-start"
   :align-items "center"})

(def icon-labeled-icon
  {:margin-right "0.125rem"})

(def msg-container (merge (flex-container "row")
                          (align-items "center")))

(def msg-warning
  {:color (str colors/warning)
   :fill (str colors/warning)})

(def msg-success
  {:color (str colors/success)
   :fill (str colors/success)})

(def circular-progress
  {:color (str colors/progress)})

(def circular-progress-label
  {:margin-left "1rem"})

(def table-col-style-wrap
  {:padding-left "0.5rem !important"
   :padding-right "0 !important"
   :overflow-wrap "break-word"
   :white-space "normal !important"})

(def table-col-style-semi-wrap (merge table-col-style-wrap
                                      {:padding-left "0.75rem !important"
                                       :padding-right "0.75rem !important"}))

(stylefy/class "table-col-style-wrap" table-col-style-wrap)
(stylefy/class "table-col-style-semi-wrap" table-col-style-semi-wrap)

(def info-row {:border-bottom (str "1px solid " colors/gray350)
               :display "flex"
               :margin-bottom "0.5rem"
               :font-size "0.875rem"})
(def info-title {:flex 3
                 :color colors/gray800
                 :overflow-wrap "break-word"
                 :padding-right "0.5rem"})

(def info-content {:flex 5
                   :display "flex"
                   :justify-content "space-between"})

(def info-title-25 {:flex-basis "25%"
                    :color colors/gray800
                    :overflow-wrap "break-word"
                    :padding-right "0.5rem"
                    ::stylefy/media {{:max-width (str width-xs "px")}
                                     {:flex-basis "50%"}}})

(def info-content-50 {:flex-basis "50%"
                      :display "flex"
                      :justify-content "space-between"
                      :overflow-wrap "anywhere"})

(def info-title-50 {:flex-basis "50%"
                    :color colors/gray800
                    :overflow-wrap "break-word"
                    :padding-right "0.5rem"})

(def info-content-75 {:flex-basis "75%"
                      :display "flex"
                      :overflow-wrap "anywhere"
                      :justify-content "space-between"
                      ::stylefy/media {{:max-width (str width-xs "px")}
                                       {:flex-basis "50%"}}})

(def capital-bold
  {:text-transform "uppercase"
   :font-weight "bold"})

(def form-footer {:background-color colors/gray200
                  :width "100%"
                  :margin-bottom "-40px"
                  :margin-top "1rem"
                  :padding-bottom "2.5rem"
                  :padding-top "1rem"})

(def notification-container {:border-top (str "4px solid " colors/purple-darker)
                             :border-left (str "1px solid " colors/purple-darker)
                             :border-right (str "1px solid " colors/purple-darker)
                             :border-bottom (str "1px solid " colors/purple-darker)
                             :color colors/purple-darker
                             :padding "1rem"
                             :margin-bottom "1rem"
                             :font-weight 600})

(def required-data-missing-container {:border-top (str "4px solid " colors/red-dark)
                                      :border-left (str "1px solid " colors/red-dark)
                                      :border-right (str "1px solid " colors/red-dark)
                                      :border-bottom (str "1px solid " colors/red-dark)
                                      :color colors/red-dark
                                      :background-color colors/gray50
                                      :padding "1rem"
                                      :margin-bottom "1rem"
                                      :font-weight 600})