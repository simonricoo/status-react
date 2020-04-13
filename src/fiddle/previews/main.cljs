(ns fiddle.previews.main
  (:require [reagent.core :as reagent]
            [oops.core :refer [ocall]]
            [fiddle.react-native :as rn]
            [fiddle.previews.text :as text]
            [fiddle.previews.header :as header]
            [status-im.react-native.js-dependencies :refer [react-native]]
            [status-im.ui.screens.routing.core :as navigation]))

(def screens [{:name      :texts
               :insets    {:top false}
               :component text/preview-text}
              {:name      :headers
               :insets    {:top false}
               :component header/preview-header}])

(defn main-screen []
  [rn/scroll-view {:flex               1
                   :padding-vertical   8
                   :padding-horizontal 16}
   [rn/view
    (for [{:keys [name]} screens]
      [rn/touchable-opacity {:on-press #(navigation/navigate-to name nil)}
       [rn/view {:style {:padding-vertical 8}}
        [rn/text (str "Preview " name)]]])]])

(defn preview-screens []
  (let [stack (navigation/create-stack)]
    [navigation/navigation-container
     {:ref navigation/set-navigator-ref}
     [stack {}
      (into [{:name      :main
              :insets    {:top false}
              :component main-screen}]
            screens)]]))

;; TODO(Ferossgp): Add separate build when shadow-cljs will be integrated
;; NOTE(Ferossgp): Separate app can be used to preview all available
;; and possible state for componetns, and for UI testing based on screenshots
(defn init []
  (ocall react-native ["AppRegistry" "registerComponent"]
         "StatusIm"
         #(reagent/reactify-component preview-screens)))
