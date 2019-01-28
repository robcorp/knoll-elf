(ns elf.views
  (:require
   [re-frame.core :as re-frame]
   [elf.events :as events]
   [elf.subs :as subs]))

(defn essential-product-summary [{:keys [product-id product-name lead-times img-src]} product]
  (let [lead-times-set (set lead-times)]
    ^{:key product-id}
    [:li
     [:a.popup-modal {:href "#essentials-modal"}
      [:div.product-col-image
       [:img {:src img-src :data-no-retina nil}]]
      [:ul.lead-time-status
       (if (contains? lead-times-set :quick)
         [:li.quick-lead-active])
       (if (contains? lead-times-set :three-week)
         [:li.three-ship-active])
       (if (contains? lead-times-set :std)
         [:li.standard-ship-active])]
      [:p product-name]]]))

(defn check-box [id]
  [:input {:type "checkbox"
           :id id
           :class "check-in"
           :checked @(re-frame/subscribe [(keyword 'elf.subs id)])
           :on-change #(re-frame/dispatch [::events/check-box-clicked id])}])

;;; render the Lead Time: filters 
(defn lead-time-filters []
  [:div.select-wrap
   [:h3 "Lead Time:"]
   [:ul.lead-time-list
    [:li#all {:class "lead-time-list-types all-lead-type active"}
     (check-box "all-lead")
     [:label.active {:for "all-lead"} "All lead Times"]]
    [:li#quickshiplist {:class "lead-time-list-types quick-ship-type active"}
     (check-box "quick-ship")
     [:label.active {:for "quick-ship"} "Quickship"]]
    [:li#three-week-ship-list {:class "lead-time-list-types three-week-ship active"}
     (check-box "three-week-ship")
     [:label.active {:for "three-week-ship"} "Three week ship"]]
    [:li#standard-ship-list {:class "lead-time-list-types standard-ship active"}
     (check-box "standard-ship")
     [:label {:for "standard-ship" :class "active"} "Standard ship"]]]])

(defn product-type-filters []
  [:div.select-wrap
   [:h3 "Product Type"]])

(defn filters-view []
  [:div {:class "left-filter-col researchPage"}
   (lead-time-filters)
   (product-type-filters)
   ])

(defn filtered-product-type-section [filtered-product-type]
  (let [product-type (:product-type filtered-product-type)]
    #_(println "rendering filtered-product-type-section for product type: " product-type)
    ^{:key product-type}
    [:div.product-list
     [:h3.titleGreyborder product-type]
     [:ul.product-list
      (map essential-product-summary (:product-list filtered-product-type))]]))

(defn filtered-products-view [filtered-products-list]
  [:div.right-product-col
   [:div.filter-btn-wrap
    [:span.filter_btn_left "FILTERS"]]
   (map filtered-product-type-section filtered-products-list)
   ])

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])
        essentials-products (re-frame/subscribe [::subs/essentials-products])]
    [:div
     [:h1 "Knoll Essentials Lead Times & Finishes"]
     [:p "(built using the " @name " app framework.)"]
     [:hr]
     [:section.wrapper
      [:section#page
       [:div {:class "product-col clearfix"}
        (filters-view)
        (filtered-products-view @essentials-products)]]]]))
