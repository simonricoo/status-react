(ns fiddle.components.animated
  (:require [reagent.core :as reagent]
            [oops.core :refer [oget]]
            [status-im.react-native.js-dependencies :as js-deps]))

(def animated (oget js-deps/react-native-reanimated "default"))

(def view (reagent/adapt-react-class (oget animated "View")))
(def text (reagent/adapt-react-class (oget animated "Text")))
