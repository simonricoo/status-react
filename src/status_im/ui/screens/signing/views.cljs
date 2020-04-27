(ns status-im.ui.screens.signing.views
  (:require-macros [status-im.utils.views :as views])
  (:require [status-im.ui.components.react :as react]
            [re-frame.core :as re-frame]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.list-item.views :as list-item]
            [status-im.ui.components.button :as button]
            [status-im.ui.components.copyable-text :as copyable-text]
            [status-im.wallet.utils :as wallet.utils]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.chat-icon.screen :as chat-icon]
            [status-im.ui.components.icons.vector-icons :as icons]
            [status-im.ui.components.text-input.view :as text-input]
            [status-im.i18n :as i18n]
            [status-im.utils.security :as security]
            [status-im.ui.screens.signing.sheets :as sheets]
            [status-im.ethereum.tokens :as tokens]
            [clojure.string :as string]
            [status-im.ui.screens.signing.styles :as styles]
            [status-im.react-native.resources :as resources]
            [status-im.ui.screens.hardwallet.pin.views :as pin.views]
            [status-im.ui.components.bottom-panel.views :as bottom-panel]
            [status-im.utils.utils :as utils]))

(defn separator []
  [react/view {:height 1 :background-color colors/gray-lighter}])

(defn displayed-name [contact]
  (if (or (:preferred-name contact) (:name contact))
    (multiaccounts/displayed-name contact)
    (utils/get-shortened-checksum-address (:address contact))))

(defn contact-item [title contact]
  [list-item/list-item
   {:title       title
    :title-prefix-width 45
    :type               :small
    :accessories
    [[copyable-text/copyable-text-view
      {:copied-text (displayed-name contact)}
      [react/text
       {:ellipsize-mode  :middle
        :number-of-lines 1
        :style           {:font-family "monospace"
                          :line-height 22}}
       (displayed-name contact)]]]}])

(defn token-item [{:keys [icon color] :as token} display-symbol]
  (when token
    [react/view
     [list-item/list-item
      {:type        :small
       :title       :t/wallet-asset
       :accessories
       [display-symbol
        (if icon
          [list/item-image
           (assoc icon
                  :style {:background-color colors/gray-lighter
                          :border-radius    16}
                  :image-style {:width 24 :height 24})]
          [chat-icon/custom-icon-view-list (:name token) color 32])]}]
     [separator]]))

(defn header
  [{:keys [in-progress?] :as sign}
   {:keys [contact amount approve?]}
   display-symbol fee fee-display-symbol]
  [react/view styles/header
   (when sign
     [react/touchable-highlight (when-not in-progress? {:on-press #(re-frame/dispatch [:set :signing/sign nil])})
      [react/view {:padding-right 16}
       [icons/icon :main-icons/back]]])
   [react/view {:flex 1}
    (if amount
      [react/text {:style {:typography :title-bold}} (str (if approve? (i18n/label :t/authorize) (i18n/label :t/sending))
                                                          " " amount " " display-symbol)]
      [react/text {:style {:typography :title-bold}} (i18n/label :t/contract-interaction)])
    (if sign
      [react/nested-text {:style           {:color colors/gray}
                          :ellipsize-mode  :middle
                          :number-of-lines 1} (i18n/label :t/to) " "
       [{:style {:color colors/black}} (displayed-name contact)]]
      [react/text {:style {:margin-top 6 :color colors/gray}}
       (str fee " " fee-display-symbol " " (string/lower-case (i18n/label :t/network-fee)))])]
   [button/button (merge {:type :secondary
                          :container-style {:padding-horizontal 24}
                          :label (i18n/label :t/cancel)}
                         (when-not in-progress? {:on-press #(re-frame/dispatch [:signing.ui/cancel-is-pressed])}))]])

(views/defview keycard-pin-view []
  (views/letsubs [pin [:hardwallet/pin]
                  small-screen? [:dimensions/small-screen?]
                  error-label [:hardwallet/pin-error-label]
                  enter-step [:hardwallet/pin-enter-step]
                  status [:hardwallet/pin-status]
                  retry-counter [:hardwallet/retry-counter]]
    [react/view
     [pin.views/pin-view
      {:pin           pin
       :retry-counter retry-counter
       :step          enter-step
       :small-screen? small-screen?
       :status        status
       :error-label   error-label}]]))

(defn sign-with-keycard-button
  [amount-error gas-error]
  (let [disabled? (or amount-error gas-error)]
    [react/touchable-highlight {:on-press #(when-not disabled?
                                             (re-frame/dispatch [:signing.ui/sign-with-keycard-pressed]))}
     [react/view (styles/sign-with-keycard-button disabled?)
      [react/text {:style (styles/sign-with-keycard-button-text disabled?)}
       (i18n/label :t/sign-with)]
      [react/view {:padding-right 16}
       [react/image {:source (resources/get-image :keycard-logo)
                     :style  {:width         64
                              :margin-bottom 7
                              :height        26}}]]]]))

(defn- signing-phrase-view [phrase]
  [react/view {:align-items :center}
   [react/text {:style {:color colors/gray :padding-bottom 8}} (i18n/label :t/signing-phrase)]
   [react/text phrase]])

(defn- keycard-view
  [{:keys [keycard-step]} phrase]
  [react/view {:height 520}
   [signing-phrase-view phrase]
   (case keycard-step
     :pin     [keycard-pin-view]
     [react/view {:align-items :center :margin-top 16 :margin-bottom 40}
      [sign-with-keycard-button nil nil]])])

(defn signature-request
  [{:keys [formatted-data fiat-amount fiat-currency keycard-step]} small-screen?]
  (let [message (:message formatted-data)
        title (case keycard-step
                :connect :t/looking-for-cards
                :signing :t/processing
                :error      :t/lost-connection
                :success :t/success)
        subtitle (case keycard-step
                   :connect :t/hold-card
                   :signing :t/try-keeping-the-card-still
                   :error :t/tap-card-again
                   :success :t/transaction-signed)]
    [react/view (assoc styles/message :padding-vertical 16 :align-items :center)
     [react/view {:style {:align-self :flex-start :padding-left 16 :margin-bottom 24}}
      [react/text {:style {:font-size (if small-screen? 15 17) :font-weight "700"}}
       (i18n/label :t/confirmation-request)]]
     (when (and (:formatted-amount message) (:formatted-currency message))
       [react/view {:style {:margin-bottom 24 :align-self :stretch}}
        [react/nested-text {:style {:font-weight "500" :font-size (if small-screen? 34 44)
                                    :text-align :center}}
         (str (:formatted-amount message) " ")
         [{:style {:color colors/gray}} (:formatted-currency message)]]
        [react/text {:style {:font-size 19 :text-align :center
                             :margin-bottom 16}}
         (str fiat-amount " " fiat-currency)]
        [separator]])
     [react/view {:style (styles/sheet-icon (case keycard-step
                                              (:connect :signing) colors/blue-transparent-10
                                              :error              colors/red-transparent-10
                                              :success            colors/green-transparent-10))}
      (case keycard-step
        :connect
        [icons/icon :main-icons/nfc {:color colors/blue :width 27 :height 21}]
        :signing
        [react/activity-indicator {:animating true :color colors/blue}]
        :error
        [icons/icon :main-icons/close {:color colors/red}]
        :success
        [icons/icon :main-icons/check {:color colors/green}])]
     [react/text {:style (styles/sheet-title small-screen?)} (i18n/label title)]
     [react/text {:style (styles/sheet-subtitle small-screen?)} (i18n/label subtitle)]
     [button/button {:type            :main
                     :disabled?       (= keycard-step :success)
                     :text-style      {:font-size (if small-screen? 18 20)}
                     :style           {:align-self :stretch}
                     :container-style {:height (if small-screen? 52 64)}
                     :label           (i18n/label :t/show-transaction-data)
                     :on-press        #(re-frame/dispatch [:show-popover {:view :transaction-data}])}]
     [button/button {:type            :main
                     :theme           :red
                     :disabled?       (= keycard-step :success)
                     :container-style {:margin-top 8
                                       :height     64 :margin-bottom 16}
                     :style           {:align-self :stretch}
                     :text-style     {:font-size 20}
                     :label           (i18n/label :t/decline)
                     :on-press        #(re-frame/dispatch [:signing.ui/cancel-is-pressed])}]]))

(defn- transaction-data-item [{:keys [label data]}]
  [react/view
   [react/text {:style {:font-size     17
                        :line-height   20
                        :margin-bottom 8
                        :color         colors/gray}}
    label]
   [react/text {:style {:font-size     17
                        :line-height   20
                        :margin-bottom 24}}
    data]])

(views/defview transaction-data []
  (views/letsubs
    [{:keys [formatted-data]} [:signing/sign]]
    [react/view {:style {:flex 1}}
     [react/view {:style {:margin-horizontal 24
                          :margin-top        24}}
      [react/text {:style {:font-size   17
                           :font-weight "700"}}
       (i18n/label :t/transaction-data)]]
     [react/scroll-view {:style {:flex               1
                                 :margin-horizontal  8
                                 :padding-horizontal 16
                                 :padding-vertical   10
                                 :margin-vertical    14}}
      [transaction-data-item {:label "Label"
                              :data  formatted-data}]]
     [separator]
     [react/view {:style {:margin-horizontal 8
                          :margin-vertical   16}}
      [button/button  {:type            :main
                       :text-style      {:font-size 20}
                       :style           {:margin-horizontal 0}
                       :container-style {:height 64}
                       :label           (i18n/label :t/close)
                       :on-press        #(re-frame/dispatch [:hide-popover])}]]]))

(views/defview password-view [{:keys [type error in-progress? enabled?] :as sign}]
  (views/letsubs [phrase [:signing/phrase]]
    (case type
      :password
      [react/view {:padding-top 8 :padding-bottom 8}
       [signing-phrase-view phrase]
       [text-input/text-input-with-label
        {:secure-text-entry   true
         :placeholder         (i18n/label :t/enter-password)
         :on-change-text      #(re-frame/dispatch [:signing.ui/password-is-changed (security/mask-data %)])
         :accessibility-label :enter-password-input
         :auto-capitalize     :none
         :editable            (not in-progress?)
         :error               error
         :container           {:margin-top 12 :margin-bottom 12 :margin-horizontal 16}}]
       [react/view {:align-items :center :height 60}
        (if in-progress?
          [react/activity-indicator {:animating true
                                     :size      :large}]
          [button/button {:on-press  #(re-frame/dispatch [:signing.ui/sign-is-pressed])
                          :disabled? (not enabled?)
                          :label     :t/transactions-sign}])]]
      :keycard
      [keycard-view sign phrase]
      [react/view])))

(views/defview message-sheet []
  (views/letsubs [{:keys [formatted-data type] :as sign} [:signing/sign]
                  small-screen? [:dimensions/small-screen?]]
    (if (= type :pinless)
      [signature-request sign small-screen?]
      [react/view (styles/message)
       [react/view styles/message-header
        [react/text {:style {:typography :title-bold}} (i18n/label :t/signing-a-message)]
        [react/touchable-highlight {:on-press #(re-frame/dispatch [:signing.ui/cancel-is-pressed])}
         [react/view {:padding 6}
          [react/text {:style {:color colors/blue}} (i18n/label :t/cancel)]]]]
       [separator]
       [react/view {:padding-top 16 :flex 1}
        [react/view styles/message-border
         [react/scroll-view
          [react/text (or formatted-data "")]]]
        [password-view sign]]])))

(defn amount-item [prices wallet-currency amount amount-error display-symbol fee-display-symbol prices-loading?]
  (let [converted-value (* amount (get-in prices [(keyword display-symbol) (keyword (:code wallet-currency)) :price]))]
    [list-item/list-item
     {:type  :small
      :title :t/send-request-amount
      :error amount-error
      :accessories [[react/view {:style {:flex-direction :row}}
                     [react/nested-text {:style {:color colors/gray}}
                      [{:style {:color colors/black}} (utils/format-decimals amount 6)]
                      " "
                      (or display-symbol fee-display-symbol)
                      " • "]
                     (if prices-loading?
                       [react/small-loading-indicator]
                       [react/text {:style {:color colors/black}} (i18n/format-currency converted-value (:code wallet-currency))])
                     [react/text {:style {:color colors/gray}} (str " " (:code wallet-currency))]]]}]))

(views/defview fee-item [prices wallet-currency fee-display-symbol fee gas-error prices-loading?]
  (views/letsubs [{:keys [gas-price-loading? gas-loading?]} [:signing/edit-fee]]
    (let [converted-fee-value (* fee (get-in prices [(keyword fee-display-symbol) (keyword (:code wallet-currency)) :price]))]
      [list-item/list-item
       {:type        :small
        :title       :t/network-fee
        :error       (when-not (or gas-price-loading? gas-loading?) gas-error)
        :disabled?   (or gas-price-loading? gas-loading?)
        :accessories (if (or gas-price-loading? gas-loading?)
                       [[react/small-loading-indicator]]
                       [[react/view {:style {:flex-direction :row}}
                         [react/nested-text {:style {:color colors/gray}}
                          [{:style {:color colors/black}} (utils/format-decimals fee 6)]
                          " "
                          fee-display-symbol
                          " • "]
                         (if prices-loading?
                           [react/small-loading-indicator]
                           [react/text {:style {:color colors/black}} (i18n/format-currency converted-fee-value (:code wallet-currency))])
                         [react/text {:style {:color colors/gray}} (str " " (:code wallet-currency))]]
                        :chevron])
        :on-press    #(re-frame/dispatch
                       [:signing.ui/open-fee-sheet
                        {:content        (fn [] [sheets/fee-bottom-sheet fee-display-symbol])
                         :content-height 270}])}])))

(views/defview network-item []
  (views/letsubs [network-name [:network-name]]
    [list-item/list-item
     {:title       :t/network
      :type        :small
      :accessories [[react/text network-name]]}]))

(views/defview sheet
  [{:keys [from contact amount token] :as tx}]
  (views/letsubs [fee                   [:signing/fee]
                  sign                  [:signing/sign]
                  chain                 [:ethereum/chain-keyword]
                  {:keys [amount-error gas-error]} [:signing/amount-errors (:address from)]
                  keycard-multiaccount? [:keycard-multiaccount?]
                  prices                [:prices]
                  wallet-currency       [:wallet/currency]
                  mainnet?              [:mainnet?]
                  prices-loading?       [:prices-loading?]]
    (let [display-symbol     (wallet.utils/display-symbol token)
          fee-display-symbol (wallet.utils/display-symbol (tokens/native-currency chain))]
      [react/view (styles/sheet)
       [header sign tx display-symbol fee fee-display-symbol]
       [separator]
       (if sign
         [react/view {:padding-top 20}
          [password-view sign]]
         [react/view
          (when-not mainnet?
            [react/view
             [network-item]
             [separator]])
          [contact-item (i18n/label :t/from) from]
          [separator]
          [contact-item (i18n/label :t/to) contact]
          [separator]
          [token-item token display-symbol]
          [amount-item prices wallet-currency amount amount-error display-symbol fee-display-symbol prices-loading?]
          [separator]
          [fee-item prices wallet-currency fee-display-symbol fee gas-error prices-loading?]
          [react/view {:align-items :center :margin-top 16 :margin-bottom 40}
           (if keycard-multiaccount?
             [sign-with-keycard-button amount-error gas-error]
             [button/button {:on-press  #(re-frame/dispatch [:set :signing/sign {:type :password}])
                             :disabled? (or amount-error gas-error)
                             :label     :t/sign-with-password}])]])])))

(views/defview signing []
  (views/letsubs [tx [:signing/tx]]
    [bottom-panel/animated-bottom-panel
     ;;we use select-keys here because we don't want to update view if other keys in map are changed
     (when tx (select-keys tx [:from :contact :amount :token :approve? :message]))
     #(if (:message %)
        [message-sheet]
        [sheet %])]))
