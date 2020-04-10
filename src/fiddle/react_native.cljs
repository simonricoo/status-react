(ns fiddle.react-native
  (:require [oops.core :refer [oget]]
            [reagent.core :as reagent]
            [status-im.react-native.js-dependencies :refer [react-native]]))

(def view (reagent/adapt-react-class (oget react-native "View")))

(def text (reagent/adapt-react-class (oget react-native "Text")))

