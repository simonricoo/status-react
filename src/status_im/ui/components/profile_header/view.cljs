(ns status-im.ui.components.profile-header.view
  (:require [quo.core :as quo]
            [quo.animated :as animated]
            [quo.design-system.spacing :as spacing]
            [quo.design-system.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.chat-icon.screen :as chat-icon.screen]))

(def avatar-extended-size 64)
(def avatar-minimized-size 40)
(def subtitle-margin 4)

(defn container-style [{:keys [animation minimized style]}]
  (merge {:flex-direction :row
          :align-items    :center}
         (if-not minimized
           (:base spacing/padding-horizontal)
           {:opacity   animation
            :transform [{:translateY
                         (animated/b-interpolate animation 4 0)}]})))

(defn header-bottom-separator []
  {:margin-bottom       (:tiny spacing/spacing)
   :height              (:small spacing/spacing)
   :border-bottom-width 1
   :border-bottom-color (:ui-02 @colors/theme)})

(defn header-text []
  {:padding-left     (:base spacing/spacing)
   :flex             1
   :justify-content  :center})

(defn header-subtitle [{:keys [minimized]}]
  (merge {:padding-right  (:large spacing/spacing)
          :flex-direction :row
          :align-items    :center}
         (when-not minimized
           {:padding-top    subtitle-margin})))

(defn extended-header [{:keys [title photo color subtitle subtitle-icon]}]
  (fn [{:keys [animation minimized]}]
    [animated/view {:pointer-events :box-none}
     [animated/view {:style          (container-style {:animation animation
                                                       :minimized minimized})
                     :pointer-events :box-none}
      [animated/view
       [chat-icon.screen/profile-icon-view
        photo title color nil
        (if minimized avatar-minimized-size avatar-extended-size)
        nil]]
      [animated/view {:style (header-text)}
       [quo/text {:animated?       true
                  :number-of-lines (if minimized 1 2)
                  :size            (if minimized :base :x-large)
                  :weight          :bold
                  :elipsize-mode   :tail}
        title]
       (when subtitle
         [animated/view {:style (header-subtitle {:minimized minimized})}
          (when subtitle-icon
            [vector-icons/icon subtitle-icon {:color           (:icon-02 @colors/theme)
                                              :width           16
                                              :height          16
                                              :container-style {:margin-right 4}}])
          [quo/text {:number-of-lines 1
                     :ellipsize-mode  :middle
                     :size            (if minimized :small :base)
                     :color           :secondary}
           subtitle]])]]
     (when-not minimized
       [animated/view {:style (header-bottom-separator)}])]))

