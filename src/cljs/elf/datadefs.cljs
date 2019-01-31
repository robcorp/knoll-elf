(ns elf.datadefs
  (:require [ajax.core :as ajax]))

(defonce essentials-products
  [{:product-group "Seating" :product-type "Lounge Chairs" :product-id :barcelona-chair, :product-name "Barcelona Chair" :lead-times [:std :quick :three-week] :img-src "https://www.knoll.com/media/858/141/205_barcelona_lounge_black_F_v3.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :pollock-chair, :product-name "Pollock Chair" :lead-times [:quick :std] :img-src "https://www.knoll.com/media/754/476/Thumb_pollock-arm-chair,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :wassily-chair, :product-name "Wassily Chair" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/432/603/Thumb_wassily_880,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :risom-lounge-chair, :product-name "Risom Lounge Chair" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/955/28/Thumb_risom-lounge-chair-880,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :womb-chair, :product-name "Womb Chair " :lead-times [:std] :img-src "https://www.knoll.com/media/986/679/Thumb_womb-chair-default-image,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :womb-chair-medium, :product-name "Womb Chair - Medium" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/539/531/Thumb_medium-womb-880,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :womb-chair-child, :product-name "Womb Chair - Child's" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/351/283/Thumb_childs-womb-chair-hero,4.png"}
   {:product-group "Seating" :product-type "Lounge Chairs" :product-id :womb-settee, :product-name "Womb Settee" :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/775/888/Thumb_womb-settee-default-image,4.png"}
   {:product-group "Seating" :product-type "Multi-Use Chairs" :product-id :ollo-chair, :product-name "Ollo", :lead-times [:std :three-week] :img-src "https://www.knoll.com/media/661/623/Thumb_ollo-default-thumb,4.png"}
   {:product-group "Seating" :product-type "Multi-Use Chairs" :product-id :newson-aluminum-chair, :product-name "Newson Aluminum Chair", :lead-times [:std :quick :three-week] :img-src "https://www.knoll.com/media/909/711/Thumb_newson-aluminum-chair-thumbnail,4.png"}
   {:product-group "Seating" :product-type "Multi-Use Chairs" :product-id :bertoia-molded-shell-side-chair, :product-name "Bertoia Molded Shell Side Chair", :lead-times [:three-week] :img-src "https://www.knoll.com/media/47/66/Thumb_Thumb_bertoia-molded-shell-chair-default,4.png"}])

(defonce filtered-products
  (group-by :product-type essentials-products))

(defonce lead-time-filters
  [{:lead-time :all :id "all-lead" :label "All lead Times" :value false :li-id "all" :li-class "all-lead-type"}
   {:lead-time :quick :id "quick-ship" :label "Quickship" :value false :li-id "quickshiplist" :li-class "quick-ship-type"}
   {:lead-time :three-week :id "three-week-ship" :label "Three week Ship" :value false :li-id "three-week-ship-list" :li-class "three-week-ship"}
   {:lead-time :std :id "standard-ship" :label "Standard Ship" :value false :li-id "standard-ship-list" :li-class "standard-ship"}])

(comment 
  (def textile (atom nil))

  (defn ajax-handler [resp]
    #_(js/alert (str "Got response from AJAX call: " resp))
    (println resp)
    (reset! textile resp))

  (def url "https://www.knoll.com/textiles/2085")

  (ajax/GET url {:handler ajax-handler :response-format :json :keywords? true}))
