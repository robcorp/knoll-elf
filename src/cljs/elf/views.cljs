(ns elf.views
  (:require
   [re-frame.core :refer [subscribe] :as re-frame]
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
       (if (lead-times-set :quick)
         [:li.quick-lead-active])
       (if (lead-times-set :three-week)
         [:li.three-ship-active])
       (if (lead-times-set :std)
         [:li.standard-ship-active])]
      [:p product-name]]]))

(defn lead-time-filter-check-box [{:keys [li-id li-class id lead-time label value]} filter]
  ^{:key id }
  [:li {:id li-id :class (str "lead-time-list-types " li-class)}
   [:input {:type "checkbox"
            :id id
            :class "check-in"
            :checked value
            :on-change #(re-frame/dispatch [::events/lead-time-filter-check-box-clicked lead-time])}]
   [:label.active {:for id} label]])

;;; render the Lead Time: filters 
(defn lead-time-filters []
  (let [filters @(subscribe [::subs/lead-time-filters])]
    [:div.select-wrap
     [:h3 "Lead Time:"]
     [:ul.lead-time-list
      (map lead-time-filter-check-box filters)]]))

(defn product-type-filters []
  [:div.select-wrap
   [:h3 "Product Type"]])

(defn filters-view []
  [:div {:class "left-filter-col researchPage"}
   (lead-time-filters)
   (product-type-filters)
   ])

(defn filtered-product-type-section [[product-type product-list]]
  ^{:key product-type}
  [:div.product-list
   [:h3.titleGreyborder product-type]
   [:ul.product-list
    (map essential-product-summary product-list)]])

(defn filtered-products-view [filtered-products-list]
  [:div.right-product-col
   [:div.filter-btn-wrap
    [:span.filter_btn_left "FILTERS"]]
   (map filtered-product-type-section (seq filtered-products-list))
   ])

(defn main-panel []
  (let [name @(subscribe [::subs/name])
        filtered-products @(subscribe [::subs/filtered-products])]
    [:div
     [:h1 "Knoll Essentials Lead Times & Finishes"]
     [:p "(built using the " name " app framework.)"]
     [:hr]
     [:section.wrapper
      [:section#page
       [:div {:class "product-col clearfix"}
        (filters-view)
        (filtered-products-view filtered-products)]]]]))
