(ns elf.datadefs
  (:require [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval] :as spctr]))

(defonce all-products
  [{:product-group "Seating"
    :product-type "Lounge Chairs"
    :product-id "barcelona-chair"
    :product-name "Barcelona Chair"
    :short-text "Barcelona Chair short text"
    :lead-times ["std" "quick" "three-week"]
    :thumb-img-src "https://www.knoll.com/media/858/141/205_barcelona_lounge_black_F_v3.png"
    :hero1-img-src "https://www.knoll.com/media/858/141/205_barcelona_lounge_black_F_v3.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "pollock-arm-chair" :product-name "Pollock Chair" :short-text "Pollock Chair short text" :lead-times ["quick" "std"] :thumb-img-src "https://www.knoll.com/media/754/476/Thumb_pollock-arm-chair,4.png" :hero1-img-src "https://www.knoll.com/media/754/476/Thumb_pollock-arm-chair,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "wassily-chair" :product-name "Wassily Chair" :short-text "Wassily Chair short text" :lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/432/603/Thumb_wassily_880,4.png" :hero1-img-src "https://www.knoll.com/media/432/603/Thumb_wassily_880,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "risom-lounge-chair" :product-name "Risom Lounge Chair" :short-text "Risom Lounge Chair short text" :lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/955/28/Thumb_risom-lounge-chair-880,4.png" :hero1-img-src "https://www.knoll.com/media/955/28/Thumb_risom-lounge-chair-880,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "womb-chair" :product-name "Womb Chair" :short-text "Womb Chair short text" :lead-times ["std"] :thumb-img-src "https://www.knoll.com/media/986/679/Thumb_womb-chair-default-image,4.png" :hero1-img-src "https://www.knoll.com/media/986/679/Thumb_womb-chair-default-image,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "womb-chair-medium" :product-name "Womb Chair - Medium" :short-text "Womb Chair - Medium short text" :lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/539/531/Thumb_medium-womb-880,4.png" :hero1-img-src "https://www.knoll.com/media/539/531/Thumb_medium-womb-880,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "womb-chair-child" :product-name "Womb Chair - Child's" :short-text "Womb Chair - Child's short text":lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/351/283/Thumb_childs-womb-chair-hero,4.png" :hero1-img-src "https://www.knoll.com/media/351/283/Thumb_childs-womb-chair-hero,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id "womb-settee" :product-name "Womb Settee" :short-text "Womb Settee short text":lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/775/888/Thumb_womb-settee-default-image,4.png" :hero1-img-src "https://www.knoll.com/media/775/888/Thumb_womb-settee-default-image,4.png"}
   {:product-group "Seating" :product-type "Multi-Use Chairs" :product-id "ollo-chair" :product-name "Ollo" :short-text "Ollo short text":lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/661/623/Thumb_ollo-default-thumb,4.png" :hero1-img-src "https://www.knoll.com/media/661/623/Thumb_ollo-default-thumb,4.png"}
   {:product-group "Seating" :product-type "Multi-Use Chairs" :product-id "newson-aluminum-chair" :product-name "Newson Aluminum Chair" :short-text "Newson Aluminum Chair short text" :lead-times ["std" "quick" "three-week"] :thumb-img-src "https://www.knoll.com/media/909/711/Thumb_newson-aluminum-chair-thumbnail,4.png" :hero1-img-src "https://www.knoll.com/media/909/711/Thumb_newson-aluminum-chair-thumbnail,4.png"}
   {:product-group "Seating" :product-type "Multi-Use Chairs" :product-id "bertoia-molded-shell-side-chair" :product-name "Bertoia Molded Shell Side Chair" :short-text "Bertoia Molded Shell Side Chair short text":lead-times ["std" "three-week"] :thumb-img-src "https://www.knoll.com/media/47/66/Thumb_Thumb_bertoia-molded-shell-chair-default,4.png" :hero1-img-src "https://www.knoll.com/media/47/66/Thumb_Thumb_bertoia-molded-shell-chair-default,4.png"}])

(defonce filtered-products
  (group-by :product-type all-products))

(defonce lead-time-filters
  [{:lead-time "all" :id "all-lead" :label "All lead Times" :value true :li-id "all" :li-class "all-lead-type"}
   {:lead-time "quick" :id "quick-ship" :label "Quickship" :value false :li-id "quickshiplist" :li-class "quick-ship-type"}
   {:lead-time "three-week" :id "three-week-ship" :label "Three week Ship" :value false :li-id "three-week-ship-list" :li-class "three-week-ship"}
   {:lead-time "std" :id "standard-ship" :label "Standard Ship" :value false :li-id "standard-ship-list" :li-class "standard-ship"}])

