(ns fiddle.core
  (:require [fiddle.components.text :as text]
            [fiddle.components.header :as header]
            [fiddle.components.safe-area :as safe-area]))

(def text text/text)
(def header header/header)

(def safe-area-provider safe-area/provider)
(def safe-area-consumer safe-area/consumer)
(def safe-area-view safe-area/view)
