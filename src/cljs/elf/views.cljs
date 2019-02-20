(ns elf.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :refer [subscribe] :as re-frame]
   [elf.events :as events]
   [elf.subs :as subs]))

(defn essential-product-summary [{:keys [product-id product-name lead-times thumb-img-src]}]
  (let [lead-times-set (set lead-times)]
    ^{:key product-id}
    [:li
     [:a.popup-modal {:href "#essentials-modal"
                      :on-click #(re-frame/dispatch [::events/product-selected product-id])}
      [:div.product-col-image
       [:img {:src thumb-img-src :data-no-retina ""}]]
      [:ul.lead-time-status
       (if (lead-times-set "quick")
         [:li.quick-lead-active])
       (if (lead-times-set "three-week")
         [:li.three-ship-active])
       (if (lead-times-set "std")
         [:li.standard-ship-active])]
      [:p product-name]]]))

(defn lead-time-filter-check-box [{:keys [li-id li-class id lead-time label value]} filter]
  [:li {:key id :id li-id :class (str "lead-time-list-types " li-class)}
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

(defn product-type-filter-group [filter-options]
  (let [name (:name filter-options)
        desc (:description filter-options)
        items (:items filter-options)]
    [:div {:class "product-type-check has-filter-submenu"}
     [:h4 desc]
     [:ul {:class "product-type-check-list", :style {:display "none"}}
      (for [i items]
        (let [id (str name "-" i)]
          ^{:key id}
          [:li
           [:input {:type "checkbox", :id id}]
           [:label {:for id} (if (= "All") (str i " " desc) i)]]))]]))

(defn product-type-filters []
  (let [seating-filter-options @(subscribe [::subs/seating-filter-options])
        tables-filter-options @(subscribe [::subs/tables-filter-options])
        storage-filter-options @(subscribe [::subs/storage-filter-options])
        power-data-filter-options @(subscribe [::subs/power-data-filter-options])
        work-tools-filter-options @(subscribe [::subs/work-tools-filter-options])
        screen-board-filter-options @(subscribe [::subs/screen-board-filter-options])]
    [:<>
     [:div {:class "filter-view-head"}
      [:h3 "Filter By"]
      [:p {:class "reset-filter-link", :style {:display "block"}} "Reset"]]
     [product-type-filter-group seating-filter-options]
     [product-type-filter-group tables-filter-options]
     [product-type-filter-group storage-filter-options]
     [product-type-filter-group power-data-filter-options]
     [product-type-filter-group work-tools-filter-options]
     [product-type-filter-group screen-board-filter-options]
     
     [:div {:class "hidden-lg visible-xs"}
      [:a {:class "apply_btn accordian_btn", :href "javascript:;"} " &lt; APPLY AND RETURN"]]]))

(defn filters-view []
  [:div {:class "left-filter-col researchPage"}
   [lead-time-filters]
   [product-type-filters]
   ])

(defn filtered-product-type-section [[product-type product-list]]
  ^{:key product-type}
  [:div.product-list
   [:h3.titleGreyborder product-type]
   [:ul.product-list
    (map essential-product-summary product-list)]])

(defn filtered-products-view []
  (let [setup-popup #(.. (js/$ ".popup-modal")
                         (magnificPopup #js {:type "inline"
                                             :midClick true
                                             :showCloseBtn false}))]
    (reagent/create-class
     {:reagent-render
      (fn []
        (let [filtered-products-list @(subscribe [::subs/filtered-products])]
          [:div.right-product-col
           [:div.filter-btn-wrap
            [:span.filter_btn_left "FILTERS"]]
           (map filtered-product-type-section (seq filtered-products-list))]))

      :display-name "filtered-products-view"

      :component-did-mount setup-popup

      :component-did-update setup-popup})))

(defn modal-popup []
  (let [selected-product @(subscribe [::subs/selected-product])
        lead-times-set (set (:lead-times selected-product))]
    [:div#essentials-modal.white-popup-block.mfp-hide
     [:div.essentials-modal-wrap
      [:div.popup-action-list-wrap
       [:ul.popup-action-list-view
        [:li
         [:span.pop-action-icon]
         [:ul.popup-action-list
          [:li 
           [:a {:href "javascript:;"} " Visit Full Product Page"]]
          [:li 
           [:a {:href "javascript:;"} "Share"]]
          [:li 
           [:a {:href "javascript:;"} "PRINT"]]
          [:li 
           [:a {:href "javascript:;"} "View essentials brochure"]]]]]]
      [:a.popup-modal-dismiss {:href "#"} "Dismiss"]
      [:div.essentials-modal-content
       [:div.essentials-product-img
        [:div.essentials-product-img-wrap
         [:img {:src (:hero1-img-src selected-product), :data-no-retina ""}]]
        [:div.essentials-product-img-detail
         [:h2 (:product-name selected-product)]
         [:p (:short-text selected-product)]]]
       [:div.essentials-product-tabs
        [:ul.essentials-tab-list
         (if (lead-times-set "quick")
           [:li.selected {:data-tab "tab1"}
            [:span.tab-color.quick-lead-active]
            [:a.tab-nav "Essentials Quickship options"]])
         (if (lead-times-set "three-week")
           [:li {:data-tab "tab2"}
            [:span.tab-color.three-ship-active]
            [:a.tab-nav "Essentials 3 week options "]])
         (if (lead-times-set "std")
           [:li {:data-tab "tab3"}
            [:span.tab-color.standard-ship-active]
            [:a.tab-nav "Standard Ship options"]])]
        [:select.tab-select-option
         (if (lead-times-set "quick")
           [:option {:value "tab1"} "ESSENTIALS Quickship options"])
         (if (lead-times-set "three-week")
           [:option {:value "tab2"} "Essentials 3 week options"])
         (if (lead-times-set "std")
           [:option {:value "tab3"} "Standard Ship options"])]
        [:div {:class "popup-tab-wrap mCustomScrollbar"}
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
             [:img {:src "/images/frame-1.jpg" :data-no-retina "" }]
             [:p "Dark Finish"]]
            [:li
             [:img {:src "/images/frame-2.jpg" :data-no-retina "" }]
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
               [:img {:src "/images/upholstery-1.jpg" :data-no-retina "" }]
               [:p "Textile 1"]]
              [:li {:data-tab "upholstery-tab-family-2", :class "selected has-sub-tab"}
               [:img {:src "/images/upholstery-2.jpg", :class "selected has-sub-tab xmCS_img_loaded", :data-no-retina "" }]
               [:p "Textile 2"]]
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-5.jpg" :data-no-retina "" }]
               [:p "Textile 5"]]
              [:li 
               [:img {:src "/images/upholstery-6.jpg" :data-no-retina "" }]
               [:p "Textile 6"]]
              [:li 
               [:img {:src "/images/upholstery-7.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-2"}
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-3"}
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-7.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]]
           [:div {:class "sub-tab-wrap"}
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-family-1"}
             [:ul {:class "upholstery-types-sub-list"}
              [:li {:data-tab "upholstery-tab-1"}
               [:a {:href "javascript:;"} "Back to all grade a"]]]
             [:h5 "Alignment K394"]
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-11.jpg" :data-no-retina "" }]
               [:p "1 Sand"]]
              [:li 
               [:img {:src "/images/upholstery-12.jpg" :data-no-retina "" }]
               [:p "2 Straw"]]
              [:li 
               [:img {:src "/images/upholstery-13.jpg" :data-no-retina "" }]
               [:p "3 Earth"]]
              [:li 
               [:img {:src "/images/upholstery-14.jpg" :data-no-retina "" }]
               [:p "4 Paprika"]]
              [:li 
               [:img {:src "/images/upholstery-15.jpg" :data-no-retina "" }]
               [:p "5 Aloe"]]
              [:li 
               [:img {:src "/images/upholstery-16.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-17.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]
              [:li 
               [:img {:src "/images/upholstery-18.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]
              [:li 
               [:img {:src "/images/upholstery-19.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]
              [:li 
               [:img {:src "/images/upholstery-20.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]
              [:li 
               [:img {:src "/images/upholstery-21.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]
              [:li 
               [:img {:src "/images/upholstery-22.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-family-2"}
             [:ul {:class "upholstery-types-sub-list"}
              [:li {:data-tab "upholstery-tab-1"}
               [:a {:href "javascript:;"} "Back to all grade a"]]]
             [:h5 "Alignment K395"]
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-12.jpg" :data-no-retina "" }]
               [:p "2 Straw"]]
              [:li 
               [:img {:src "/images/upholstery-13.jpg" :data-no-retina "" }]
               [:p "3 Earth"]]
              [:li 
               [:img {:src "/images/upholstery-14.jpg" :data-no-retina "" }]
               [:p "4 Paprika"]]
              [:li 
               [:img {:src "/images/upholstery-15.jpg" :data-no-retina "" }]
               [:p "5 Aloe"]]
              [:li 
               [:img {:src "/images/upholstery-16.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-17.jpg" :data-no-retina "" }]
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
               [:img {:src "/images/upholstery-1.jpg" :data-no-retina "" }]
               [:p "Textile 1"]]
              [:li 
               [:img {:src "/images/upholstery-2.jpg" :data-no-retina "" }]
               [:p "Textile 2"]]
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-5.jpg" :data-no-retina "" }]
               [:p "Textile 5"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-a-2"}
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-a-3"}
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-7.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
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
               [:img {:src "/images/upholstery-1.jpg" :data-no-retina "" }]
               [:p "Textile 1"]]
              [:li 
               [:img {:src "/images/upholstery-2.jpg" :data-no-retina "" }]
               [:p "Textile 2"]]
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-5.jpg" :data-no-retina "" }]
               [:p "Textile 5"]]
              [:li 
               [:img {:src "/images/upholstery-6.jpg" :data-no-retina "" }]
               [:p "Textile 6"]]
              [:li 
               [:img {:src "/images/upholstery-7.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-b-2"}
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]
            [:div {:class "upholstery-tab-content", :id "upholstery-tab-b-3"}
             [:ul {:class "upholstery-tetile-list"}
              [:li 
               [:img {:src "/images/upholstery-3.jpg" :data-no-retina "" }]
               [:p "Textile 3"]]
              [:li 
               [:img {:src "/images/upholstery-4.jpg" :data-no-retina "" }]
               [:p "Textile 4"]]
              [:li 
               [:img {:src "/images/upholstery-7.jpg" :data-no-retina "" }]
               [:p "Textile 7"]]
              [:li 
               [:img {:src "/images/upholstery-8.jpg" :data-no-retina "" }]
               [:p "Textile 8"]]]]]]]]]
       [:a {:class "swap-pro-arrow swap-pro-arrow-left"}]
       [:a {:class "swap-pro-arrow swap-pro-arrow-right"}]]]]))


(defn mouse-pos-comp []
  (reagent/with-let [pointer (reagent/atom {:x nil :y nil})
                     handler #(swap! pointer assoc
                                     :x (.-pageX %)
                                     :y (.-pageY %))
                     _ (.addEventListener js/document "mousemove" handler)]
    [:div
     "Pointer moved to: " [:br]
     (str @pointer)]
    (finally
      (.removeEventListener js/document "mousemove" handler))))

(defn main-panel []
  (let [name @(subscribe [::subs/name])]
    [:<> ; this allows sibling elements without needing to wrap in a separate [:div]
     [:h1 name]
     [:p "(built using the re-frame app framework.)"]
     [mouse-pos-comp]
     [:hr]
     [:section.wrapper
      [:section#page
       [:div {:class "product-col clearfix"}
        [filters-view]
        [filtered-products-view]]]]
     [modal-popup]]))
