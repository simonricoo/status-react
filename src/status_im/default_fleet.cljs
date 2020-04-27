(ns status-im.default-fleet
  (:require-macros [status-im.utils.slurp :refer [slurp]]))

(def default-fleets
  (do (println "HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHh")
      (slurp "resources/config/fleets.json")))
