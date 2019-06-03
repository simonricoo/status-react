(ns status-im.ui.screens.keycard.onboarding.views
  (:require [status-im.ui.components.react :as react]
            [status-im.ui.screens.keycard.onboarding.styles :as styles]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.i18n :as i18n]
            [re-frame.core :as re-frame]
            [status-im.react-native.resources :as resources]
            [status-im.ui.components.common.common :as components.common]
            [status-im.ui.components.styles :as components.styles]))

(defn intro []
  [react/view styles/container
   [toolbar/toolbar
    {:transparent? true
     :style        {:margin-top 32}}
    toolbar/default-nav-back
    nil]
   [react/view {:flex            1
                :flex-direction  :column
                :justify-content :space-between
                :align-items     :center}
    [react/view {:flex-direction :column
                 :align-items    :center}
     [react/view {:margin-top 16}
      [react/text {:style {:typography :header}}
       (i18n/label :t/keycard-onboarding-intro-header)]]
     [react/view {:margin-top 16
                  :width      311}
      [react/text {:style {:font-size   15
                           :line-height 22
                           :color       colors/gray
                           :text-align  :center}}
       (i18n/label :t/keycard-onboarding-intro-text)]]
     [react/view {:margin-top 33}
      [react/touchable-highlight {:on-press #(.openURL (react/linking) "https://keycard.status.im")}
       [react/view {:flex-direction  :row
                    :align-items     :center
                    :justify-content :center}
        [react/text {:style {:text-align :center
                             :color      colors/blue}}
         (i18n/label :t/learn-more-about-keycard)]
        [vector-icons/icon :tiny-icons/tiny-external {:color           colors/blue
                                                      :container-style {:margin-left 5}}]]]]]
    [react/view
     [react/view {:align-items     :center
                  :justify-content :center}
      [react/image {:source (resources/get-image :keycard)
                    :style  {:width  144
                             :height 114}}]]]
    [react/view {:margin-bottom 50}
     [react/touchable-highlight
      {:on-press #(re-frame/dispatch [:keycard.onboarding.intro.ui/begin-setup-pressed])}
      [react/view {:background-color colors/gray-background
                   :align-items      :center
                   :justify-content  :center
                   :flex-direction   :row
                   :width            133
                   :height           44
                   :border-radius    10}
       [react/text {:style {:color colors/blue}}
        (i18n/label :t/begin-set-up)]]]]]])

(defn start []
  [react/view styles/container
   [toolbar/toolbar
    {:transparent? true
     :style        {:margin-top 32}}
    toolbar/default-nav-back
    nil]
   [react/view {:flex            1
                :flex-direction  :column
                :justify-content :space-between
                :align-items     :center}
    [react/view {:flex-direction :column
                 :align-items    :center}
     [react/view {:margin-top 16}
      [react/text {:number-of-lines 2
                   :style           {:typography :header
                                     :text-align :center}}
       (i18n/label :t/keycard-onboarding-start-header)]]
     [react/view {:margin-top 16
                  :width      311}
      [react/text {:number-of-lines 3
                   :style           {:font-size   15
                                     :line-height 22
                                     :color       colors/gray
                                     :text-align  :center}}
       (i18n/label :t/keycard-onboarding-start-text)]]
     [react/view {:margin-top 20
                  :width      "80%"}
      (for [[number header text] [["1"
                                   (i18n/label :t/keycard-onboarding-start-step1)
                                   (i18n/label :t/keycard-onboarding-start-step1-text)]
                                  ["2"
                                   (i18n/label :t/keycard-onboarding-start-step2)
                                   (i18n/label :t/keycard-onboarding-start-step2-text)]
                                  ["3"
                                   (i18n/label :t/keycard-onboarding-start-step3)
                                   (i18n/label :t/keycard-onboarding-start-step3-text)]]]
        ^{:key number} [react/view {:flex-direction :row
                                    :margin-top     15}
                        [react/view {:border-width    1
                                     :border-radius   20
                                     :border-color    colors/gray-light
                                     :align-items     :center
                                     :justify-content :center
                                     :width           40
                                     :height          40}
                         [react/text {:style {:typography :title}}
                          number]]
                        [react/view {:align-items     :flex-start
                                     :justify-content :flex-start
                                     :margin-left     11}
                         [react/view
                          [react/text {:style {:typography :main-medium}}
                           header]]
                         [react/view
                          [react/text {:number-of-lines 2
                                       :style           {:color         colors/gray
                                                         :padding-right 35}}
                           text]]]])]]
    [react/view {:margin-bottom   12
                 :align-items     :center
                 :justify-content :center}
     [react/image {:source      (resources/get-image :keycard-phone)
                   :resize-mode :center
                   :style       {:width  160
                                 :height 170}}]]]])

(defn puk-code []
  [react/view styles/container
   [toolbar/toolbar
    {:transparent? true
     :style        {:margin-top 32}}
    [toolbar/nav-text
     {:handler #(re-frame/dispatch [:keycard.onboarding.puk-code.ui/cancel-pressed])
      :style   {:padding-left 21}}
     (i18n/label :t/cancel)]
    [react/text {:style {:color colors/gray}}
     "Step 2 of 3"]]
   [react/view {:flex            1
                :flex-direction  :column
                :justify-content :space-between
                :align-items     :center}
    [react/view {:flex-direction :column
                 :align-items    :center}
     [react/view {:margin-top 16}
      [react/text {:number-of-lines 2
                   :style           {:typography :header
                                     :text-align :center}}
       (i18n/label :t/keycard-onboarding-puk-code-header)]]
     [react/view {:margin-top 32
                  :width      "85%"}
      [react/view {:justify-content :center
                   :flex-direction  :row}
       [react/view {:width             "100%"
                    :margin-horizontal 16
                    :height            108
                    :align-items       :center
                    :justify-content   :space-between
                    :flex-direction    :column
                    :background-color  colors/gray-lighter
                    :border-radius     8}
        [react/view {:justify-content :center
                     :flex            1
                     :margin-top      10}
         [react/text {:style {:color      colors/gray
                              :text-align :center}}
          (i18n/label :t/puk-code)]]
        [react/view {:justify-content :flex-start
                     :flex            1}
         [react/text {:style {:typography :header
                              :text-align :center
                              :color      colors/blue}}
          "123456"]]]]
      [react/view {:margin-top 16}
       [react/text {:style           {:color colors/gray}
                    :number-of-lines 3}
        (i18n/label :t/puk-code-explanation)]]
      [react/view {:justify-content :center
                   :margin-top      32
                   :flex-direction  :row}
       [react/view {:width             "100%"
                    :margin-horizontal 16
                    :height            108
                    :align-items       :center
                    :justify-content   :space-between
                    :flex-direction    :column
                    :background-color  colors/gray-lighter
                    :border-radius     8}
        [react/view {:justify-content :center
                     :flex            1
                     :margin-top      10}
         [react/text {:style {:color      colors/gray
                              :text-align :center}}
          (i18n/label :t/puk-code)]]
        [react/view {:justify-content :flex-start
                     :flex            1}
         [react/text {:style {:typography :header
                              :text-align :center
                              :color      colors/blue}}
          "abcdfg"]]]]
      [react/view {:margin-top 16}
       [react/text {:style           {:color colors/gray}
                    :number-of-lines 2}
        (i18n/label :t/pair-code-explanation)]]]]
    [react/view {:flex-direction  :row
                 :justify-content :space-between
                 :align-items     :center
                 :width           "100%"
                 :height          86}
     [react/view components.styles/flex]
     [react/view {:margin-right 20}
      [components.common/bottom-button
       {:on-press #(re-frame/dispatch [:keycard.onboarding.puk-code.ui/next-pressed])
        :forward? true}]]]]])

(defn- loading [title-label]
  [react/view styles/container
   [toolbar/toolbar {:transparent? true
                     :style        {:margin-top 32}}
    nil nil]
   [react/view {:flex            1
                :flex-direction  :column
                :justify-content :space-between
                :align-items     :center}
    [react/view {:flex-direction :column
                 :align-items    :center}
     [react/view {:margin-top 16}
      [react/activity-indicator {:animating true
                                 :size      :large}]]
     [react/view {:margin-top 16}
      [react/text {:number-of-lines 2
                   :style           {:typography :header
                                     :text-align :center}}
       (i18n/label title-label)]]
     [react/view {:margin-top 16
                  :width      311}
      [react/text {:number-of-lines 3
                   :style           {:font-size   15
                                     :line-height 22
                                     :color       colors/gray
                                     :text-align  :center}}
       (i18n/label :t/this-will-take-few-seconds)]]]
    [react/view {:flex            1
                 :align-items     :center
                 :justify-content :center}
     [react/image {:source      (resources/get-image :keycard-phone)
                   :resize-mode :center
                   :style       {:width  160
                                 :height 170}}]
     [react/view {:margin-top 10}
      [react/text {:style {:text-align  :center
                           :color       colors/gray
                           :font-size   15
                           :line-height 22}}
       (i18n/label :t/hold-card)]]]]])

(defn preparing []
  (loading :t/keycard-onboarding-preparing-header))

(defn pairing []
  (loading :t/keycard-onboarding-pairing-header))

(defn finishing []
  (loading :t/keycard-onboarding-finishing-header))
