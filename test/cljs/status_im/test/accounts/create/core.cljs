(ns status-im.test.accounts.create.core
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.utils.utils :as utils]
            [status-im.accounts.create.core :as models]))

(deftest on-account-created
  (let [result (models/on-account-created {:random-guid-generator (constantly "")
                                           :signing-phrase        ""
                                           :db                    {}}
                                          {:pubkey   "04de2e21f1642ebee03b9aa4bf1936066124cc89967eaf269544c9b90c539fd5c980166a897d06dd4d3732b38116239f63c89395a8d73eac72881fab802010cb56"
                                           :address  "7e92236392a850980d00d0cd2a4b92886bd7fe7b"
                                           :mnemonic "hello world"}
                                          "password"
                                          true)]
    (is (= (:db result)
           {:accounts/login {:address "7e92236392a850980d00d0cd2a4b92886bd7fe7b", :password "password", :processing true}, :accounts/accounts {"7e92236392a850980d00d0cd2a4b92886bd7fe7b" {:keycard-pairing nil, :address "7e92236392a850980d00d0cd2a4b92886bd7fe7b", :mnemonic "hello world", :remember-syncing-choice? false, :signing-phrase "", :signed-up? true, :keycard-key-uid nil, :syncing-on-mobile-network? false, :name "Dark Woozy Alligatorsnappingturtle", :desktop-notifications? false, :settings {:web3-opt-in? true, :preview-privacy? false, :wallet {:visible-tokens {}}}, :networks nil, :photo-path "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAMAAAC7IEhfAAADAFBMVEX////YjowAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACRS2MAAABAHRSTlP//wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKmfXxgAABnNJREFUeNoBaAaX+QAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAQEBAQEBAQEBAQEBAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAQEBAQEBAQEBAQEBAQEBAQEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQEBAQEBAQEBAQEBAQEBAQEAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAQEBAQEBAQEBAQEBAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAQEBAQEBAQEBAQEBAQEBAQEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQEBAQEBAQEBAQEBAQEBAQEAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAQEBAQEBAQEBAQEBAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAQEBAQEBAQEBAQEBAQEBAQEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQAAAAAAAAAAAAAAAAEBAQEAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAAAAAAAAAAAAAAAAAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAQEAAAAAAAAAAAAAAAABAQEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQAAAAAAAAAAAAAAAAEBAQEAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAAAAAAEBAQEAAAAAAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAQEAAAAAAQEBAQAAAAABAQEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQAAAAABAQEBAAAAAAEBAQEAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAAAAAAEBAQEAAAAAAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQEBAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoRUBAbwwC1cAAAAASUVORK5CYII=", :new-account? nil, :status nil, :keycard-paired-on nil, :seed-backed-up? nil, :network nil, :public-key "04de2e21f1642ebee03b9aa4bf1936066124cc89967eaf269544c9b90c539fd5c980166a897d06dd4d3732b38116239f63c89395a8d73eac72881fab802010cb56", :keycard-instance-uid nil, :installation-id nil}}, :node/on-ready :login, :intro-wizard {:step 1}, :view-id :intro-wizard, :navigation-stack '(:intro-wizard)}))
    (is (= (keys result)
           [:db :accounts.login/clear-web-data :data-store/change-account :status-im.ui.screens.navigation/navigate-to :data-store/base-tx]))))

(deftest intro-step-back
  (testing "Back from choose-key"
    (let [db {:intro-wizard {:step 2}}
          result (get-in (models/intro-step-back {:db db}) [:db :intro-wizard])]
      (is (= result  {:step 1}))))

  (testing "Back from create-code"
    (let [db {:intro-wizard {:step 4 :key-code "qwerty"}}
          result (get-in (models/intro-step-back {:db db}) [:db :intro-wizard])]
      (is (= result  {:step 3 :key-code nil}))))

  (testing "Back from confirm-code"
    (let [db {:intro-wizard {:step 5 :confirm-failure? true}}
          result (get-in (models/intro-step-back {:db db}) [:db :intro-wizard])]
      (is (= result  {:step 4 :key-code nil :confirm-failure? false})))))

(deftest intro-step-forward
  (testing "Forward from choose-key"
    (let [db {:intro-wizard {:step 2}}
          ;; In this case intro-step-forward returns fx/merge result which is an fn
          ;; to be invoked on cofx
          result (get-in ((models/intro-step-forward {:db db}) {:db db}) [:db :intro-wizard])]
      (is (= result {:step 3}))))

  (testing "Forward from generate-key"
    (let [db {:intro-wizard {:step 1}}
          result ((models/intro-step-forward {:db db}) {:db db})]
      (is (= result {:db {:intro-wizard {:step 1 :generating-keys? true}}
                     :intro-wizard/new-onboarding {:n 5 :mnemonic-length 12}}))))

  (testing "Forward from create-code"
    (let [db {:intro-wizard {:step 4 :key-code "qwerty"}}
          result (get-in ((models/intro-step-forward {:db db}) {:db db}) [:db :intro-wizard])]
      (is (= result {:step 5 :key-code nil :stored-key-code "qwerty"}))))

  (testing "Forward from confirm-code (failure case)"
    (with-redefs [utils/vibrate (fn [] "vibrating")]
      (let [db {:intro-wizard {:step 5 :key-code "abcdef" :encrypt-with-password? true :stored-key-code "qwerty"}}
            result (get-in ((models/intro-step-forward {:db db}) {:db db}) [:db :intro-wizard])]
        (is (= result {:step 5 :key-code "abcdef" :confirm-failure? true
                       :encrypt-with-password? true
                       :stored-key-code "qwerty"}))))))

(deftest on-keys-generated
  (testing "Test merging of generated keys into app-db"
    (let [db {:intro-wizard {:step 1 :generating-keys true}}
          accounts [{:id "0x01"}
                    {:id "0x02"}
                    {:id "0x03"}
                    {:id "0x04"}
                    {:id "0x05"}]
          result (get-in (models/on-keys-generated {:db db} {:accounts accounts}) [:db :intro-wizard])]
      (is (= result) {:step 2 :accounts accounts :selected-storage-type :default :selected-id (-> accounts first :id)}))))

(deftest get-new-key-code
  (testing "Add new character to keycode"
    (is (= "abcd" (models/get-new-key-code "abc" "d" true))))
  (testing "Remove trailing character from keycode"
    (is (= "ab" (models/get-new-key-code "abc" :remove true)))))
