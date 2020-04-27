(ns quo.previews.text-input
  (:require [quo.core :as quo]
            [quo.react-native :as rn]))

(defn preview-text []
  [rn/scroll-view {:flex               1
                   :padding-horizontal 16}
   (for [multiline     [false true]
         label         [nil "Input label"]
         default-value [nil "Test initial value"]
         placeholder   [nil "Placeholder value"]
         before        [nil {:icon :main-icons/search}]
         after         [nil {:icon :main-icons/show}]
         error         [nil "Something went wrong!"]]
     ^{:key (str multiline label default-value placeholder)}
     [rn/view {:padding-vertical 16}
      [quo/text-input {:label         label
                          :default-value default-value
                          :placeholder   placeholder
                          :multiline     multiline
                          :before        before
                          :after         after
                          :error         error}]])])
