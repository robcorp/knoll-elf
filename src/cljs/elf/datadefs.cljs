(ns elf.datadefs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:require-macros [cljs.spec.test.alpha :as st]))

(comment
  (s/def :lead-time #{:std :quick :three-week})
  (s/def ::lead-times (s/coll-of ::lead-time :distinct true :min-count 1 :max-count 3))
  (s/def ::ProductTypeFamily #{"Seating" "Tables" "Storage" "Power & Data" "Work Tools & Accessories" "Screens & Communications Boards"})

  (s/def ::product-id keyword?)
  (s/def ::product-name string?)
  (s/def ::product (s/keys :req [::product-id ::product-name ::lead-times]))
  (s/def ::products (s/coll-of ::product)))


(def essentials-products
  [{:product-type "Lounge Chairs"
    :product-list [
                   {:product-id :barcelona-chair, :product-name "Barcelona Chair" :lead-times [:std :quick :three-week] :img-src "https://www.knoll.com/media/858/141/205_barcelona_lounge_black_F_v3.png"}
                   {:product-id :pollock-chair, :product-name "Pollock Chair" :lead-times [:quick :std] :img-src "https://www.knoll.com/media/754/476/Thumb_pollock-arm-chair,4.png"}
                   {:product-id :wassily-chair, :product-name "Wassily Chair" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/432/603/Thumb_wassily_880,4.png"}
                   {:product-id :risom-lounge-chair, :product-name "Risom Lounge Chair" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/955/28/Thumb_risom-lounge-chair-880,4.png"}
                   {:product-id :womb-chair, :product-name "Womb Chair " :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/986/679/Thumb_womb-chair-default-image,4.png"}
                   {:product-id :womb-chair-medium, :product-name "Womb Chair - Medium" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/539/531/Thumb_medium-womb-880,4.png"}
                   {:product-id :womb-chair-child, :product-name "Womb Chair - Child's" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/351/283/Thumb_childs-womb-chair-hero,4.png"}
                   {:product-id :womb-settee, :product-name "Womb Settee" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/775/888/Thumb_womb-settee-default-image,4.png"}
                   ]}
   {:product-type "Multi-Use Chairs"
    :product-list [
                   {:product-id :ollo-chair, :product-name "Ollo", :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/661/623/Thumb_ollo-default-thumb,4.png"}
                   {:product-id :newson-aluminum-chair, :product-name "Newson Aluminum Chair", :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/909/711/Thumb_newson-aluminum-chair-thumbnail,4.png"}
                   {:product-id :bertoia-molded-shell-side-chair, :product-name "Bertoia Molded Shell Side Chair", :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/47/66/Thumb_Thumb_bertoia-molded-shell-chair-default,4.png"}
                   ]}])

