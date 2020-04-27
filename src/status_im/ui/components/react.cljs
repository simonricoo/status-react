(ns status-im.ui.components.react
  (:require-macros [status-im.utils.views :as views])
  (:require [reagent.core :as reagent]
            [status-im.ui.components.styles :as styles]
            [status-im.utils.utils :as utils]
            [status-im.utils.core :as utils.core]
            [status-im.utils.platform :as platform]
            [status-im.i18n :as i18n]
            ["react-native" :as react-native :refer (Keyboard)]
            ["react-native-image-crop-picker" :default image-picker]
            ["react-native-gesture-handler" :refer (TouchableWithoutFeedback)]
            ["react-native-safe-area-context" :as safe-area-context
             :refer (SafeAreaView SafeAreaProvider SafeAreaConsumer)]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.typography :as typography]))

(def native-modules (.-NativeModules react-native))
(def device-event-emitter (.-DeviceEventEmitter react-native))

;; React Components

(def app-state (.-AppState react-native))
(def view (reagent/adapt-react-class (.-View react-native)))
(def progress-bar (reagent/adapt-react-class (.-ProgressBarAndroid react-native)))

(def status-bar-class (.-StatusBar react-native))

(def scroll-view-class (reagent/adapt-react-class (.-ScrollView react-native)))
(def keyboard-avoiding-view-class (reagent/adapt-react-class (.-KeyboardAvoidingView react-native)))

(def text-class (reagent/adapt-react-class (.-Text react-native)))
(def text-input-class (reagent/adapt-react-class (.-TextInput react-native)))
(def image-class (reagent/adapt-react-class (.-Image react-native)))
(def picker-class (reagent/adapt-react-class (.-Picker react-native)))
(def picker-item-class (reagent/adapt-react-class (.-Item (.-Picker react-native))))

(defn valid-source? [source]
  (or (not (map? source))
      (not (contains? source :uri))
      (and (contains? source :uri)
           (:uri source))))

(defn image [{:keys [source] :as props}]
  (when (valid-source? source)
    [image-class props]))

(def switch-class (reagent/adapt-react-class (.-Switch react-native)))

(defn switch [props]
  [switch-class props])

(def touchable-highlight-class (reagent/adapt-react-class (.-TouchableHighlight react-native)))
(def touchable-without-feedback-class (reagent/adapt-react-class (.-TouchableWithoutFeedback react-native)))
(def touchable-opacity-class (reagent/adapt-react-class (.-TouchableOpacity react-native)))
(def activity-indicator-class (reagent/adapt-react-class (.-ActivityIndicator react-native)))

(defn activity-indicator [props]
  [activity-indicator-class props])

(defn small-loading-indicator [color]
  [activity-indicator {:color   (if color color :colors/gray)
                       :ios     {:size :small}
                       :android {:size :16}}])

(def modal (reagent/adapt-react-class (.-Modal react-native)))

(def pan-responder (.-PanResponder react-native))
(def animated (.-Animated react-native))

(def animated-view-class
  (reagent/adapt-react-class (.-View ^js animated)))

(def animated-flat-list-class
  (reagent/adapt-react-class (.-FlatList ^js animated)))

(defn animated-view [props & content]
  (vec (conj content props animated-view-class)))

(def dimensions (.-Dimensions react-native))
(def keyboard (.-Keyboard react-native))
(def dismiss-keyboard! #(.dismiss ^js Keyboard))
(def linking (.-Linking react-native))
(def desktop-notification (.-DesktopNotification ^js (.-NativeModules react-native)))

(def max-font-size-multiplier 1.25)

(defn prepare-text-props [props]
  (-> props
      (update :style typography/get-style)
      (assoc :max-font-size-multiplier max-font-size-multiplier)))

(defn prepare-nested-text-props [props]
  (-> props
      (update :style typography/get-nested-style)
      (assoc :parseBasicMarkdown true)
      (assoc :nested? true)))

;; Accessor methods for React Components
(defn text
  "For nested text elements, use nested-text instead"
  ([text-element]
   (text {} text-element))
  ([options text-element]
   [text-class (prepare-text-props options) text-element]))

(defn nested-text
  "Returns nested text elements with proper styling and typography
  Do not use the nested? option, it is for internal usage of the function only"
  [options & nested-text-elements]
  (let [options-with-style (if (:nested? options)
                             (prepare-nested-text-props options)
                             (prepare-text-props options))]
    (reduce (fn [acc text-element]
              (conj acc
                    (if (string? text-element)
                      text-element
                      (let [[options & nested-text-elements] text-element]
                        (apply nested-text (prepare-nested-text-props options)
                               nested-text-elements)))))
            [text-class (dissoc options-with-style :nested?)]
            nested-text-elements)))

;; We track all currently mounted text input refs
;; in a ref-to-defaultValue map
;; so that we can clear them (restore their default values)
;; when global react-navigation's onWillBlur event is invoked
(def text-input-refs (atom {}))

(defn text-input
  [options text]
  (let [render-fn (fn [options text]
                    [text-input-class
                     (merge
                      {:underline-color-android  :transparent
                       :max-font-size-multiplier max-font-size-multiplier
                       :placeholder-text-color   colors/text-gray
                       :placeholder              (i18n/label :t/type-a-message)
                       :value                    text}
                      (-> options
                          (dissoc :preserve-input?)
                          (update :style typography/get-style)
                          (update :style dissoc :line-height)))])]
    (if (:preserve-input? options)
      render-fn
      (let [input-ref (atom nil)]
        (reagent/create-class
         {:component-will-unmount #(when @input-ref
                                     (swap! text-input-refs dissoc @input-ref))
          :reagent-render
          (fn [options text]
            (render-fn (assoc options :ref
                              (fn [r]
                                ;; Store input and its defaultValue
                                ;; one we receive a non-nil ref
                                (when (and r (nil? @input-ref))
                                  (swap! text-input-refs assoc r (:default-value options)))
                                (reset! input-ref r)
                                (when (:ref options)
                                  ((:ref options) r)))) text))})))))

(defn i18n-text
  [{:keys [style key]}]
  [text {:style  style} (i18n/label key)])

(defn icon
  ([n] (icon n {:width  24
                :height 24}))
  ([n style]
   [image {:source     {:uri (keyword (str "icon_" (name n)))}
           :resizeMode "contain"
           :style      style}]))

(defn touchable-opacity [props content]
  [touchable-opacity-class props content])

(defn touchable-highlight [props content]
  [touchable-highlight-class
   (merge {:underlay-color :transparent} props)
   content])

(defn touchable-without-feedback [props content]
  [touchable-without-feedback-class
   props
   content])

(defn get-dimensions [name]
  (js->clj (.get ^js dimensions name) :keywordize-keys true))

(defn list-item [component]
  (reagent/as-element component))

(defn value->picker-item [{:keys [value label]}]
  [picker-item-class {:value (or value "") :label (or label value "")}])

(defn picker [{:keys [style on-change selected enabled data]}]
  (into
   [picker-class (merge (when style {:style style})
                        (when enabled {:enabled enabled})
                        (when on-change {:on-value-change on-change})
                        (when selected {:selected-value selected}))]
   (map value->picker-item data)))

;; Image picker
(defn show-access-error [o]
  (when (= "E_PERMISSION_MISSING" (.-code ^js o))
    (utils/show-popup (i18n/label :t/error)
                      (i18n/label :t/photos-access-error))))

(defn show-image-picker
  ([images-fn]
   (show-image-picker images-fn nil))
  ([images-fn media-type]
   (->  ^js image-picker
        (.openPicker (clj->js {:multiple false :mediaType (or media-type "any")}))
        (.then images-fn)
        (.catch show-access-error))))

;; Clipboard

(def sharing
  (.-Share react-native))

(defn copy-to-clipboard [text]
  (.setString  ^js (.-Clipboard react-native) text))

(defn get-from-clipboard [clbk]
  (let [clipboard-contents (.getString  ^js (.-Clipboard react-native))]
    (.then clipboard-contents #(clbk %))))

;; KeyboardAvoidingView

(defn keyboard-avoiding-view [props & children]
  (into [keyboard-avoiding-view-class
         (merge (when platform/ios? {:behavior :padding}) props)]
        children))

(defn scroll-view [props & children]
  (vec (conj children props scroll-view-class)))

(views/defview with-activity-indicator
  [{:keys [timeout style enabled? preview]} comp]
  (views/letsubs
    [loading (reagent/atom true)]
    {:component-did-mount (fn []
                            (if (or (nil? timeout)
                                    (> 100 timeout))
                              (reset! loading false)
                              (utils/set-timeout #(reset! loading false)
                                                 timeout)))}
    (if (and (not enabled?) @loading)
      (or preview
          [view {:style (or style {:justify-content :center
                                   :align-items     :center})}
           [activity-indicator {:animating true}]])
      comp)))

(def safe-area-provider (reagent/adapt-react-class SafeAreaProvider))
(def safe-area-consumer (reagent/adapt-react-class SafeAreaConsumer))

(def safe-area-view (reagent/adapt-react-class SafeAreaView))

(def touchable-without-feedback-gesture (reagent/adapt-react-class TouchableWithoutFeedback))
