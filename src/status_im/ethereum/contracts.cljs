(ns status-im.ethereum.contracts
  (:require [status-im.ethereum.core :as ethereum]))

(def contracts
  {:status/snt
   {:mainnet "0x744d70fdbe2ba4cf95131626614a1763df805b9e"
    :testnet "0xc55cf4b03948d7ebc8b9e8bad92643703811d162"}
   :status/tribute-to-talk
   {:testnet "0xC61aa0287247a0398589a66fCD6146EC0F295432"}
   :status/stickers
   {:testnet "0x8cc272396be7583c65bee82cd7b743c69a87287d"
    :mainnet ""}
   :status/sticker-market
   {:testnet "0x6CC7274aF9cE9572d22DFD8545Fb8c9C9Bcb48AD"
    :mainnet ""}
   :status/sticker-pack
   {:testnet "0xf852198d0385c4b871e0b91804ecd47c6ba97351"
    :mainnet ""}})

(defn get-address
  [db contract]
  (let [chain-keyword (ethereum/chain-keyword db)]
    (get-in contracts [contract chain-keyword])))
