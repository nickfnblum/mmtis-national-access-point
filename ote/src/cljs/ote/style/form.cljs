(ns ote.style.form
  "Form layout styles"
  (:require [stylefy.core :as stylefy]
            [garden.units :refer [pt px em]]
            [ote.style.base :as base]))

;; FIXME: use garden unit helpers (currently stylefy has a bug and they don't work)


(def form-field {:margin-right "0.5rem"})

(def div-form-field {:padding-right "0.25rem"
                     :padding-left "0.25rem"
                     :min-height "5.5rem"})

(def form-group-base {:margin-bottom "1em"})

(def form-group-column (merge form-group-base
                              (base/flex-container "column")))
(def form-group-row (merge form-group-base
                           (base/flex-container "row")
                           {:flex-wrap "wrap" :align-items "center"}))

(def form-group-container {:padding-bottom "1rem" :width "100%"})

(def form-card {:background-color "#fff"
                :box-shadow "rgba(0, 0, 0, 0.12) 0px 1px 6px, rgba(0, 0, 0, 0.12) 0px 1px 4px"})

(def form-card-label {:padding "1rem 1rem"
                      :font-size "1.125em"
                      :font-weight "bold"
                      :color "#fff"
                      :background-color "#06c"})

(def form-card-body {:padding "1rem 1rem"
                     :font-size "1em"
                     :color "#444444"})

(def form-info-text {:display "inline-block"
                     :position "relative"
                     :top "-0.5em"})

(def full-width {:width "100%"})

(def half-width {:width "50%"})


(def subtitle (merge full-width
                     {:margin "1em 0 0 0.5em"}))
(def subtitle-h {:margin "0"})

(def subheader {:color "#666"
                :margin-top "-0.5rem"})

(def border-color "#C4C4C4")
(def border-right {:border-right (str "solid 2px " border-color)
                   :box-sizing "border-box"
                   :padding-right "20px"})

(def help-icon-element {:padding "0px 0px 0px 10px"})
(def help-text-element {:padding "0" :line-height "21px"})

(def organization-padding {:padding-top "20px"})

(def padding-top {:padding-top "2rem"})

(def action-control-section-margin {:margin-top "2rem"})

(def input-element-wrapper-div {:width "100%"
                                :height "72px"
                                :display "inline-block"
                                :position "relative"
                                :background-color "transparent"
                                :font-family "Public Sans, sans-serif"
                                :transition " height 200ms cubic-bezier (0.23, 1, 0.32, 1) 0ms"
                                :cursor "auto"})

(def input-element-label {:position "absolute"
                          :font-size "12px"
                          :line-height "22px"
                          :top "14px"
                          :transition "all 450ms cubic-bezier (0.23, 1, 0.32, 1) 0ms"
                          :z-index 1
                          :transform "scale (0.75) translate (0px, -28px)"
                          :transform-origin "left top"
                          :pointer-events "none"
                          :user-select "none"
                          :color "rgb (33, 33, 33)"})

