(ns status-im.test.node.core
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.utils.config :as config]
            [status-im.node.core :as node]))

(deftest start-test
  (let [address "a"
        db {:multiaccounts/multiaccounts {address {:installation-id "id"}}}]
    (testing "installation-id"
      (let [actual (node/get-new-config db address)]
        (is (= "id" (:InstallationID actual)))))))
