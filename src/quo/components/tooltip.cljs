(ns quo.components.tooltip
  (:require [reagent.core :as reagent]
            [oops.core :refer [oget]]
            [quo.animated :as animated]
            [quo.react-native :as rn]
            [quo.design-system.colors :as colors]
            [quo.design-system.spacing :as spacing]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [quo.components.text :as text]))

(defn tooltip-style [{:keys [bottom-value animation]}]
  (merge
   (:base spacing/padding-horizontal)
   {:position    :absolute
    :align-items :center
    :left        0
    :right       0
    :top         (- bottom-value)
    :opacity     animation
    ;; TODO: Use spring
    :transform   [{:translateY (animated/interpolate animation
                                                     {:inputRange  [0 1]
                                                      :outputRange [bottom-value 0]})}]}))

(defn container-style []
  {:z-index        2
   :elevation      2
   :align-items    :center
   :shadow-radius  16
   :shadow-opacity 1
   :shadow-color   (:shadow-01 @colors/theme)
   :shadow-offset  {:width 0 :height 4}})

(defn content-style []
  (merge (:base spacing/padding-horizontal)
         {:padding-vertical 6
          :background-color (:ui-background @colors/theme)
          :border-radius    8}))

(defn tooltip []
  (let [layout    (reagent/atom {})
        animation (animated/value 1)
        on-layout (fn [evt]
                    (let [width  (oget evt "nativeEvent" "layout" "width")
                          height (oget evt "nativeEvent" "layout" "height")]
                      (reset! layout {:width  width
                                      :height height})))]
    (fn  [{:keys [bottom-value]} & children]
      [animated/view {:style          (tooltip-style {:bottom-value (- (get @layout :height)
                                                                       bottom-value)
                                                      :animation animation})
                      :pointer-events :box-none}
       [animated/view {:style          (container-style)
                       :pointer-events :box-none}
        (into [rn/view {:style     (content-style)
                        :on-layout on-layout}]
              children)
        [vector-icons/icon :icons/tooltip-tip {:width  18
                                               :height 8
                                               :color  (:ui-background @colors/theme)}]]])))
