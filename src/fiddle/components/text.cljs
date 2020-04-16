(ns fiddle.components.text
  (:require [reagent.core :as reagent]
            [fiddle.react-native :as rn]
            [fiddle.design-system.colors :as colors]
            [fiddle.design-system.typography :as typography]
            [fiddle.animated :as animated]))

(defn text-style [{:keys [size align weight color style]
                   :or   {size   :base
                          weight :regular
                          align  :auto
                          color  :main}}]
  (merge (case weight
           :regular   typography/font-regular
           :medium    typography/font-medium
           :semi-bold typography/font-semi-bold
           :bold      typography/font-bold)
         (case color
           :main              {:color (:text-01 @colors/theme)}
           :secondary         {:color (:text-02 @colors/theme)}
           :secondary-inverse {:color (:text-03 @colors/theme)}
           :link              {:color (:text-04 @colors/theme)}
           :positive          {:color (:positive-01 @colors/theme)}
           :negative          {:color (:negative-01 @colors/theme)})
         (case size
           :tiny     typography/tiny
           :small    typography/small
           :base     typography/base
           :large    typography/large
           :x-large  typography/x-large
           :xx-large typography/xx-large)
         {:text-align align}
         style))

(defn text []
  (let [this      (reagent/current-component)
        props     (reagent/props this)
        component (if (:animated? props) animated/text rn/text)]
    (into [component (merge {:style (text-style props)}
                            (dissoc props
                                    :style :size :weight :color
                                    :align :animated?))]
          (reagent/children this))))
