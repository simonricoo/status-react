(ns status-im.ens.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.multiaccounts.update.core :as multiaccounts.update]
            [status-im.ethereum.abi-spec :as abi-spec]
            [status-im.ethereum.contracts :as contracts]
            [status-im.ethereum.core :as ethereum]
            [status-im.ethereum.ens :as ens]
            [status-im.ethereum.resolver :as resolver]
            [status-im.ethereum.stateofus :as stateofus]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.fx :as fx]
            [status-im.utils.money :as money]
            [status-im.signing.core :as signing]
            [status-im.ethereum.eip55 :as eip55]
            [taoensso.timbre :as log])
  (:refer-clojure :exclude [name]))

(defn fullname [custom-domain? username]
  (if custom-domain?
    username
    (stateofus/subdomain username)))

(re-frame/reg-fx
 :ens/resolve-address
 (fn [[registry name cb]]
   (ens/get-addr registry name cb)))

(re-frame/reg-fx
 :ens/resolve-pubkey
 (fn [[registry name cb]]
   (resolver/pubkey registry name cb)))

(defn assoc-details-for [db username k v]
  (assoc-in db [:ens/names :details username k] v))

(fx/defn set-state
  {:events [::name-resolved]}
  [{:keys [db]} username state]
  (when (= username
           (get-in db [:ens/registration :username]))
    {:db (assoc-in db [:ens/registration :state] state)}))

(fx/defn on-resolver-found
  {:events [::resolver-found]}
  [{:keys [db] :as cofx} resolver-contract]
  (let [{:keys [state username custom-domain?]} (:ens/registration db)
        {:keys [public-key]} (:multiaccount db)
        {:keys [x y]} (ethereum/coordinates public-key)
        namehash (ens/namehash (str username (when-not custom-domain?
                                               ".stateofus.eth")))]
    (signing/eth-transaction-call
     cofx
     {:contract   resolver-contract
      :method     "setPubkey(bytes32,bytes32,bytes32)"
      :params     [namehash x y]
      :on-result  [::save-username custom-domain? username]
      :on-error   [:ens/on-registration-failure]})))

(fx/defn on-input-submitted
  {:events [::input-submitted]}
  [{:keys [db] :as cofx}]
  (let [{:keys [state username custom-domain?]} (:ens/registration db)
        registry-contract (get ens/ens-registries (ethereum/chain-keyword db))
        ens-name (str username (when-not custom-domain?
                                 ".stateofus.eth"))]
    (case state
      ;; :registrable
      :owned
      (ens/resolver registry-contract ens-name
                    #(re-frame/dispatch [::resolver-found %]))
      (log/error ::invalid-username-state))))

(fx/defn username-saved
  {:events [::username-saved]}
  [{:keys [db]}]
  {:db (-> db
           (assoc-in [:ens/registration :state] :saved)
           (update :ens/registration dissoc :registering?))})

(defn- on-resolve [registry custom-domain? username address public-key s]
  (cond
    ;; if we get an address back, we try to get the public key associated
    ;; with the username as well
    (= (eip55/address->checksum address) (eip55/address->checksum s))
    (resolver/pubkey registry (fullname custom-domain? username)
                     #(re-frame/dispatch [::name-resolved username (if (= % public-key) :connected :owned)]))

    ;; No address for a stateofus subdomain: it can be registered
    (and (nil? s) (not custom-domain?))
    (re-frame/dispatch [::name-resolved username :registrable])

    :else
    (re-frame/dispatch [::name-resolved username :unregistrable])))

(fx/defn register-name
  {:events [:ens/register]}
  [{:keys [db] :as cofx} {:keys [amount contract custom-domain? username address public-key]}]
  (let [{:keys [x y]} (ethereum/coordinates public-key)]
    (signing/eth-transaction-call
     cofx
     {:contract   (contracts/get-address db :status/snt)
      :method     "approveAndCall(address,uint256,bytes)"
      :params     [contract
                   (money/unit->token amount 18)
                   (abi-spec/encode "register(bytes32,address,bytes32,bytes32)"
                                    [(ethereum/sha3 username) address x y])]
      :on-result  [::save-username custom-domain? username]
      :on-error   [:ens/on-registration-failure]})))

(defn- valid-custom-domain? [username]
  (and (ens/is-valid-eth-name? username)
       (stateofus/lower-case? username)))

(defn- valid-username? [custom-domain? username]
  (if custom-domain?
    (valid-custom-domain? username)
    (stateofus/valid-username? username)))

(defn- state [custom-domain? username]
  (cond
    (string/blank? username) :initial
    (> 4 (count username)) :too-short
    (valid-username? custom-domain? username) :valid
    :else :invalid))

(fx/defn set-username-candidate
  {:events [:ens/set-username-candidate]}
  [{:keys [db]} custom-domain? username]
  (let [state  (state custom-domain? username)
        valid? (= state :valid)
        name (fullname custom-domain? username)]
    (merge
     {:db (-> db
              (assoc-in [:ens/registration :username] username)
              (assoc-in [:ens/registration :state] state))}
     (when (and name valid?)
       (let [{:keys [multiaccount]} db
             {:keys [public-key]} multiaccount
             address (ethereum/default-address db)
             registry (get ens/ens-registries (ethereum/chain-keyword db))]
         {:ens/resolve-address [registry name #(on-resolve registry custom-domain? username address public-key %)]})))))

(fx/defn clear-cache-and-navigate-back
  {:events [:ens/clear-cache-and-navigate-back]}
  [{:keys [db] :as cofx} _]
  (fx/merge cofx
            {:db (assoc db :ens/registration nil)} ;; Clear cache
            (navigation/navigate-back)))

(fx/defn switch-domain-type
  {:events [::switch-domain-type]}
  [{:keys [db]} _]
  (let [custom-domain? (not (get-in db [:ens/registration :custom-domain?]))]
    {:db (assoc db :ens/registration {:registering? true
                                      :custom-domain? custom-domain?})}))

(fx/defn save-preferred-name
  {:events [::save-preferred-name]}
  [{:keys [db] :as cofx} name]
  (multiaccounts.update/multiaccount-update cofx
                                            {:preferred-name name}
                                            {}))

(fx/defn save-username
  {:events [::save-username]}
  [{:keys [db] :as cofx} custom-domain? username]
  (let [name   (fullname custom-domain? username)
        names  (get-in db [:multiaccount :usernames] [])
        new-names (conj names name)]
    (multiaccounts.update/multiaccount-update cofx
                                              (cond-> {:usernames new-names}
                                                (empty? names) (assoc :preferred-name name))
                                              {:on-success #(re-frame/dispatch [::username-saved])})))

(fx/defn switch-show-username
  {:events [:ens/switch-show-username]}
  [{:keys [db] :as cofx} _]
  (let [show-name? (not (get-in db [:multiaccount :show-name?]))]
    (multiaccounts.update/multiaccount-update cofx
                                              {:show-name? show-name?}
                                              {})))

(fx/defn on-registration-failure
  "TODO not sure there is actually anything to do here
   it should only be called if the user cancels the signing
   Actual registration failure has not been implemented properly"
  {:events [:ens/on-registration-failure]}
  [{:keys [db]} username])

(fx/defn store-name-detail
  {:events [:ens/store-name-detail]}
  [{:keys [db]} name k v]
  {:db (assoc-details-for db name k v)})

(fx/defn navigate-to-name
  {:events [:ens/navigate-to-name]}
  [{:keys [db] :as cofx} name]
  (let [registry (get ens/ens-registries (ethereum/chain-keyword db))]
    (fx/merge cofx
              {:ens/resolve-address [registry name #(re-frame/dispatch [:ens/store-name-detail name :address %])]
               :ens/resolve-pubkey  [registry name #(re-frame/dispatch [:ens/store-name-detail name :public-key %])]}
              (navigation/navigate-to-cofx :ens-name-details name))))

(fx/defn start-registration
  {:events [::add-username-pressed ::get-started-pressed]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (assoc-in db [:ens/registration :registering?] true)}
            (navigation/navigate-to-cofx :ens-register {})))
