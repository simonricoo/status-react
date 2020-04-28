(ns quo.components.header
  (:require [oops.core :refer [oget]]
            [quo.animated :as animated]
            [quo.components.text :as text]
            [quo.design-system.colors :as colors]
            [quo.design-system.spacing :as spacing]
            [quo.react :as react]
            [quo.react-native :as rn]
            [reagent.core :as reagent]
            [status-im.ui.components.icons.vector-icons :as icons]))

(def header-height 56)

(defn header-wrapper-style [{:keys [height border-bottom]}]
  (merge
   {:background-color (:ui-background @colors/theme)
    :height           height}
   (when border-bottom
     {:border-bottom-width 1
      :border-bottom-color (:ui-02 @colors/theme)})))

(def absolute-fill {:position :absolute
                    :top      0
                    :bottom   0
                    :left     0
                    :right    0})

(def content  {:flex            1
               :flex-direction  :row
               :align-items     :center
               :justify-content :center})

(def left {:position        :absolute
           :left            0
           :top             0
           :bottom          0
           :justify-content :center
           :align-items     :flex-start})

(def right {:position        :absolute
            :right           0
            :top             0
            :bottom          0
            :justify-content :center
            :align-items     :flex-end})

(defn title-style [{:keys [left]} title-align]
  (merge
   {:position        :absolute
    :justify-content :center
    :top             0
    :bottom          0}
   (:tiny spacing/padding-horizontal)
   (case title-align
     :left {:left (:width left)}
     {:align-items :center})))

(def header-actions-style
  (merge
   {:flex               1
    :flex-direction     :row
    :align-items        :center
    :justify-content    :center}
   (:tiny spacing/padding-horizontal)))

(def header-action-placeholder
  {:width (:tiny spacing/spacing)})

(def header-icon-touchable
  (merge
   {:flex               1
    :align-items        :center
    :justify-content    :center}
   (:tiny spacing/padding-horizontal)))

(def element {:align-items        :center
              :justify-content    :center
              :flex               1})

(defn header-action [{:keys [icon label on-press accessibility-label]}]
  [rn/touchable-opacity {:on-press on-press}
   [rn/view (merge {:style header-icon-touchable}
                   (when accessibility-label
                     {:accessibility-label accessibility-label}))
    (cond
      icon  [icons/icon icon]
      label [text/text {:color :link} label])]])

(defn header-actions [{:keys [accessories component]}]
  [rn/view {:style element}
   (cond
     (seq accessories)
     (into [rn/view {:style header-actions-style}]
           (map header-action accessories))

     component component

     :else
     [rn/view {:style header-action-placeholder}])])

(defn header-title [{:keys [title subtitle component]}]
  [react/fragment
   (cond
     component component

     (and title subtitle)
     [react/fragment
      [text/text {:weight :medium} title]
      [text/text {:weight :regular} subtitle]]

     title [text/text {:weight :bold
                       :size   :large}
            title])])

(defn header []
  (let [layout        (reagent/atom {:left  {:width  8
                                             :height header-height}
                                     :right {:width  8
                                             :height header-height}
                                     :title {:width  0
                                             :height header-height}})
        handle-layout (fn [el get-layout]
                        (fn [evt]
                          (let [width  (oget evt "nativeEvent" "layout" "width")
                                height (oget evt "nativeEvent" "layout" "height")]
                            (when get-layout
                              (get-layout el {:width  width
                                              :height height}))
                            (swap! layout assoc el {:width  width
                                                    :height height}))))]
    (fn [{:keys [left-accessories left-component border-bottom
                 right-accessories right-component insets get-layout
                 title subtitle title-component style title-align]
          :or  {title-align :center}}]
      (let [status-bar-height (get insets :top 0)
            height            (+ header-height status-bar-height)]
        [animated/view {:style (header-wrapper-style {:height        height
                                                      :border-bottom border-bottom})}
         [rn/view {:pointer-events :box-none
                   :height         status-bar-height}]
         [rn/view {:style          (merge {:height header-height}
                                          style)
                   :pointer-events :box-none}
          [rn/view {:style          absolute-fill
                    :pointer-events :box-none}
           [rn/view {:style          content
                     :pointer-events :box-none}
            [rn/view {:style          left
                      :on-layout      (handle-layout :left get-layout)
                      :pointer-events :box-none}
             [header-actions {:accessories left-accessories
                              :component   left-component}]]

            [rn/view {:style          (title-style @layout title-align)
                      :on-layout      (handle-layout :title get-layout)
                      :pointer-events :box-none}
             [header-title {:title     title
                            :subtitle  subtitle
                            :component title-component}]]

            [rn/view {:style          right
                      :on-layout      (handle-layout :right get-layout)
                      :pointer-events :box-none}
             [header-actions {:accessories right-accessories
                              :component   right-component}]]]]]]))))
