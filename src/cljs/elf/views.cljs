(ns elf.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :refer [subscribe] :as re-frame]
   [elf.events :as events]
   [elf.subs :as subs]))

(defn essential-product-summary [{:keys [product-id product-name lead-times img-src]} product]
  (let [lead-times-set (set lead-times)]
    ^{:key product-id}
    [:li
     [:a.popup-modal {:href "#" #_"#essentials-modal"
                      :on-click #(re-frame/dispatch [::events/product-selected product-id])}
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

(defn filtered-products-view []
  (let [filtered-products-list @(subscribe [::subs/filtered-products])]
    [:div.right-product-col
     [:div.filter-btn-wrap
      [:span.filter_btn_left "FILTERS"]]
     (map filtered-product-type-section (seq filtered-products-list))
     ]))

(defn modal-popup []
  (let [selected-product-id @(subscribe [::subs/selected-product])]
    [:div {:id "essentials-modal", :class "white-popup-block mfp-hide"}
     [:div {:class "essentials-modal-wrap"}
      [:div {:class "popup-action-list-wrap"}
       [:ul {:class "popup-action-list-view"}
        [:li  
         [:span {:class "pop-action-icon"}]
         [:ul {:class "popup-action-list"}
          [:li 
           [:a {:href "javascript:;"} " Visit Full Product Page"]]
          [:li 
           [:a {:href "javascript:;"} "Share"]]
          [:li 
           [:a {:href "javascript:;"} "PRINT"]]
          [:li 
           [:a {:href "javascript:;"} "View essentials brochure"]]]]]]
      [:a {:class "popup-modal-dismiss", :href "#"} "Dismiss"]
      [:div {:class "essentials-modal-content"}
       [:div {:class "essentials-product-img"}
        [:div {:class "essentials-product-img-wrap"}
         [:img {:src "/images/product-popup-1.jpg", :data-no-retina ""}]]
        [:div {:class "essentials-product-img-detail"}
         [:h2 "MultiGeneration by Knoll High Task"]
         [:p "The Diamond Chair is an astounding study in space, form and function by one of the master sculptors of the last century. Like Saarinen and Mies, Bertoia found sublime grace in an industrial material, elevating it beyond its normal utility into a work of art. Harry Bertoia’s wire chairs are among the most recognized achievements of mid-century modern design and a proud part of the Knoll heritage."]]]
       [:div {:class "essentials-product-tabs"}
        [:ul {:class "essentials-tab-list"}
         [:li {:data-tab "tab1"}
          [:span {:class "tab-color quick-lead-active"}]
          [:a {:class "tab-nav"} "Essentials Quickship  options"]]
         [:li {:data-tab "tab2"}
          [:span {:class "tab-color three-ship-active"}]
          [:a {:class "tab-nav"} "Essentials 3 week options "]]
         [:li {:data-tab "tab3"}
          [:span {:class "tab-color standard-ship-active"}]
          [:a {:class "tab-nav"} "Standard Ship options"]]]
        [:select {:class "tab-select-option"}
         [:option {:value "tab1"} "ESSENTIALS Quickship options"]
         [:option {:value "tab2"} "Essentials 3 week options"]
         [:option {:value "tab3"} "Standard Ship options"]]
        [:div {:class "popup-tab-wrap mCustomScrollbar _mCS_1"}
         [:div {:id "mCSB_1", :class "xmCustomScrollBox xmCS-light xmCSB_vertical xmCSB_inside", :style {:max-height "645px"}, :tabIndex "0"}
          [:div {:id "mCSB_1_container", :class "mCSB_container", :style {:position "relative" :left "0px" :top "0px"}, :dir "ltr"}
           [:div {:class "popup-tab-content selected", :id "tab1"}
            [:div {:class "options-list-wrap"}
             [:h4 "Options"]
             [:ul {:class "options-list"}
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]
             [:ul {:class "options-list"}
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]]
            [:div {:class "frame-finish-wrap"}
             [:h4 "Frame Finish"]
             [:ul {:class "frame-list"}
              [:li  
               [:img {:src "/images/frame-1.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
               [:p "Dark Finish"]]
              [:li  
               [:img {:src "/images/frame-2.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
               [:p "Light Finish"]]]]
            [:div {:class "upholstery-list-wrap"}
             [:h4 "Upholstery"]
             [:div {:class "tab-main"}
              [:label "Grade:"]
              [:ul {:class "upholstery-types-list"}
               [:li {:data-tab "upholstery-tab-1", :class "selected"}
                [:a {:href "javascript:;"} "a"]]
               [:li {:data-tab "upholstery-tab-2"}
                [:a {:href "javascript:;"} "b"]]
               [:li {:data-tab "upholstery-tab-3"}
                [:a {:href "javascript:;"} "c"]]
               [:li 
                [:a {:href "javascript:;"} "LEATHER & VINYL"]]]]
             [:div {:class "upholstery-tab-wrap"}
              [:div {:class "upholstery-tab-content selected", :id "upholstery-tab-1"}
               [:ul {:class "upholstery-tetile-list"}
                [:li {:data-tab "upholstery-tab-family-1", :class "selected has-sub-tab"}
                 [:img {:src "/images/upholstery-1.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 1"]]
                [:li {:data-tab "upholstery-tab-family-2", :class "selected has-sub-tab"}
                 [:img {:src "/images/upholstery-2.jpg", :class "selected has-sub-tab mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 2"]]
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-5.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 5"]]
                [:li 
                 [:img {:src "/images/upholstery-6.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 6"]]
                [:li 
                 [:img {:src "/images/upholstery-7.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-2"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-3"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-7.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]]
             [:div {:class "sub-tab-wrap"}
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-family-1"}
               [:ul {:class "upholstery-types-sub-list"}
                [:li {:data-tab "upholstery-tab-1"}
                 [:a {:href "javascript:;"} "Back to all grade a"]]]
               [:h5 "Alignment K394"]
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-11.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "1 Sand"]]
                [:li 
                 [:img {:src "/images/upholstery-12.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "2 Straw"]]
                [:li 
                 [:img {:src "/images/upholstery-13.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "3 Earth"]]
                [:li 
                 [:img {:src "/images/upholstery-14.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "4 Paprika"]]
                [:li 
                 [:img {:src "/images/upholstery-15.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "5 Aloe"]]
                [:li 
                 [:img {:src "/images/upholstery-16.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-17.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]
                [:li 
                 [:img {:src "/images/upholstery-18.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]
                [:li 
                 [:img {:src "/images/upholstery-19.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]
                [:li 
                 [:img {:src "/images/upholstery-20.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]
                [:li 
                 [:img {:src "/images/upholstery-21.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]
                [:li 
                 [:img {:src "/images/upholstery-22.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-family-2"}
               [:ul {:class "upholstery-types-sub-list"}
                [:li {:data-tab "upholstery-tab-1"}
                 [:a {:href "javascript:;"} "Back to all grade a"]]]
               [:h5 "Alignment K395"]
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-12.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "2 Straw"]]
                [:li 
                 [:img {:src "/images/upholstery-13.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "3 Earth"]]
                [:li 
                 [:img {:src "/images/upholstery-14.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "4 Paprika"]]
                [:li 
                 [:img {:src "/images/upholstery-15.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "5 Aloe"]]
                [:li 
                 [:img {:src "/images/upholstery-16.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-17.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]]]]
           [:div {:class "popup-tab-content", :id "tab2"}
            [:div {:class "options-list-wrap"}
             [:h4 "Options"]
             [:ul {:class "options-list"}
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Lumbar support"]]
             [:ul {:class "options-list"}
              [:li " Three arm options, or armless"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]]
            [:div {:class "upholstery-list-wrap"}
             [:h4 "Upholstery"]
             [:label "Grade:"]
             [:ul {:class "upholstery-types-list"}
              [:li {:data-tab "upholstery-tab-a-1", :class "selected"}
               [:a {:href "javascript:;"} "a"]]
              [:li {:data-tab "upholstery-tab-a-2"}
               [:a {:href "javascript:;"} "b"]]
              [:li {:data-tab "upholstery-tab-a-3"}
               [:a {:href "javascript:;"} "c"]]]
             [:div {:class "upholstery-tab-wrap"}
              [:div {:class "upholstery-tab-content selected", :id "upholstery-tab-a-1"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-1.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 1"]]
                [:li 
                 [:img {:src "/images/upholstery-2.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 2"]]
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-5.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 5"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-a-2"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-a-3"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-7.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]]]]
           [:div {:class "popup-tab-content", :id "tab3"}
            [:div {:class "options-list-wrap"}
             [:h4 "Options"]
             [:ul {:class "options-list"}
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]
             [:ul {:class "options-list"}
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]]
            [:div {:class "upholstery-list-wrap"}
             [:h4 "Upholstery"]
             [:label "Grade:"]
             [:ul {:class "upholstery-types-list"}
              [:li {:data-tab "upholstery-tab-b-1", :class "selected"}
               [:a {:href "javascript:;"} "a"]]
              [:li {:data-tab "upholstery-tab-b-2"}
               [:a {:href "javascript:;"} "b"]]
              [:li {:data-tab "upholstery-tab-b-3"}
               [:a {:href "javascript:;"} "c"]]]
             [:div {:class "upholstery-tab-wrap"}
              [:div {:class "upholstery-tab-content selected", :id "upholstery-tab-b-1"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-1.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 1"]]
                [:li 
                 [:img {:src "/images/upholstery-2.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 2"]]
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-5.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 5"]]
                [:li 
                 [:img {:src "/images/upholstery-6.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 6"]]
                [:li 
                 [:img {:src "/images/upholstery-7.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-b-2"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]
              [:div {:class "upholstery-tab-content", :id "upholstery-tab-b-3"}
               [:ul {:class "upholstery-tetile-list"}
                [:li 
                 [:img {:src "/images/upholstery-3.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 3"]]
                [:li 
                 [:img {:src "/images/upholstery-4.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 4"]]
                [:li 
                 [:img {:src "/images/upholstery-7.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 7"]]
                [:li 
                 [:img {:src "/images/upholstery-8.jpg", :class "mCS_img_loaded", :data-no-retina "" }]
                 [:p "Textile 8"]]]]]]]]
          [:div {:id "mCSB_1_scrollbar_vertical", :class "mCSB_scrollTools mCSB_1_scrollbar mCS-light mCSB_scrollTools_vertical", :style {:display "block"}}
           [:div {:class "mCSB_draggerContainer"}
            [:div {:id "mCSB_1_dragger_vertical", :class "mCSB_dragger", :style {:position "absolute" :min-height "100px" :top "0px" :display "block" :height "556px" :max-height "635px"}}
             [:div {:class "mCSB_dragger_bar", :style {:line-height "100px"}}]]
            [:div {:class "mCSB_draggerRail"}]]]]]]
       [:a {:class "swap-pro-arrow swap-pro-arrow-left"}]
       [:a {:class "swap-pro-arrow swap-pro-arrow-right"}]]]]))

(defn main-panel []
  (let [name @(subscribe [::subs/name])]
    (reagent/create-class
     {:display-name "main-panel"

      :component-did-mount
      (fn []
        (println "The main-panel component has mounted")
        #_(.. (js/$ ".popup-modal")
              (magnificPopup #js {:type "inline"
                                  :midClick true
                                  :showCloseBtn false})))

      :reagent-render
      (fn []
        [:div
         [:h1 "Knoll Essentials Lead Times & Finishes"]
         [:p "(built using the " name " app framework.)"]
         [:hr]
         [:section.wrapper
          [:section#page
           [:div {:class "product-col clearfix"}
            (filters-view)
            (filtered-products-view)]]]
         (modal-popup)])})))

