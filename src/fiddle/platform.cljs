(ns fiddle.platform
  (:require [fiddle.react-native :as rn]))

(def os (when rn/platform (.-OS rn/platform)))

(def android? (= os "android"))
(def ios? (= os "ios"))
