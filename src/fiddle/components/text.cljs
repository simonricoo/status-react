(ns fiddle.components.text
  (:require [reagent.core :as reagent]
            [fiddle.react-native :as rn]
            ;; TODO(Ferossgp): Move colors into fiddle
            [status-im.ui.components.colors :as colors]
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
           :link {:color colors/blue}
           :main {:color colors/black})
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
