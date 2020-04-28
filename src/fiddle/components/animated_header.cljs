(ns fiddle.components.animated-header
  (:require [oops.core :refer [oget]]
            [reagent.core :as reagent]
            [fiddle.react-native :as rn]
            [fiddle.platform :as platform]
            [fiddle.components.safe-area :as safe-area]
            [fiddle.animated :as animated]
            [fiddle.components.header :as header]))

(defn header-wrapper-style [{:keys [value offset]}]
  (merge
   {:background-color :white}
   (when (and offset platform/android?)
     {:elevation (animated/interpolate
                  value
                  {:inputRange  [0 offset]
                   :outputRange [0 4]
                   :extrapolate (:clamp animated/extrapolate)})})
   (when (and offset platform/ios?)
     {:shadow-opacity (animated/interpolate
                       value
                       {:inputRange  [0 offset]
                        :outputRange [0 1]
                        :extrapolate (:clamp animated/extrapolate)})
      :shadow-radius  16
      :z-index        2
      :shadow-color   (:shadow-01 @colors/theme)
      :shadow-offset  {:width 0 :height 4}})))

(defn header-opened-style [{:keys [value offset]}]
  (merge
   {:position :absolute
    :top      0
    :left     0
    :right    0}
   (when offset
     {:transform [{:translateY
                   (animated/interpolate
                    value
                    {:inputRange       [0 offset]
                     :outputRange      [0 (- header/header-height)]
                     :extrapolateRight (:clamp animated/extrapolate)})}]})))

(defn header-container []
  (let [y         (animated/value 0)
        on-scroll (animated/on-scroll {:y y})
        layout    (reagent/atom {})
        offset    (reagent/atom 0)
        on-layout (fn [evt]
                    (reset! offset (oget evt "nativeEvent" "layout" "height")))]
    (fn [{:keys [extended-header] :as props} & children]
      [animated/view {:flex           1
                      :pointer-events :box-none}
       [animated/view {:pointer-events :box-none
                       :style          (header-wrapper-style {:value  y
                                                              :offset @offset})}
        [header/header (merge {:get-layout (fn [el l] (swap! layout assoc el l))}
                              (dissoc props :extended-header))]
        [rn/view {:pointer-events :box-none}
         [animated/view {:style          (header-opened-style {:value  y
                                                               :offset @offset})
                         :pointer-events :box-none
                         :on-layout      on-layout}
          [extended-header {:value  y
                            :layout @layout
                            :offset @offset}]]]]
       (into [animated/scroll-view {:on-scroll           on-scroll
                                    :scrollEventThrottle 1}
              [rn/view {:pointer-events :box-none
                        :height         @offset}]]
             children)])))

(defn header [{:keys [use-insets] :as props} & children]
  (if use-insets
    [safe-area/consumer
     (fn [insets]
       [header-container (-> props
                             (dissoc :use-insets)
                             (assoc :insets insets))
        children])]
    [header-container props children]))
