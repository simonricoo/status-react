(ns quo.previews.tooltip
  (:require [quo.core :as quo]
            [quo.react-native :as rn]))

(defn preview-tooltip []
  [rn/scroll-view {:flex               1
                   :padding-horizontal 16}
   (for [children [[quo/text {:size :small} "Simple text"]
                   [quo/text {:color :negative
                                 :size  :small}
                    "Error text"]
                   [rn/view {:width 100 :height 20 :background-color :red}]
                   [quo/text "Just text, but long. Officia autem est repellendus ad quia exercitationem veniam."]]]
     ^{:key (str children)}
     [rn/view {:margin-vertical 50}
      [rn/view {:height           20
                :background-color "rgba(0,0,0,0.1)"}]
      [quo/tooltip {}
       children]])])
