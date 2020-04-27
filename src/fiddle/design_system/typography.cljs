(ns fiddle.design-system.typography
  (:require [fiddle.platform :as platform]))

(def tiny {:font-size    10
           :line-height 14})

(def small {:font-size   13
            :line-height 18})

(def base {:font-size   15
           :line-height 22})

(def large {:font-size   17
            :line-height 24})

(def x-large {:font-size   22
              :line-height 30})

(def xx-large {:font-size  28
               :line-height 38})

(def font-regular {:font-family "Inter-Regular"}) ; 400

(def font-medium {:font-family "Inter-Medium"}) ; 500 ff

(def font-semi-bold {:font-family "Inter-SemiBold"}) ; 600

(def font-bold {:font-family "Inter-Bold"}) ; 700

(def monospace (if platform/ios? "Menlo-Regular" "monospace"))
