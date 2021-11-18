(ns ote.style.transit-changes
  "Transit changes styling"
  (:require [stylefy.core :as stylefy]
            [ote.theme.colors :as colors]
            [ote.theme.screen-sizes :refer [width-xxs width-xs width-sm width-md width-l width-xl]]))

(def transit-changes-legend-container
  {:background-color colors/gray300
   :margin-top "1rem"
   :padding "1rem"})

(def transit-changes-icon-legend-row-container
  {:display "flex"
   :width "100%"
   :flex-wrap "wrap"})

(def transit-changes-icon-row-container
  {:align-items "center"
   :display "flex"
   :flex-direction "row"
   :flex-wrap "nowrap"
   :justify-content "flex-start"})

(def transit-changes-legend-icon
  {:margin-left "1rem"})

(def transit-changes-icon
  {:margin-right "0.25rem"})

(def change-icon-value
  {:display "inline-block"
   :position "relative"
   :top "-0.5rem"
   :left "0.2rem"})

(def date1-highlight-color "rgba(53,140,217,1)")
(def date2-highlight-color "rgba(219,25,169,1)")
(def date-highlight-color-hover colors/gray650)


(defn date-highlight-style
  ([]
   (date-highlight-style "rgba(0,0,0,0)"))
  ([hash-color]
   (date-highlight-style hash-color date2-highlight-color))
  ([hash-color highlight-color]
   {:background (str "radial-gradient(circle at center, " highlight-color " 50%, " (or hash-color "#FFF") " 40%) 0px 0px")
    :color "#fff"}))

(defn date2-highlight-style
  ([]
   (date2-highlight-style "rgba(0,0,0,0)"))
  ([hash-color]
   (date2-highlight-style hash-color date1-highlight-color))
  ([hash-color highlight-color]
   {:background (str "radial-gradient(circle at center, " highlight-color " 50%, " (or hash-color "#FFF") " 40%) 0px 0px")
    :color "#E1E1F9"}))

(defn date1-highlight-style
  ([]
   (date1-highlight-style "rgba(0,0,0,0)"))
  ([hash-color]
   (date1-highlight-style hash-color date2-highlight-color))
  ([hash-color highlight-color]
   {:background (str "radial-gradient(circle at center, " highlight-color " 50%, " (or hash-color "#FFF") " 40%) 0px 0px")
    :color "#F6C6EA"}))

(def map-different-date-container {:position "relative"
                                   :margin-right "0.5em"
                                   :top "-5px"
                                   :color "red"
                                   :display "inline-block"
                                   :width "20px"})

(def map-different-date1 (merge map-different-date-container
                                {:border-bottom (str "solid 3px " date2-highlight-color)}))
(def map-different-date2 (merge map-different-date-container
                                {:border-bottom (str "solid 3px " date1-highlight-color)}))

(def section
  {:border "solid 1px #646464"
   :padding-bottom "1.25rem"
   :margin-bottom "2.5rem"})

(def section-closed
  (dissoc section :padding-bottom))

(def section-title
  {:background-color "#646464"
   :color "white"
   :font-size "1.125rem"
   :font-family "Public Sans"
   :padding "0.875rem 0.875rem 0.875rem 0.625rem"
   :line-height "2rem"
   :font-weight "600"})

(def infobox {:background-color "#FAFAFA"
              :border "solid 1px #e1e1e1"
              :margin-bottom "1.25rem"
              :padding "0.7rem 0.875rem 0.75rem"
              :font-size "0.875rem"
              :font-family "Public Sans, sans-serif"
              :color "#505050"})

(def infobox-text {:margin-bottom "0.25rem"
                   :line-height "1.5em"})

(def infobox-more-link {:color "#06c"
                        :font-weight 400
                        :text-decoration "none"
                        :display "flex"
                        :flex-direction "row"
                        :align-items "flex-start"
                        :justify-content "flex-start"})

(def section-header
  {:padding "1rem"
   :background-color "#F0F0F0"
   :line-height "1.5rem"})

(def section-body
  {:padding-left "1.25rem"
   :padding-right "1.25rem"})

(def map-checkbox-container {:margin-top "0.5rem"
                             :display "flex"
                             :flex-direction "row-reverse"
                             :flex-wrap "nowrap"
                             :justify-content "space-between"
                             ::stylefy/media {{:max-width (str width-xs "px")} {:flex-direction "column"}}})

(def new-change-indicator
  {:height "6px"
   :width "6px"
   :background-color colors/primary
   :border-radius "50%"})

(def new-change-container
  {:height "100%"
   :width "100%"
   :display "flex"
   :justify-content "center"
   :align-items "center"})

(def new-change-legend-icon
  {:height "20px"
   :width "20px"
   :display "flex"
   :justify-content "center"
   :align-items "center"})

(def gtfs-package-info-icons {:height 18
                              :color colors/negative-button})
