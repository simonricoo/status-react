(ns status-im.network.net-info
  (:require [taoensso.timbre :as log]
            [status-im.utils.platform :as platform]
            [re-frame.core :as re-frame]
            [status-im.utils.fx :as fx]
            [status-im.mailserver.core :as mailserver]
            [status-im.chaos-mode.core :as chaos-mode]
            [status-im.native-module.core :as status]
            [status-im.ui.screens.mobile-network-settings.events :as mobile-network]
            [status-im.wallet.core :as wallet]
            ["@react-native-community/netinfo" :default net-info]))

(fx/defn change-network-status
  [{:keys [db] :as cofx} is-connected?]
  (fx/merge cofx
            {:db (assoc db :network-status (if is-connected? :online :offline))}
            (when is-connected?
              (if-not (= (count (get-in db [:wallet :accounts])) (count (get db :multiaccount/accounts)))
                (wallet/update-balances nil)))))

(fx/defn change-network-type
  [{:keys [db] :as cofx} old-network-type network-type expensive?]
  (fx/merge cofx
            {:db (assoc db :network/type network-type)
             :network/notify-status-go [network-type expensive?]}
            (mobile-network/on-network-status-change)))

(fx/defn handle-network-info-change
  {:events [::network-info-changed]}
  [{:keys [db] :as cofx} {:keys [isConnected type details] :as state}]
  (let [old-network-status (:network-status db)
        old-network-type (:network/type db)
        connectivity-status (if isConnected :online :offline)]
    (fx/merge cofx
              (when-not (= connectivity-status old-network-status)
                (change-network-status isConnected))
              (when-not (= type old-network-type)
                (change-network-type old-network-type type (:is-connection-expensive details))))))

(defn add-net-info-listener []
  (when net-info
    (.addEventListener ^js net-info
                       #(re-frame/dispatch [::network-info-changed
                                            (js->clj % :keywordize-keys true)]))))

(re-frame/reg-fx
 ::listen-to-network-info
 (fn []
   (add-net-info-listener)))

(re-frame/reg-fx
 :network/notify-status-go
 (fn [[network-type expensive?]]
   (status/connection-change network-type expensive?)))
