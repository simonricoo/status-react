(ns status-im.swarm.core
  (:refer-clojure :exclude [cat])
  (:require [cljs.spec.alpha :as spec]
            [re-frame.core :as re-frame]
            [status-im.contact.db :as contact.db]
            [status-im.ethereum.core :as ethereum]
            [status-im.js-dependencies :as dependencies]
            [status-im.native-module.core :as status]
            [status-im.utils.fx :as fx]
            [status-im.utils.http :as http]
            [status-im.utils.types :as types]))

(spec/def :swarm.timeline/protocol "timeline")
(spec/def :swarm.timeline/version "1.0.0")
(spec/def :swarm.timeline/timestamp integer?)
(spec/def :swarm.timeline/author string?)
(spec/def :swarm.timeline/type string?)
(spec/def :swarm.timeline/content map?)
(spec/def :swarm.timeline/references (spec/coll-of string?))
(spec/def :swarm.timeline/signature string?)
(spec/def :swarm.timeline/previous string?)

(spec/def :swarm.timeline/chapter
  (spec/keys :req-un [:swarm.timeline/protocol
                      :swarm.timeline/version
                      :swarm.timeline/timestamp
                      :swarm.timeline/author
                      :swarm.timeline/type
                      :swarm.timeline/content]
             :opt-un [:swarm.timeline/references
                      :swarm.timeline/previous
                      :swarm.timeline/signature]))

(defn make-chapter
  [{:keys [now db]}]
  (types/clj->json
   (let [account (:account/account db)]
     {:protocol "timeline"
      :version "1.0.0"
      :timestamp now
      :author (ethereum/current-address db)
      :type "application/json"
      :content (select-keys account [:name :photo-path :ens-name])})))

(defn utils [] (dependencies/web3-utils))

(def status-profile-topic "status-profile")

;; we currently use a swarm gateway but this detail is not relevant
;; outside of this namespace
(def swarm-gateway "https://swarm-gateways.net")
#_(def swarm-gateway "https://swarm.epiclabs.io")
#_(def swarm-gateway "https://test-swarm.status.im")

(def bzz-url (str swarm-gateway "/bzz:/"))
(def bzz-feed-url (str swarm-gateway "/bzz-feed:/"))

(def gateway-timeout 5000)

(defn bzz-read-feed-url
  [name user]
  (str bzz-feed-url
       "?name=" name
       "&user=" user))

(defn bzz-read-feed-template-url
  [name user]
  (str (bzz-read-feed-url name user) "&meta=1"))

(defn bzz-post-feed-update-url
  [{:keys [topic user level time protocol-version signature]}]
  (str bzz-feed-url
       "?topic=" topic
       "&user=" user
       "&level=" level
       "&time=" time
       "&protocolVersion=" protocol-version
       "&signature=" signature))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn number-to-hex
  [n]
  (subs (.numberToHex (utils) n) 2))

(defn pad-left
  [str n]
  (.padLeft (utils) str n))

(defn pad-right
  [str n]
  (.padRight (utils) str n))

(defn UInt32LE
  "Converts an integer into little endian in an hex encoded string"
  [i]
  (->> i
       number-to-hex
       reverse
       (partition 2)
       (map reverse)
       flatten
       (apply str)))

(defn utf8-to-hex
  [s]
  (subs (.utf8ToHex (utils) s) 2))

(defn sha3 [s]
  (.sha3 (utils) s))

(defn hex-to-bytes [s]
  (.hexToBytes (utils) s))

(defn bytes-to-hex [b]
  (.bytesToHex (utils) b))

(defn digest
  [data {:keys [feed epoch protocolVersion]}]
  (let [{:keys [topic user]} feed
        {:keys [time level]} epoch
        digest (str "0x"
                    (pad-left (number-to-hex 0) 2)
                    (pad-left "" 14)
                    (subs topic 2)
                    (subs user 2)
                    (pad-right (UInt32LE time) 14)
                    (pad-left (number-to-hex level) 2)
                    (utf8-to-hex data))]
    (sha3 digest)))

(re-frame/reg-fx
 ::sign-digest
 (fn [{:keys [digest callback]}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; swarm api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn upload-data
  [{:keys [data on-success on-failure]}]
  {:http-post {:url bzz-url
               :data data
               :opts {:headers {"Content-Type" "application/json"}}
               :timeout-ms 10000
               :on-success
               (fn [{:keys [response-body]}]
                 (println response-body)
                 (println :test)
                 (on-success response-body))
               :on-failure
               (fn [error] (on-failure error))}})

(defn post-feed-update
  [{:keys [data feed-template] :as update-params} signature]
  (let [{:keys [feed epoch protocolVersion]} feed-template
        {:keys [topic user]} feed
        {:keys [time level]} epoch
        url (bzz-post-feed-update-url
             {:topic topic
              :user user
              :level level
              :time time
              :protocol-version protocolVersion
              :signature signature})
        on-success
        (fn [{:keys [response-body]}]
          (println (assoc update-params
                          :response
                          (js/JSON.parse response-body))))
        on-error
        (fn [error]
          (println (assoc update-params
                          :error
                          error)))]
    (http/post url data on-success on-error {:timeout-ms gateway-timeout})))

(defn sign-feed-update
  [{:keys [data feed-template] :as update-params}]
  (status/private-sign-hash (digest data feed-template)
                            (fn [signature]
                              (post-feed-update update-params
                                                signature))))

(defn get-feed-template
  [{:keys [name user] :as update-params}]
  (let [on-success
        (fn [response]
          (let [feed-template (js->clj (js/JSON.parse response)
                                       :keywordize-keys true)]
            (sign-feed-update (assoc update-params
                                     :feed-template
                                     feed-template))))
        on-error
        (fn [error]
          (println (assoc update-params
                          :error
                          error)))]
    (http/get (bzz-read-feed-template-url name user)
              on-success
              on-error
              {:timeout-ms gateway-timeout})))

(defn read-file
  [{:keys [swarm-hash on-success on-failure]}]
  (http/get (str bzz-url swarm-hash "/")
            on-success
            on-failure
            {:timeout-ms gateway-timeout}))

(defn read-feed
  [{:keys [user name on-success on-failure]}]
  {:http-get {:url (bzz-read-feed-url name user)
              :timeout-ms gateway-timeout
              :on-success on-success
              :on-failure #(println :read-error %) #_on-failure}})

(fx/defn verify-feed
  [cofx update-params]
  (read-feed (assoc update-params
                    :on-success println
                    :on-error println)))

(fx/defn update-feed
  [{:keys [db] :as cofx} feed-name data]
  (when-let [user-address (ethereum/current-address db)]
    (upload-data {:data data
                  :on-success
                  (fn [swarm-hash]
                    (get-feed-template {:user user-address
                                        :name feed-name
                                        :data swarm-hash}))
                  :on-failure println})))

(fx/defn update-profile
  {:events [:swarm/update-profile]}
  [{:keys [db] :as cofx}]
  (when-let [user-address (ethereum/current-address db)]
    (update-feed cofx
                 status-profile-topic
                 (make-chapter cofx))))

(fx/defn read-profile
  {:events [:swarm/read-profile]}
  [{:keys [db] :as cofx} public-key]
  (when-let [user-address (if public-key
                            (-> public-key
                                contact.db/public-key->address
                                ethereum/normalized-address)
                            (ethereum/current-address db))]
    (read-feed {:user user-address
                :name status-profile-topic
                :on-success
                (fn [swarm-hash]
                  (read-file {:swarm-hash swarm-hash
                              :on-success println
                              :on-failure println}))
                :on-failure println})))
