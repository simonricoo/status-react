(ns status-im.ios.core
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            status-im.utils.db
            status-im.ui.screens.db
            status-im.ui.screens.events
            status-im.subs
            [status-im.react-native.js-dependencies :as rn-dependencies]
            [oops.core :refer [ocall]]
            [status-im.ui.screens.views :as views]
            [status-im.ui.components.react :as react]
            [status-im.core :as core]
            [status-im.utils.snoopy :as snoopy]
            [status-im.i18n :as i18n]))

(defn app-state-change-handler [state]
  (dispatch [:app-state-change state]))

(defn on-languages-change [event]
  (i18n/set-language (.-language event)))

(defn on-shake []
  (dispatch [:shake-event]))

(defn app-root [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (.addListener react/keyboard
                    "keyboardWillShow"
                    (fn [e]
                      (let [h (.. e -endCoordinates -height)]
                        (dispatch-sync [:set :keyboard-height h])
                        (dispatch-sync [:set :keyboard-max-height h]))))
      (.addListener react/keyboard
                    "keyboardWillHide"
                    #(dispatch-sync [:set :keyboard-height 0]))
      (.hide react/splash-screen)
      (.addEventListener react/app-state "change" app-state-change-handler)
      (.addEventListener rn-dependencies/react-native-languages "change" on-languages-change)
      (.addEventListener rn-dependencies/react-native-shake
                         "ShakeEvent"
                         on-shake)
      (dispatch [:set-initial-props (reagent/props this)]))
    :component-will-unmount
    (fn []
      (.removeEventListener react/app-state "change" app-state-change-handler)
      (.removeEventListener rn-dependencies/react-native-languages "change" on-languages-change))
    :display-name "root"
    :reagent-render views/main}))

(defn init []
  (ocall rn-dependencies/react-native-screens "enableScreens")
  (core/init app-root)
  (snoopy/subscribe!))
