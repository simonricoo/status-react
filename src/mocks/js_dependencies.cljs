(ns mocks.js-dependencies)

(def action-button          #js {:default #js {:Item #js {}}})
(def config                 #js {:default #js {}})
(def camera                 #js {:RNCamera #js {:Constants #js {}}})
(def dialogs                #js {})
(def dismiss-keyboard       #js {})
(def emoji-picker           #js {:default #js {}})
(def fs                     #js {})
(def i18n                   #js {:locale "en"})
(def image-crop-picker      #js {})
(def image-resizer          #js {})
(def qr-code                #js {})
(def svg                    #js {})

(def react-native
  (clj->js {:NativeModules {:RNGestureHandlerModule {:Direction (fn [])}
                            :ReanimatedModule {:configureProps (fn [])}}

            :View      {}
            :FlatList  {}
            :Text      {}
            :ProgressBarAndroid {}
            :StatusBar {}
            :ScrollView {}
            :KeyboardAvoidingView {}
            :TextInput {}
            :Image {}
            :Picker {:Item {}}
            :Switch {}
            :Modal {}
            :Keyboard {:dismiss (fn [])}
            :Linking {}
            :TouchableWithoutFeedback  {}
            :TouchableHighlight {}
            :TouchableOpacity {}
            :ActivityIndicator {}
            :StyleSheet {:create (fn [])}
            :Animated            {:createAnimatedComponent identity
                                  :Value (fn [])
                                  :ValueXY (fn [])
                                  :View      {}
                                  :FlatList  {}
                                  :Text      {}}
            :Easing              {:bezier (fn [])
                                  :poly (fn [])
                                  :out (fn [])
                                  :in (fn [])
                                  :inOut (fn [])}
            :DeviceEventEmitter  {:addListener (fn [])}
            :Dimensions          {:get (fn [])}
            :Platform {:select (fn [])}
            :I18nManager {:isRTL ""}
            :NativeEventEmitter (fn [])
            :requireNativeComponent (fn [] {:propTypes ""})}))

(set! js/ReactNative react-native)

(def vector-icons           #js {:default #js {}})
(def webview                #js {:WebView #js {}})
(def status-keycard         #js {:default #js {}})

(def desktop-linking #js {:addEventListener (fn [])})
(def desktop-shortcuts #js {:addEventListener (fn [])})

(def snoopy                 #js {:default #js {}})
(def snoopy-filter          #js {:default #js {}})
(def snoopy-bars            #js {:default #js {}})
(def snoopy-buffer          #js {:default #js {}})
(def fetch                  #js {})

(def background-timer (clj->js {:default {:setTimeout js/setTimeout
                                          :setInterval js/setInterval
                                          :clearTimeout js/clearTimeout
                                          :clearInterval js/clearInterval}}))

(def keychain #js {:setGenericPassword (constantly (.resolve js/Promise true))
                   "ACCESSIBLE" {}
                   "ACCESS_CONTROL" {}})
(def desktop-menu #js {})
(def desktop-config #js {})
(def react-native-mail #js {:mail #js {}})
(def react-native-navigation-twopane  #js {})
(def react-native-screens  #js {})
(def react-native-shake  #js {})
(def net-info  #js {})
(def touchid  #js {})
(def safe-area-context (clj->js {:SafeAreaProvider {:_reactNativeIphoneXHelper {:getStatusBarHeight (fn [])}}
                                 :SafeAreaConsumer {}
                                 :SafeAreaView {}}))
(def react-native-dark-mode #js {"eventEmitter" {} "initialMode" {}})

(def back-handler #js {:addEventListener identity
                       :removeEventListener identity})
(def react #js {:useCallback nil
                :useEffect nil
                :useRef nil
                :createRef nil
                :Fragment identity})
(def react-navigation-native #js {:NavigationContainer #js {}
                                  :useFocusEffect      identity
                                  :CommonActions       #js {}
                                  :StackActions        #js {}})
(def react-navigation-stack #js {:createStackNavigator identity
                                 :TransitionPresets    #js {:ModalPresentationIOS #js {}}})
(def react-navigation-bottom-tabs #js {:createBottomTabNavigator identity})

(def react-native-haptic-feedback #js {:default #js {:trigger nil}})

(def react-native-reanimated #js {:default      #js {:createAnimatedComponent identity
                                                     :eq                      nil
                                                     :greaterOrEq             nil
                                                     :add                     nil
                                                     :sub                     nil
                                                     :multiply                nil
                                                     :abs                     nil
                                                     :min                     nil
                                                     :max                     nil
                                                     :neq                     nil
                                                     :and                     nil
                                                     :or                      nil
                                                     :not                     nil
                                                     :set                     nil
                                                     :startClock              nil
                                                     :stopClock               nil
                                                     :Value                   nil
                                                     :Clock                   nil
                                                     :debug                   nil
                                                     :log                     nil
                                                     :event                   nil
                                                     :cond                    nil
                                                     :block                   nil
                                                     :interpolate             nil
                                                     :call                    nil
                                                     :timing                  nil
                                                     :onChange                nil
                                                     :View                    #js {}
                                                     :ScrollView              #js {}
                                                     :Text                    #js {}
                                                     :Extrapolate             #js {:CLAMP nil}
                                                     :Code                    #js {}}
                                  :Easing       #js {:bezier nil
                                                     :linear nil}
                                  :clockRunning nil})
(def react-native-gesture-handler #js {:default                  #js {}
                                       :State                    #js {:BEGAN        nil
                                                                      :ACTIVE       nil
                                                                      :CANCELLED    nil
                                                                      :END          nil
                                                                      :FAILED       nil
                                                                      :UNDETERMINED nil}
                                       :PureNativeButton         #js {}
                                       :TapGestureHandler        #js {}
                                       :PanGestureHandler        #js {}
                                       :LongPressGestureHandler  #js {}
                                       :TouchableWithoutFeedback #js {}
                                       :createNativeWrapper      identity})

(def react-native-redash #js {:clamp nil})

(def react-native-languages
  (clj->js {:default {:language "en",
                      :addEventListener (fn []),
                      :removeEventListener (fn [])}}))

(defn mock [module]
  (case module
    "react-native-languages" react-native-languages
    "react-native-background-timer" background-timer
    "react-native-image-crop-picker" image-crop-picker
    "react-native-gesture-handler" react-native-gesture-handler
    "react-native-safe-area-context" safe-area-context
    "react-native-config" config
    "react-native-dark-mode" react-native-dark-mode
    "react-native-iphone-x-helper" (clj->js {:getStatusBarHeight (fn [])
                                             :getBottomSpace (fn [])})
    "react-native-screens" (clj->js {})
    "react-native-reanimated" react-native-reanimated
    "react-native-redash" react-native-redash
    "react-native-fetch-polyfill" fetch
    "react-native-status-keycard" status-keycard
    "react-native-keychain" keychain
    "react-native-touch-id" touchid
    "@react-native-community/netinfo" net-info
    "react-native-dialogs" dialogs
    "react-native" react-native
    "react-native-fs" fs
    "react-native-mail" react-native-mail
    "react-native-image-resizer" image-resizer
    nil))
