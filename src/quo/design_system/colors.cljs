(ns quo.design-system.colors
  (:require [reagent.core :as reagent]))

(def white "#FFFFFF")
(def black "#000000")

(def light-theme
  {:positive-01    "rgba(68,208,88,1)"     ; Primary Positive, text, icons color
   :positive-02    "rgba(78,188,96,0.1)"   ; Secondary Positive, Supporting color for success illustrations
   :negative-01    "rgba(255,45,85,1)"     ; Primary Negative, text, icons color
   :negative-02    "rgba(255,45,85,0.1))"  ; Secondary Negative, Supporting color for errors illustrations
   :interactive-01 "rgba(67,96,223,1)"     ; Accent color, buttons, own message, actions,active state
   :interactive-02 "rgba(236,239,252,1)"   ; Light Accent, buttons background, actions background, messages
   :interactive-03 "rgba(255,255,255,0.1)" ; Background for interactive above accent
   :ui-background  "rgba(255,255,255,1)"   ; Default view background
   :ui-01          "rgba(238,242,245,1)"   ; Secondary background
   :ui-02          "rgba(0,0,0,0.1)"       ; Deviders
   :text-01        "rgba(0,0,0,1)"         ; Main text color
   :text-02        "rgba(147,155,161,1)"   ; Secondary text
   :text-03        "rgba(255,255,255,0.7)" ; Secondary on accent
   :text-04        "rgba(67,96,223,1)"     ; Links text color
   :icon-01        "rgba(0,0,0,1)"         ; Primary icons
   :icon-02        "rgba(147,155,161,1)"   ; Secondary icons
   :icon-03        "rgba(255,255,255,0.4)" ; Secondary icons on accent bg
   :icon-04        "rgba(255,255,255,1)"   ; Icons inverse on accent background
   :shadow-01      "rgba(0,9,26,0.12)"     ; Main shadow color
})

(def dark-theme
  {:positive-01    "rgba(68,208,88,1)"
   :positive-02    "rgba(78,188,96,0.1)"
   :negative-01    "rgba(252,95,95,1)"
   :negative-02    "rgba(252,95,95,0.1)"
   :interactive-01 "rgba(97,119,229,1)"
   :interactive-02 "rgba(35,37,47,1)"
   :interactive-03 "rgba(255,255,255,0.1)"
   :ui-background  "rgba(20,20,20,1)"
   :ui-01          "rgba(37,37,40,1)"
   :ui-02          "rgba(0,0,0,0.1)"
   :text-01        "rgba(255,255,255,1)"
   :text-02        "rgba(131,140,145,1)"
   :text-03        "rgba(255,255,255,0.7)"
   :text-04        "rgba(97,119,229,1)"
   :icon-01        "rgba(255,255,255,1)"
   :icon-02        "rgba(131,140,145,1)"
   :icon-03        "rgba(255,255,255,0.4)"
   :icon-04        "rgba(20,20,20,1)"
   :shadow-01      "rgba(0,0,0,0.75)"})

(def theme (reagent/atom light-theme))
