(ns quo.core
  (:require [quo.components.animated-header :as animated-header]
            [quo.components.header :as header]
            [quo.components.safe-area :as safe-area]
            [quo.components.text :as text]))

(def text text/text)
(def header header/header)
(def animated-header animated-header/header)

(def safe-area-provider safe-area/provider)
(def safe-area-consumer safe-area/consumer)
(def safe-area-view safe-area/view)
