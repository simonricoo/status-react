(ns status-im.multiaccounts.create.core
  (:require [clojure.set :refer [map-invert]]
            [re-frame.core :as re-frame]
            [status-im.constants :as constants]
            [status-im.data-store.settings :as data-store.settings]
            [status-im.ethereum.core :as ethereum]
            [status-im.ethereum.eip55 :as eip55]
            [taoensso.timbre :as log]
            [status-im.i18n :as i18n]
            [status-im.hardwallet.nfc :as nfc]
            [status-im.multiaccounts.db :as db]
            [status-im.native-module.core :as status]
            [status-im.node.core :as node]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.fx :as fx]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.security :as security]
            [status-im.utils.signing-phrase.core :as signing-phrase]
            [status-im.utils.types :as types]
            [status-im.utils.utils :as utils]
            [status-im.utils.platform :as platform]
            [status-im.ui.components.bottom-sheet.core :as bottom-sheet]))

(def step-kw-to-num
  {:generate-key         1
   :choose-key           2
   :select-key-storage   3
   :create-code          4
   :confirm-code         5})

(defn decrement-step [step]
  (let [inverted  (map-invert step-kw-to-num)]
    (if (and (= step :create-code)
             (or (not platform/android?)
                 (not (nfc/nfc-supported?))))
      :choose-key
      (inverted (dec (step-kw-to-num step))))))

(defn inc-step [step]
  (let [inverted  (map-invert step-kw-to-num)]
    (if (and (= step :choose-key)
             (or (not platform/android?)
                 (not (nfc/nfc-supported?))))
      :create-code
      (inverted (inc (step-kw-to-num step))))))

;; multiaccounts create module
(defn get-selected-multiaccount [{:keys [db]}]
  (let [{:keys [selected-id multiaccounts]} (:intro-wizard db)]
    (some #(when (= selected-id (:id %)) %) multiaccounts)))

(defn normalize-derived-data-keys [derived-data]
  (->> derived-data
       (map (fn [[path {:keys [publicKey] :as data}]]
              [path (cond-> (-> data
                                (dissoc :publicKey)
                                (assoc :public-key publicKey)))]))
       (into {})))

(defn normalize-multiaccount-data-keys
  [{:keys [publicKey keyUid derived] :as data}]
  (cond-> (-> data
              (dissoc :keyUid :publicKey)
              (assoc :key-uid keyUid
                     :public-key publicKey))
    derived
    (update :derived normalize-derived-data-keys)))

(fx/defn create-multiaccount
  [{:keys [db]}]
  (let [{:keys [selected-id key-code]} (:intro-wizard db)
        hashed-password (ethereum/sha3 (security/safe-unmask-data key-code))
        callback (fn [result]
                   (let [derived-data (normalize-derived-data-keys (types/json->clj result))
                         public-key (get-in derived-data [constants/path-whisper-keyword :public-key])]
                     (status/gfycat-identicon-async
                      public-key
                      (fn [name photo-path]
                        (let [derived-whisper (derived-data constants/path-whisper-keyword)
                              derived-data-extended (assoc-in derived-data
                                                              [constants/path-whisper-keyword]
                                                              (merge derived-whisper {:name name :photo-path photo-path}))]
                          (re-frame/dispatch [::store-multiaccount-success
                                              key-code derived-data-extended]))))))]
    {::store-multiaccount [selected-id hashed-password callback]}))

(fx/defn prepare-intro-wizard
  [{:keys [db] :as cofx}]
  {:db (assoc db :intro-wizard {:step :generate-key
                                :weak-password? true
                                :back-action :intro-wizard/navigate-back
                                :forward-action :intro-wizard/step-forward-pressed
                                :encrypt-with-password? true})})

(fx/defn intro-wizard
  {:events [:multiaccounts.create.ui/intro-wizard]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (update db :hardwallet dissoc :flow)}
            (prepare-intro-wizard)
            (navigation/navigate-to-cofx :create-multiaccount-generate-key nil)))

(fx/defn get-new-key
  {:events [:multiaccounts.create.ui/get-new-key]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            (prepare-intro-wizard)
            (bottom-sheet/hide-bottom-sheet)
            (navigation/navigate-to-cofx :create-multiaccount-generate-key nil)))

(fx/defn dec-step
  {:events [:intro-wizard/dec-step]}
  [{:keys [db] :as cofx}]
  (let  [step (get-in db [:intro-wizard :step])]
    (fx/merge cofx
              (when-not (= :generate-key step)
                {:db (cond-> (assoc-in db [:intro-wizard :step] (decrement-step step))
                       (#{:create-code :confirm-code} step)
                       (update :intro-wizard assoc :weak-password? true :key-code nil)
                       (= step :confirm-code)
                       (assoc-in [:intro-wizard :confirm-failure?] false))})
              (when (= :generate-key-step)
                {:db (dissoc db :intro-wizard)}))))

(fx/defn intro-step-back
  {:events [:intro-wizard/navigate-back]}
  [{:keys [db] :as cofx} skip-alert?]
  (let  [step (get-in db [:intro-wizard :step])]
    (if (and (= step :choose-key) (not skip-alert?))
      (utils/show-question
       (i18n/label :t/are-you-sure-to-cancel)
       (i18n/label :t/you-will-start-from-scratch)
       #(re-frame/dispatch [:intro-wizard/navigate-back true]))
      (fx/merge cofx
                dec-step
                navigation/navigate-back))))

(fx/defn exit-wizard
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (dissoc db :intro-wizard)}
            (navigation/navigate-to-cofx :welcome nil)))

(fx/defn init-key-generation
  [{:keys [db] :as cofx}]
  {:db (assoc-in db [:intro-wizard :processing?] true)
   :intro-wizard/start-onboarding nil})

(fx/defn on-confirm-failure [{:keys [db] :as cofx}]
  (do
    (utils/vibrate)
    {:db (assoc-in db [:intro-wizard :confirm-failure?] true)}))

(defn confirm-failure? [db]
  (let [step (get-in db [:intro-wizard :step])]
    (and (= step :confirm-code)
         (not (:multiaccounts/login db))
         (get-in db [:intro-wizard :encrypt-with-password?])
         (not= (get-in db [:intro-wizard :stored-key-code]) (get-in db [:intro-wizard :key-code])))))

(fx/defn store-key-code [{:keys [db] :as cofx}]
  (let [key-code  (get-in db [:intro-wizard :key-code])]
    {:db (update db :intro-wizard
                 assoc :stored-key-code key-code
                 :key-code nil)}))

(fx/defn intro-step-forward
  {:events [:intro-wizard/step-forward-pressed]}
  [{:keys [db] :as cofx} {:keys [skip?] :as opts}]
  (let [{:keys [step selected-storage-type processing? weak-password?]} (:intro-wizard db)]
    (log/debug "[multiaccount.create] intro-step-forward"
               "step" step)
    (cond (confirm-failure? db)
          (on-confirm-failure cofx)

          (= step :generate-key)
          (init-key-generation cofx)

          (and  (= step :create-code)
                (= weak-password? true))
          nil

          (and (= step :confirm-code)
               (not processing?))
          (fx/merge cofx
                    {:db (assoc-in db [:intro-wizard :processing?] true)}
                    create-multiaccount)

          (and  (= step :confirm-code)
                (:multiaccounts/login db))
          (exit-wizard cofx)

          (and (= step :select-key-storage)
               (= :advanced selected-storage-type))
          {:dispatch [:keycard/start-onboarding-flow]}

          :else (let [next-step (inc-step step)]
                  (fx/merge cofx
                            {:db (update db :intro-wizard
                                         assoc :processing? false
                                         :step next-step)}
                            (when (= step :create-code)
                              store-key-code)
                            (navigation/navigate-to-cofx (->> next-step name (str "create-multiaccount-") keyword) nil))))))

(defn prepare-accounts-data
  [multiaccount]
  [(let [{:keys [public-key address]}
         (get-in multiaccount [:derived constants/path-default-wallet-keyword])]
     {:public-key public-key
      :address    (eip55/address->checksum address)
      :color      colors/blue-persist
      :wallet     true
      :path       constants/path-default-wallet
      :name       "Status account"})
   (let [{:keys [public-key address name photo-path]}
         (get-in multiaccount [:derived constants/path-whisper-keyword])]
     {:public-key public-key
      :address    (eip55/address->checksum address)
      :name       name
      :photo-path photo-path
      :path       constants/path-whisper
      :chat       true})])

(fx/defn save-multiaccount-and-login-with-keycard
  [_ args]
  {:hardwallet/save-multiaccount-and-login args})

(fx/defn save-account-and-login
  [_ multiaccount-data password settings node-config accounts-data]
  {::save-account-and-login [(types/clj->json multiaccount-data)
                             password
                             (types/clj->json settings)
                             node-config
                             (types/clj->json accounts-data)]})

(fx/defn on-multiaccount-created
  [{:keys [signing-phrase random-guid-generator db] :as cofx}
   {:keys [address chat-key keycard-instance-uid key-uid
           keycard-pairing keycard-paired-on mnemonic public-key]
    :as multiaccount}
   password
   {:keys [save-mnemonic? login?] :or {login? true save-mnemonic? false}}]
  (let [[wallet-account {:keys [public-key photo-path name]} :as accounts-data] (prepare-accounts-data multiaccount)
        multiaccount-data {:name       name
                           :address    address
                           :photo-path photo-path
                           :key-uid    key-uid
                           :keycard-pairing keycard-pairing}
        keycard-multiaccount? (boolean keycard-pairing)
        eip1581-address (get-in multiaccount [:derived
                                              constants/path-eip1581-keyword
                                              :address])
        new-multiaccount
        (cond-> (merge
                 {;; address of the master key
                  :address               address
                  ;; sha256 of master public key
                  :key-uid               key-uid
                  ;; The address from which we derive any wallet
                  :wallet-root-address
                  (get-in multiaccount [:derived
                                        constants/path-wallet-root-keyword
                                        :address])
                  :name                  name
                  :photo-path            photo-path
                  ;; public key of the chat account
                  :public-key            public-key
                  ;; default address for Dapps
                  :dapps-address         (:address wallet-account)
                  :latest-derived-path   0
                  :signing-phrase        signing-phrase
                  :installation-id       (random-guid-generator)}
                 constants/default-multiaccount)
          ;; The address from which we derive any chat
          ;; account/encryption keys
          eip1581-address
          (assoc :eip1581-address eip1581-address)
          save-mnemonic?
          (assoc :mnemonic mnemonic)
          keycard-multiaccount?
          (assoc :keycard-instance-uid keycard-instance-uid
                 :keycard-pairing keycard-pairing
                 :keycard-paired-on keycard-paired-on))
        db (assoc db
                  :multiaccounts/login {:key-uid    key-uid
                                        :name       name
                                        :photo-path photo-path
                                        :password   password
                                        :creating?  true
                                        :processing true}
                  :multiaccount new-multiaccount
                  :multiaccount/accounts [wallet-account]
                  :networks/current-network constants/default-network
                  :networks/networks (data-store.settings/rpc->networks constants/default-networks))
        settings (assoc new-multiaccount
                        :networks/current-network constants/default-network
                        :networks/networks constants/default-networks)]
    (fx/merge cofx
              {:db db}
              (if keycard-multiaccount?
                (save-multiaccount-and-login-with-keycard
                 {:multiaccount-data multiaccount-data
                  :password          password
                  :settings          settings
                  :node-config       (node/get-new-config db)
                  :accounts-data     accounts-data
                  :chat-key          chat-key})
                (save-account-and-login multiaccount-data
                                        (ethereum/sha3 (security/safe-unmask-data password))
                                        settings
                                        (node/get-new-config db)
                                        accounts-data))
              (when (:intro-wizard db)
                (intro-step-forward {})))))

(re-frame/reg-fx
 :intro-wizard/start-onboarding
 (fn []
   (status/multiaccount-generate-and-derive-addresses
    5
    12
    [constants/path-whisper constants/path-default-wallet]
    #(re-frame/dispatch [:intro-wizard/on-keys-generated
                         (mapv normalize-multiaccount-data-keys
                               (types/json->clj %))]))))

(fx/defn on-keys-generated
  {:events [:intro-wizard/on-keys-generated]}
  [{:keys [db] :as cofx} result]
  (fx/merge
   {:db (update db :intro-wizard
                (fn [data]
                  (-> data
                      (dissoc :processing?)
                      (assoc :multiaccounts result
                             :selected-storage-type :default
                             :selected-id (-> result first :id)
                             :step :choose-key))))}
   (navigation/navigate-to-cofx :create-multiaccount-choose-key nil)))

(fx/defn on-key-selected
  {:events [:intro-wizard/on-key-selected]}
  [{:keys [db] :as cofx} id]
  {:db (assoc-in db [:intro-wizard :selected-id] id)})

(fx/defn on-key-storage-selected
  {:events [:intro-wizard/on-key-storage-selected]}
  [{:keys [db] :as cofx} storage-type]
  {:db (assoc-in db [:intro-wizard :selected-storage-type] storage-type)})

(fx/defn on-encrypt-with-password-pressed
  {:events [:intro-wizard/on-encrypt-with-password-pressed]}
  [{:keys [db] :as cofx}]
  {:db (assoc-in db [:intro-wizard :encrypt-with-password?] true)})

(fx/defn on-learn-more-pressed
  {:events [:intro-wizard/on-learn-more-pressed]}
  [{:keys [db] :as cofx}]
  {:db (assoc-in db [:intro-wizard :show-learn-more?] true)})

(defn get-new-key-code [current-code sym encrypt-with-password?]
  (cond (or (= sym :remove) (= sym "Backspace"))
        (subs current-code 0 (dec (count current-code)))
        (and (not encrypt-with-password?) (= (count current-code) 6))
        current-code
        (= (count sym) 1)
        (str current-code sym)
        :else current-code))

(fx/defn code-symbol-pressed
  {:events [:intro-wizard/code-symbol-pressed]}
  [{:keys [db] :as cofx} new-key-code]
  (let [encrypt-with-password? (get-in db [:intro-wizard :encrypt-with-password?])]
    {:db (update db :intro-wizard assoc :key-code new-key-code
                 :confirm-failure? false
                 :weak-password? (not (db/valid-length? new-key-code)))}))

(re-frame/reg-cofx
 ::get-signing-phrase
 (fn [cofx _]
   (assoc cofx :signing-phrase (signing-phrase/generate))))

(fx/defn create-multiaccount-success
  {:events [::store-multiaccount-success]
   :interceptors [(re-frame/inject-cofx :random-guid-generator)
                  (re-frame/inject-cofx ::get-signing-phrase)]}
  [cofx password derived]
  (on-multiaccount-created cofx
                           (assoc
                            (get-selected-multiaccount cofx)
                            :derived
                            derived)
                           password
                           {:save-mnemonic? true}))

(re-frame/reg-fx
 ::store-multiaccount
 (fn [[id hashed-password callback]]
   (status/multiaccount-store-derived
    id
    [constants/path-wallet-root
     constants/path-eip1581
     constants/path-whisper
     constants/path-default-wallet]
    hashed-password
    callback)))

(re-frame/reg-fx
 ::save-account-and-login
 (fn [[multiaccount-data hashed-password settings config accounts-data]]
   (status/save-account-and-login multiaccount-data
                                  hashed-password
                                  settings
                                  config
                                  accounts-data)))
