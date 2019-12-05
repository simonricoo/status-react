(ns status-im.ethereum.transactions.core
  (:require [re-frame.core :as re-frame]
            [clojure.set :as clojure.set]
            [clojure.string :as string]
            [status-im.constants :as constants]
            [status-im.ethereum.decode :as decode]
            [status-im.ethereum.eip55 :as eip55]
            [status-im.ethereum.encode :as encode]
            [status-im.ethereum.json-rpc :as json-rpc]
            [status-im.ethereum.core :as ethereum]
            [status-im.ethereum.eip55 :as eip55]
            [status-im.ethereum.tokens :as tokens]
            [status-im.utils.fx :as fx]
            [status-im.utils.money :as money]
            [status-im.wallet.core :as wallet]
            [status-im.react-native.js-dependencies :as rn-dependencies]
            [taoensso.timbre :as log]))

(def confirmations-count-threshold 12)

(def etherscan-supported? #{:testnet :mainnet :rinkeby})

(let [network->subdomain {:testnet "ropsten" :rinkeby "rinkeby"}]
  (defn get-transaction-details-url [chain hash]
    {:pre [(keyword? chain) (string? hash)]
     :post [(or (nil? %) (string? %))]}
    (when (etherscan-supported? chain)
      (let [network-subdomain (when-let [subdomain (network->subdomain chain)]
                                (str subdomain "."))]
        (str "https://" network-subdomain "etherscan.io/tx/" hash)))))

(def default-erc20-token
  {:symbol   :ERC20
   :decimals 18
   :name     "ERC20"})

(defn direction
  [address to]
  (if (= (string/lower-case address) (string/lower-case to))
    :inbound
    :outbound))

(defn- parse-token-transfer
  [chain-tokens contract]
  (let [{:keys [nft? symbol] :as token}  (get chain-tokens contract
                                              default-erc20-token)]
    {:symbol        symbol
     :token         token
     ;; NOTE(goranjovic) - just a flag we need when we merge this entry
     ;; with the existing entry in the app, e.g. transaction info with
     ;; gas details, or a previous transfer entry with old confirmations
     ;; count.
     :transfer      true}))

(defn enrich-transfer
  [chain-tokens
   {:keys [address blockNumber timestamp type transaction receipt from txStatus
           txHash gasPrice gasUsed contract value gasLimit input nonce to type id] :as transfer}]
  (let [erc20?  (= type "erc20")
        failed? (= txStatus "0x0")]
    (merge {:address   (eip55/address->checksum address)
            :id        id
            :block     (str (decode/uint blockNumber))
            :timestamp (* (decode/uint timestamp) 1000)
            :gas-used  (str (decode/uint gasUsed))
            :gas-price (str (decode/uint gasPrice))
            :gas-limit (str (decode/uint gasLimit))
            :nonce     (str (decode/uint nonce))
            :hash      txHash
            :data      input
            :from      from
            :to        to
            :type      (if failed?
                         :failed
                         (direction address to))
            :value     (str (decode/uint value))}
           (if erc20?
             (parse-token-transfer chain-tokens contract)
             ;; this is not a ERC20 token transaction
             {:symbol :ETH}))))

(defn enrich-transfers
  [chain-tokens transfers]
  (mapv (fn [transfer]
          (enrich-transfer chain-tokens transfer))
        transfers))

;; -----------------------------------------------
;; transactions api
;; -----------------------------------------------

(fx/defn watch-transaction
  "Set a watch for the given transaction
   `watch-params` needs to contain a `trigger-fn` and `on-trigger` functions
   `trigger-fn` is a function that returns true if the watch has been triggered
   `on-trigger` is a function that returns the effects to apply when the
   transaction has been triggered"
  [{:keys [db]} transaction-id {:keys [trigger-fn on-trigger] :as watch-params}]
  (when (and (fn? trigger-fn)
             (fn? on-trigger))
    {:db (assoc-in db [:ethereum/watched-transactions transaction-id]
                   watch-params)}))

(fx/defn check-transaction
  "Check if the transaction has been triggered and applies the effects returned
   by `on-trigger` if that is the case"
  [{:keys [db] :as cofx} {:keys [hash] :as transaction}]
  (when-let [watch-params
             (get-in db [:ethereum/watched-transactions hash])]
    (let [{:keys [trigger-fn on-trigger]} watch-params]
      (when (trigger-fn db transaction)
        (fx/merge cofx
                  {:db (update db :ethereum/watched-transactions
                               dissoc hash)}
                  (on-trigger transaction))))))

(fx/defn check-watched-transactions
  [{:keys [db] :as cofx}]
  (let [watched-transactions
        (select-keys (get-in db [:wallet :transactions])
                     (keys (get db :ethereum/watched-transactions)))]
    (apply fx/merge
           cofx
           (map (fn [[_ transaction]]
                  (check-transaction transaction))
                watched-transactions))))

(fx/defn add-transfer
  "We determine a unique id for the transfer before adding it because some
   transaction can contain multiple transfers and they would overwrite each other
   in the transfer map if identified by hash"
  [{:keys [db] :as cofx} {:keys [hash id address] :as transfer}]
  (let [transfer-by-hash (get-in db [:wallet :accounts address :transactions hash])
        transfer-by-id   (get-in db [:wallet :accounts address :transactions id])]
    (when-let [unique-id (when-not (or transfer-by-id
                                       (= transfer transfer-by-hash))
                           (if (and transfer-by-hash
                                    (not (= :pending
                                            (:type transfer-by-hash))))
                             id
                             hash))]
      (fx/merge cofx
                {:db (assoc-in db [:wallet :accounts address :transactions unique-id]
                               (assoc transfer :hash unique-id))}
                (check-transaction transfer)))))

;(fx/defn add-transfer-etherscan
;         "We determine a unique id for the transfer before adding it because some
;          transaction can contain multiple transfers and they would overwrite each other
;          in the transfer map if identified by hash"
;         [{:keys [db] :as cofx} {:keys [hash id address] :as transfer}]
;         (let [transfer-by-hash (get-in db [:wallet :accounts address :transactions-etherscan hash])
;               transfer-by-id   (get-in db [:wallet :accounts address :transactions-etherscan id])]
;           (when-let [unique-id (when-not (or transfer-by-id
;                                              (= transfer transfer-by-hash))
;                                  (if (and transfer-by-hash
;                                           (not (= :pending
;                                                   (:type transfer-by-hash))))
;                                    id
;                                    hash))]
;             (fx/merge cofx
;                       {:db (assoc-in db [:wallet :accounts address :transactions-etherscan unique-id]
;                                      (assoc transfer :hash unique-id))}
;                       (check-transaction transfer)))))


(fx/defn new-transfers
  {:events [::new-transfers]}
  [{:keys [db] :as cofx} transfers historical?]
         (log/debug "=== new-transfers got " (count transfers))
  (let [effects (cond-> (map add-transfer transfers)
                  ;;NOTE: we only update the balance for new transfers and not historical ones
                  (not historical?) (conj (wallet/update-balances (into [] (reduce (fn [acc {:keys [address]}]
                                                                                     (conj acc address))
                                                                                   #{}
                                                                                   transfers)))))]
    (apply fx/merge cofx effects)))

(fx/defn new-transfers-etherscan
         {:events [::new-transfers-etherscan-eth]}
         [{:keys [db] :as cofx} transfers historical?]
         (let [effects (cond-> (map add-transfer transfers)
                               ;;NOTE: we only update the balance for new transfers and not historical ones
                               (not historical?) (conj (wallet/update-balances (into [] (reduce (fn [acc {:keys [address]}]
                                                                                                  (conj acc address))
                                                                                                #{}
                                                                                                transfers)))))]
           (apply fx/merge cofx effects)))

(fx/defn handle-token-history
  [{:keys [db]} transactions]
  {:db (update-in db
                  [:wallet :transactions]
                  merge transactions)})

(re-frame/reg-fx
  ::get-transfers
  (fn [{:keys [chain-tokens from-block to-block historical?]
        :or {from-block "0"
             to-block nil}}]
    (json-rpc/call
      {:method "wallet_getTransfers"
       :params [(encode/uint from-block) (encode/uint to-block)]
       :on-success #(re-frame/dispatch [::new-transfers (enrich-transfers chain-tokens %) historical?])})))


(defn etherscan-tx-to-statux-tx
  [address token? {:keys [hash] :as etherscan-tx}]
  (let [transaction (-> etherscan-tx
                        (dissoc :transactionIndex :confirmations :isError :cumulativeGasUsed :tokenName :tokenDecimal :tokenSymbol)
                        (clojure.set/rename-keys {:hash :txHash
                                                  :gas :gasLimit
                                                  :timeStamp :timestamp
                                                  :blockHash :blockhash
                                                  :txreceipt_status :txStatus
                                                  :contractAddress :contract
                                                  })
                        (assoc :address address :id hash))
        to-hex (select-keys transaction [:gasPrice :gasUsed :gasLimit :timestamp :nonce :value :txStatus :blockNumber])
        hexed (into  {} (map #(identity [(first %) (encode/uint (second %))]) to-hex))
        token-info (if token? {:type "erc20" :txStatus "0x1"} {:type "eth" :contract "0x0000000000000000000000000000000000000000"})]
  (merge transaction hexed token-info)
  ))

(defn convert-etherscan-txlist-to-status-txs
  [etherscan-txlist address token?]
  (map (partial etherscan-tx-to-statux-tx address token?) etherscan-txlist))


(re-frame/reg-fx
::get-transfers-from-etherscan
(fn [{:keys [chain-tokens from-block to-block historical? address]
      :or {from-block 0
           to-block 99999999}}]
  (let [page 0
        offset 0
        sort-order "desc"
        contract-address ""]
  (->
       (.tokentx (.-account rn-dependencies/etherscan-api) address contract-address from-block to-block sort-order)
      (.then #(do
                (re-frame/dispatch [::new-transfers-etherscan-eth
                                    (enrich-transfers chain-tokens (convert-etherscan-txlist-to-status-txs (:result (js->clj % :keywordize-keys true)) address true) )
                                  historical?])))
      (.catch #(log/info "=== GOT TXLIST ERROR: " %)))
  (->
    (.txlist (.-account rn-dependencies/etherscan-api) address from-block to-block page offset sort-order)
    (.then #(do
              (re-frame/dispatch [::new-transfers-etherscan-eth
                                  (enrich-transfers chain-tokens (convert-etherscan-txlist-to-status-txs (:result (js->clj % :keywordize-keys true)) address false) )
                                  historical?])))
    (.catch #(log/info "=== GOT TXLIST ERROR: " %)))
  )))



(fx/defn initialize
  [{:keys [db] :as cofx}]
  (let [{:keys [:wallet/all-tokens]} db
        wallet (get-in db [:wallet ])
        wallet-all-tokens (get-in db [:wallet/all-tokens ])
        chain (ethereum/chain-keyword db)
        chain-tokens (into {} (map (juxt :address identity)
                                   (tokens/tokens-for all-tokens chain)))]
    {
     ::get-transfers {:chain-tokens chain-tokens
                      :historical? true}
     ;::get-transfers-from-etherscan {:chain-tokens chain-tokens
     ;                           :address (ethereum/default-address db)
     ;                               :historical? true}
     }))
