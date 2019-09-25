(ns status-im.ui.components.icons.vector-icons
  (:require [status-im.ui.components.react :as react]
            [status-im.ui.components.colors :as colors]
            [status-im.utils.platform :as platform]
            [clojure.string :as string])
  (:refer-clojure :exclude [use]))

(defn- match-color [color]
  (cond
    (keyword? color)
    (case color
      :dark colors/black
      :gray colors/gray
      :blue colors/blue
      :active colors/blue
      :white colors/white
      :red colors/red
      colors/black)
    (string? color)
    color
    :else
    colors/black))

(def icons
  (when platform/desktop?
    {"add" (js/require "./desktop/resources/add.png")
     "address" (js/require "./desktop/resources/address.png")
     "add_contact" (js/require "./desktop/resources/add_contact.png")
     "arrow_left" (js/require "./desktop/resources/arrow_left.png")
     "arrow_right" (js/require "./desktop/resources/arrow_right.png")
     "arrow_up" (js/require "./desktop/resources/arrow_up.png")
     "back" (js/require "./desktop/resources/back.png")
     "backspace" (js/require "./desktop/resources/backspace.png")
     "bell" (js/require "./desktop/resources/bell.png")
     "browser" (js/require "./desktop/resources/browser.png")
     "camera" (js/require "./desktop/resources/camera.png")
     "cancel" (js/require "./desktop/resources/cancel.png")
     "change" (js/require "./desktop/resources/change.png")
     "check" (js/require "./desktop/resources/check.png")
     "close" (js/require "./desktop/resources/close.png")
     "commands" (js/require "./desktop/resources/commands.png")
     "copy" (js/require "./desktop/resources/copy.png")
     "corner_left_bottom" (js/require "./desktop/resources/corner_left_bottom.png")
     "corner_left_top" (js/require "./desktop/resources/corner_left_top.png")
     "corner_right_bottom" (js/require "./desktop/resources/corner_right_bottom.png")
     "corner_right_top" (js/require "./desktop/resources/corner_right_top.png")
     "dapp" (js/require "./desktop/resources/dapp.png")
     "delete" (js/require "./desktop/resources/delete.png")
     "desktop" (js/require "./desktop/resources/desktop.png")
     "download" (js/require "./desktop/resources/download.png")
     "dropdown" (js/require "./desktop/resources/dropdown.png")
     "dropdown_up" (js/require "./desktop/resources/dropdown_up.png")
     "edit" (js/require "./desktop/resources/edit.png")
     "filter" (js/require "./desktop/resources/filter.png")
     "fingerprint" (js/require "./desktop/resources/fingerprint.png")
     "flash" (js/require "./desktop/resources/flash.png")
     "flash_active" (js/require "./desktop/resources/flash_active.png")
     "group_chat" (js/require "./desktop/resources/group_chat.png")
     "help" (js/require "./desktop/resources/help.png")
     "history" (js/require "./desktop/resources/history.png")
     "home" (js/require "./desktop/resources/home.png")
     "home_1" (js/require "./desktop/resources/home_1.png")
     "icon_action_back" (js/require "./desktop/resources/icon_action_back.png")
     "icon_action_forward" (js/require "./desktop/resources/icon_action_forward.png")
     "icon_action_fullscreen_collapse" (js/require "./desktop/resources/icon_action_fullscreen_collapse.png")
     "icon_action_fullscreen_expand" (js/require "./desktop/resources/icon_action_fullscreen_expand.png")
     "icon_arrow_top" (js/require "./desktop/resources/icon_arrow_top.png")
     "icon_avatar" (js/require "./desktop/resources/icon_avatar.png")
     "icon_check_on" (js/require "./desktop/resources/icon_check_on.png")
     "icon_close_light_gray" (js/require "./desktop/resources/icon_close_light_gray.png")
     "icon_forward_gray" (js/require "./desktop/resources/icon_forward_gray.png")
     "icon_lock_gray" (js/require "./desktop/resources/icon_lock_gray.png")
     "icon_lock_white" (js/require "./desktop/resources/icon_lock_white.png")
     "icon_menu_group" (js/require "./desktop/resources/icon_menu_group.png")
     "icon_money_white" (js/require "./desktop/resources/icon_money_white.png")
     "icon_muted" (js/require "./desktop/resources/icon_muted.png")
     "icon_notifications_on" (js/require "./desktop/resources/icon_notifications_on.png")
     "icon_phone_white" (js/require "./desktop/resources/icon_phone_white.png")
     "ic_background" (js/require "./desktop/resources/ic_background.png")
     "ic_foreground" (js/require "./desktop/resources/ic_foreground.png")
     "ic_stat_status_notification" (js/require "./desktop/resources/ic_stat_status_notification.png")
     "info" (js/require "./desktop/resources/info.png")
     "in_contacts" (js/require "./desktop/resources/in_contacts.png")
     "keycard" (js/require "./desktop/resources/keycard.png")
     "keycard_logo" (js/require "./desktop/resources/keycard_logo.png")
     "language" (js/require "./desktop/resources/language.png")
     "launch_logo" (js/require "./desktop/resources/launch_logo.png")
     "link" (js/require "./desktop/resources/link.png")
     "logo" (js/require "./desktop/resources/logo.png")
     "log_out" (js/require "./desktop/resources/log_out.png")
     "mailserver" (js/require "./desktop/resources/mailserver.png")
     "make_admin" (js/require "./desktop/resources/make_admin.png")
     "max" (js/require "./desktop/resources/max.png")
     "message" (js/require "./desktop/resources/message.png")
     "mobile" (js/require "./desktop/resources/mobile.png")
     "more" (js/require "./desktop/resources/more.png")
     "network" (js/require "./desktop/resources/network.png")
     "next" (js/require "./desktop/resources/next.png")
     "notification" (js/require "./desktop/resources/notification.png")
     "one_on_one_chat" (js/require "./desktop/resources/one_on_one_chat.png")
     "password" (js/require "./desktop/resources/password.png")
     "paste" (js/require "./desktop/resources/paste.png")
     "photo" (js/require "./desktop/resources/photo.png")
     "profile" (js/require "./desktop/resources/profile.png")
     "public_chat" (js/require "./desktop/resources/public_chat.png")
     "qr" (js/require "./desktop/resources/qr.png")
     "receive" (js/require "./desktop/resources/receive.png")
     "refresh" (js/require "./desktop/resources/refresh.png")
     "remove_contact" (js/require "./desktop/resources/remove_contact.png")
     "reply" (js/require "./desktop/resources/reply.png")
     "rotate_camera" (js/require "./desktop/resources/rotate_camera.png")
     "search" (js/require "./desktop/resources/search.png")
     "security" (js/require "./desktop/resources/security.png")
     "send" (js/require "./desktop/resources/send.png")
     "settings" (js/require "./desktop/resources/settings.png")
     "settings_advanced" (js/require "./desktop/resources/settings_advanced.png")
     "share" (js/require "./desktop/resources/share.png")
     "stickers" (js/require "./desktop/resources/stickers.png")
     "text" (js/require "./desktop/resources/text.png")
     "tiny_arrow_down" (js/require "./desktop/resources/tiny_arrow_down.png")
     "tiny_check" (js/require "./desktop/resources/tiny_check.png")
     "tiny_clear" (js/require "./desktop/resources/tiny_clear.png")
     "tiny_edit" (js/require "./desktop/resources/tiny_edit.png")
     "tiny_external" (js/require "./desktop/resources/tiny_external.png")
     "tiny_group" (js/require "./desktop/resources/tiny_group.png")
     "tiny_lock" (js/require "./desktop/resources/tiny_lock.png")
     "tiny_lock_broken" (js/require "./desktop/resources/tiny_lock_broken.png")
     "tiny_new_contact" (js/require "./desktop/resources/tiny_new_contact.png")
     "tiny_pending" (js/require "./desktop/resources/tiny_pending.png")
     "tiny_public" (js/require "./desktop/resources/tiny_public.png")
     "tiny_reply" (js/require "./desktop/resources/tiny_reply.png")
     "tiny_settings" (js/require "./desktop/resources/tiny_settings.png")
     "tiny_tribute_to_talk" (js/require "./desktop/resources/tiny_tribute_to_talk.png")
     "tribute_to_talk" (js/require "./desktop/resources/tribute_to_talk.png")
     "two_arrows" (js/require "./desktop/resources/two_arrows.png")
     "username" (js/require "./desktop/resources/username.png")
     "user_profile" (js/require "./desktop/resources/user_profile.png")
     "user_profile_1" (js/require "./desktop/resources/user_profile_1.png")
     "wallet" (js/require "./desktop/resources/wallet.png")
     "warning" (js/require "./desktop/resources/warning.png")}))

(defn icon-source [name]
  (let [file-name (string/replace (clojure.core/name name) "-" "_")]
    (if platform/desktop?
      {:source (get icons file-name)}
      {:source {:uri (keyword (clojure.core/name name))}})))

(defn icon
  ([name] (icon name nil))
  ([name {:keys [color resize-mode container-style
                 accessibility-label width height]
          :or   {accessibility-label :icon}}]
   ^{:key name}
   [react/view
    {:style               (or
                           container-style
                           {:width  (or width 24)
                            :height (or height 24)})
     :accessibility-label accessibility-label}
    [react/image (merge {:style  (cond-> {:width  (or width 24)
                                          :height (or height 24)}

                                   resize-mode
                                   (assoc :resize-mode resize-mode)

                                   color
                                   (assoc :tint-color (match-color color)))}
                        (icon-source name))]]))

(defn tiny-icon
  ([name] (tiny-icon name {}))
  ([name options]
   (icon name (merge {:width 16 :height 16}
                     options))))
