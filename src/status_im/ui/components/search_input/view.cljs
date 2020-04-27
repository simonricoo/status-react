(ns status-im.ui.components.search-input.view
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [reagent.core :as reagent]
            [status-im.i18n :as i18n]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.search-input.styles :as styles]
            [status-im.ui.components.icons.vector-icons :as icons]))

(defn search-input [{:keys [search-active?]}]
  (let [input-ref      (atom nil)
        search-active? (or search-active? (reagent/atom nil))]
    (fn [{:keys [on-cancel on-focus on-change
                 search-container-style search-filter auto-focus]}]
      [react/view {:style (or search-container-style (styles/search-container))}
       [react/view {:style (styles/search-input-container)}
        [icons/icon :main-icons/search {:color           colors/gray
                                        :container-style {:margin-left  6
                                                          :margin-right 2}}]
        [react/text-input {:placeholder     (i18n/label :t/search)
                           :blur-on-submit  true
                           :multiline       false
                           :ref             #(reset! input-ref %)
                           :style           styles/search-input
                           :default-value   search-filter
                           :auto-focus      auto-focus
                           :auto-correct    false
                           :auto-capitalize :none
                           :on-focus        #(do
                                               (when on-focus
                                                 (on-focus search-filter))
                                               (reset! search-active? true))
                           :on-change       (fn [e]
                                              (let [^js native-event (.-nativeEvent ^js e)
                                                    text         (.-text native-event)]
                                                (when on-change
                                                  (on-change text))))}]]
       (when @search-active?
         [react/touchable-highlight
          {:on-press (fn []
                       (.clear ^js @input-ref)
                       (.blur ^js @input-ref)
                       (when on-cancel (on-cancel))
                       (reset! search-active? false))
           :style    {:margin-left 16}}
          [react/text {:style {:color colors/blue}}
           (i18n/label :t/cancel)]])])))
