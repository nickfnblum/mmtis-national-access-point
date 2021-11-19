(ns ote.views.footer
  "NAP - footer"
  (:require [ote.style.base :as style-base]
            [ote.localization :refer [tr tr-key] :as localization]
            [ote.ui.common :refer [linkify]]
            [cljs-react-material-ui.icons :as ic]
            [stylefy.core :as stylefy]
            [ote.style.front-page :as style-front-page]
            [ote.app.controller.front-page :as fp-controller]
            [ote.app.controller.flags :as flags]))

(def selectable-languages [["fi" "Suomeksi"]
                           ["sv" "På Svenska"]
                           ["en" "In English"]])

(defn footer [e!]
  [:div
   [:footer.site-footer
    [:div.container {:style {:margin-bottom "60px"}}
     [:div.col-xs-12.col-sm-4.col-md-4 (stylefy/use-style style-front-page/footer-3-container)
      [:ul.unstyled
       [:li (stylefy/use-style style-front-page/third-column-text)
        [:a.logo {:href (tr [:common-texts :footer-livi-url-link])}
             [:img {:class (:class (stylefy/use-style style-front-page/footer-logo)) :src "/img/icons/TRAFICOM_rgb.svg" :alt (tr [:common-texts :footer-livi-logo])}]]]
       [:li (stylefy/use-style style-front-page/third-column-text)
        [:a.logo {:href "#"}
             [:img {:style {:width "120px"} :src "/img/icons/nap-logo.svg" :alt "NAP"}]]]]]
     [:div.col-xs-12.col-sm-4.col-md-4 (stylefy/use-style style-front-page/footer-3-container)
      [:ul.unstyled {:style {:font-size "0.875em"}}
       [:li [linkify (tr [:common-texts :user-menu-nap-help-link])
             [:div {:style {:height "30px"}} [:span [ic/notification-sms-failed {:style style-front-page/footer-small-icon}] (tr [:common-texts :user-menu-nap-help])]] {:target "_blank"}]]
       [:li [linkify "https://github.com/finnishtransportagency/mmtis-national-access-point/blob/master/docs/api/README.md"
             [:div {:style {:height "30px"}} [:span [ic/action-code {:style style-front-page/footer-small-icon}] (tr [:common-texts :navigation-for-developers])]] {:target "_blank"}]]
       [:li [linkify (tr [:common-texts :navigation-feedback-link])
             [:div {:style {:height "30px"}} [:span [ic/action-description {:style style-front-page/footer-small-icon}] (tr [:common-texts :navigation-give-feedback])]] {:target "_blank"}]
        [:span (stylefy/use-style style-front-page/footer-gray-info-text)
         (tr [:common-texts :navigation-feedback-email])]]
       (when (flags/enabled? :terms-of-service)
         [:li [linkify (tr [:common-texts :navigation-terms-of-service-url])
               [:div {:style {:height "30px"}} [:span [ic/action-description {:style style-front-page/footer-small-icon}] (tr [:common-texts :navigation-terms-of-service-text])]] {:target "_blank"}]])
       [:li [linkify (tr [:common-texts :navigation-privacy-policy-url])
             [:div {:style {:height "30px"}} [:span [ic/action-description {:style style-front-page/footer-small-icon}] (tr [:common-texts :navigation-privacy-policy])]] {:target "_blank"}]]
       [:li [linkify (tr [:common-texts :footer-livi-url-link])
             [:div {:style {:height "30px"}} [:span [ic/action-open-in-new {:style style-front-page/footer-small-icon}] (tr [:common-texts :footer-livi-url])]] {:target "_blank"}]]]]
     [:div.col-xs-12.col-sm-4.col-md-4 (stylefy/use-style style-front-page/footer-3-container)
      [:ul.unstyled
       [:li (stylefy/use-style style-front-page/third-column-text) [:div {:style {:display "flex"}} [:img {:style {:width "80px" :height "52px" :margin-right "20px"} :src "/img/EU-logo.svg"}] (tr [:common-texts :footer-funded])]]
       [:li (stylefy/use-style style-front-page/third-column-text) [:div {:style {:display "flex"}} [:img {:style {:width "60px" :height "60px" :margin-right "40px"} :src "/img/icons/cc.svg"}] (tr [:common-texts :footer-copyright-disclaimer])]]]]]]])
