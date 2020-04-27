(ns status-im.utils.universal-links.core
  (:require [cljs.spec.alpha :as spec]
            [clojure.string :as string]
            [goog.string :as gstring]
            [re-frame.core :as re-frame]
            [status-im.multiaccounts.model :as multiaccounts.model]
            [status-im.chat.models :as chat]
            [status-im.constants :as constants]
            [status-im.ethereum.ens :as ens]
            [status-im.ethereum.core :as ethereum]
            [status-im.utils.security :as security]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.components.react :as react]
            [status-im.ui.screens.add-new.new-chat.db :as new-chat.db]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.fx :as fx]
            [taoensso.timbre :as log]
            [status-im.wallet.choose-recipient.core :as choose-recipient]))

;; TODO(yenda) investigate why `handle-universal-link` event is
;; dispatched 7 times for the same link

(def public-chat-regex #"(?:https?://join\.)?status[.-]im(?::/)?/(?:chat/public/([a-z0-9\-]+)$|([a-z0-9\-]+))$")
(def profile-regex #"(?:https?://join\.)?status[.-]im(?::/)?/(?:u/(0x.*)$|u/(.*)$|user/(.*))$")
(def browse-regex #"(?:https?://join\.)?status[.-]im(?::/)?/(?:b/(.*)$|browse/(.*))$")

;; domains should be without the trailing slash
(def domains {:external "https://join.status.im"
              :internal "status-im:/"})

(def links {:public-chat "%s/%s"
            :user        "%s/u/%s"
            :browse      "%s/b/%s"})

(defn generate-link [link-type domain-type param]
  (gstring/format (get links link-type)
                  (get domains domain-type)
                  param))

(defn match-url [url regex]
  (some->> url
           (re-matches regex)
           rest
           reverse
           (remove nil?)
           first))

(defn is-request-url? [url]
  (string/starts-with? url "ethereum:"))

(defn universal-link? [url]
  (boolean
   (re-matches constants/regx-universal-link url)))

(defn deep-link? [url]
  (boolean
   (re-matches constants/regx-deep-link url)))

(defn open! [url]
  (log/info "universal-links:  opening " url)
  (if-let [dapp-url (match-url url browse-regex)]
    (when (security/safe-link? url)
      (list-selection/browse-dapp dapp-url))
    ;; We need to dispatch here, we can't openURL directly
    ;; as it is opened in safari on iOS
    (re-frame/dispatch [:handle-universal-link url])))

(fx/defn handle-browse [cofx url]
  (log/info "universal-links: handling browse" url)
  (when (security/safe-link? url)
    {:browser/show-browser-selection url}))

(fx/defn handle-public-chat [cofx public-chat]
  (log/info "universal-links: handling public chat" public-chat)
  (chat/start-public-chat cofx public-chat {}))

(fx/defn handle-view-profile
  [{:keys [db] :as cofx} {:keys [public-key ens-name]}]
  (log/info "universal-links: handling view profile" (or ens-name public-key))
  (cond
    (and public-key (new-chat.db/own-public-key? db public-key))
    (navigation/navigate-to-cofx cofx :my-profile nil)

    public-key
    (navigation/navigate-to-cofx (assoc-in cofx [:db :contacts/identity] public-key) :profile nil)

    ens-name
    (let [chain (ethereum/chain-keyword db)]
      {:resolve-public-key {:chain            chain
                            :contact-identity ens-name
                            :cb               (fn [pub-key]
                                                (cond
                                                  (and pub-key (new-chat.db/own-public-key? db pub-key))
                                                  (re-frame/dispatch [:navigate-to :my-profile])

                                                  pub-key
                                                  (re-frame/dispatch [:chat.ui/show-profile pub-key])

                                                  :else
                                                  (log/info "universal-link: no pub-key for ens-name " ens-name)))}})))

(fx/defn handle-eip681 [cofx url]
  (fx/merge cofx
            (choose-recipient/parse-eip681-uri-and-resolve-ens url)
            (navigation/navigate-to-cofx :wallet nil)))

(defn handle-not-found [full-url]
  (log/info "universal-links: no handler for " full-url))

(defn dispatch-url
  "Dispatch url so we can get access to re-frame/db"
  [url]
  (if-not (nil? url)
    (re-frame/dispatch [:handle-universal-link url])
    (log/debug "universal-links: no url")))

(fx/defn route-url
  "Match a url against a list of routes and handle accordingly"
  [cofx url]
  (cond

    (spec/valid? :global/public-key (match-url url profile-regex))
    (handle-view-profile cofx {:public-key (match-url url profile-regex)})

    (or (ens/valid-eth-name-prefix? (match-url url profile-regex))
        (ens/is-valid-eth-name? (match-url url profile-regex)))
    (handle-view-profile cofx {:ens-name (match-url url profile-regex)})

    (match-url url browse-regex)
    (handle-browse cofx (match-url url browse-regex))

    (is-request-url? url)
    (handle-eip681 cofx url)

    ;; This needs to stay last, as it's a bit of a catch-all regex
    (match-url url public-chat-regex)
    (handle-public-chat cofx (match-url url public-chat-regex))

    :else (handle-not-found url)))

(fx/defn store-url-for-later
  "Store the url in the db to be processed on login"
  [{:keys [db]} url]
  (log/info :store-url-for-later)
  {:db (assoc db :universal-links/url url)})

(fx/defn handle-url
  "Store url in the database if the user is not logged in, to be processed
  on login, otherwise just handle it"
  [cofx url]
  (if (multiaccounts.model/logged-in? cofx)
    (route-url cofx url)
    (store-url-for-later cofx url)))

(fx/defn process-stored-event
  "Return an event description for processing a url if in the database"
  [{:keys [db] :as cofx}]
  (when-let [url (:universal-links/url db)]
    (fx/merge cofx
              {:db (dissoc db :universal-links/url)}
              (handle-url url))))

(defn unwrap-js-url [e]
  (-> e
      (js->clj :keywordize-keys true)
      :url))

(def url-event-listener
  (comp dispatch-url unwrap-js-url))

(defn initialize
  "Add an event listener for handling background->foreground transition
  and handles incoming url if the app has been started by clicking on a link"
  []
  (log/debug "universal-links: initializing")
  (.. react/linking
      (getInitialURL)
      (then dispatch-url))
  (.. react/linking
      (addEventListener "url" url-event-listener)))

(defn finalize
  "Remove event listener for url"
  []
  (log/debug "universal-links: finalizing")
  (.. react/linking
      (removeEventListener "url" url-event-listener)))
