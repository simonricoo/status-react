(ns status-im.ui.components.react
  (:require-macros [status-im.utils.views :as views])
  (:require [goog.object :as object]
            [reagent.core :as reagent]
            [status-im.ui.components.styles :as styles]
            [status-im.utils.utils :as utils]
            [status-im.utils.core :as utils.core]
            [status-im.utils.platform :as platform]
            [status-im.i18n :as i18n]
            [status-im.react-native.js-dependencies :as js-dependencies]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.typography :as typography]))

(defn get-react-property [name]
  (if js-dependencies/react-native
    (or (object/get js-dependencies/react-native name) {})
    #js {}))

(defn adapt-class [class]
  (when class
    (reagent/adapt-react-class class)))

(defn get-class [name]
  (adapt-class (get-react-property name)))

(def native-modules (.-NativeModules js-dependencies/react-native))
(def device-event-emitter (.-DeviceEventEmitter js-dependencies/react-native))

(def splash-screen (.-SplashScreen native-modules))

;; React Components

(def app-registry (get-react-property "AppRegistry"))
(def app-state (get-react-property "AppState"))
(def view (get-class "View"))
(def progress-bar (get-class "ProgressBarAndroid"))

(def status-bar-class (when-not platform/desktop? (get-react-property "StatusBar")))

(def scroll-view-class (get-class "ScrollView"))
(def keyboard-avoiding-view-class (get-class "KeyboardAvoidingView"))


(def text-class (get-class "Text"))
(def text-input-class (get-class "TextInput"))
(def image-class (get-class "Image"))
(def picker-class (get-class "Picker"))
(def picker-item-class (adapt-class (.-Item (get-react-property "Picker"))))

(defn valid-source? [source]
  (or (not (map? source))
      (not (contains? source :uri))
      (and (contains? source :uri)
           (:uri source))))

(defn image [{:keys [source] :as props}]
  (when (valid-source? source)
    [image-class props]))

(def switch-class (get-class "Switch"))

(defn switch [props]
  [switch-class props])

(def touchable-highlight-class (get-class "TouchableHighlight"))
(def touchable-without-feedback-class (get-class "TouchableWithoutFeedback"))
(def touchable-opacity-class (get-class "TouchableOpacity"))
(def activity-indicator-class (get-class "ActivityIndicator"))

(defn activity-indicator [props]
  [activity-indicator-class props])

(defn small-loading-indicator [color]
  [activity-indicator {:color   (if color color :colors/gray)
                       :ios     {:size :small}
                       :android {:size :16}}])

(def modal (get-class "Modal"))

(def pan-responder (.-PanResponder js-dependencies/react-native))
(def animated (.-Animated js-dependencies/react-native))

(def animated-view-class
  (reagent/adapt-react-class (.-View animated)))

(def animated-flat-list-class
  (reagent/adapt-react-class (.-FlatList animated)))

(def animated-scroll-view-class
  (reagent/adapt-react-class (.-ScrollView animated)))

(defn animated-view [props & content]
  (vec (conj content props animated-view-class)))

(defn animated-scroll-view [props & children]
  (vec (conj children props animated-scroll-view-class)))

(def dimensions (.-Dimensions js-dependencies/react-native))
(def keyboard (.-Keyboard js-dependencies/react-native))
(defn dismiss-keyboard! [] (.dismiss keyboard))
(def linking (.-Linking js-dependencies/react-native))
(def desktop-notification (.-DesktopNotification (.-NativeModules js-dependencies/react-native)))

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
  (js->clj (.get dimensions name) :keywordize-keys true))

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

(def image-picker-class js-dependencies/image-crop-picker)

(defn show-access-error [o]
  (when (= "E_PERMISSION_MISSING" (object/get o "code"))
    (utils/show-popup (i18n/label :t/error)
                      (i18n/label :t/photos-access-error))))

(defn show-image-picker
  ([images-fn]
   (show-image-picker images-fn nil))
  ([images-fn media-type]
   (let [image-picker (.-default image-picker-class)]
     (-> image-picker
         (.openPicker (clj->js {:multiple false :mediaType (or media-type "any")}))
         (.then images-fn)
         (.catch show-access-error)))))

;; Net info
(def net-info (if platform/desktop?
                (get-react-property "NetInfo")
                (.-default js-dependencies/net-info)))

;; Clipboard

(def sharing
  (.-Share js-dependencies/react-native))

(defn copy-to-clipboard [text]
  (.setString (.-Clipboard js-dependencies/react-native) text))

(defn get-from-clipboard [clbk]
  (let [clipboard-contents (.getString (.-Clipboard js-dependencies/react-native))]
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

(def safe-area-provider (adapt-class (object/get js-dependencies/safe-area-context "SafeAreaProvider")))
(def safe-area-consumer (adapt-class (object/get js-dependencies/safe-area-context "SafeAreaConsumer")))

(def safe-area-view (adapt-class (object/get js-dependencies/safe-area-context "SafeAreaView")))


(def touchable-without-feedback-gesture (adapt-class (object/get js-dependencies/react-native-gesture-handler "TouchableWithoutFeedback")))
