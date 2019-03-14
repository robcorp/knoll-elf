(ns elf.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [elf.events :as events]
   [elf.subs :as subs]
   [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first] :as spctr]))

(def <sub (comp deref re-frame/subscribe)) ; permits using (<sub [::subs/name]) rather than @(subscribe [::subs/name])

(declare filters-view filtered-products-view modal-popup mouse-pos-comp)

(defn main-panel []
  (let [name (<sub [::subs/name])]
    [:<> ; this allows sibling elements without needing to wrap in a separate [:div]
     #_[:section.body_container]
     #_[:div
          [:h1 name]
          [:p "(built using the re-frame app framework.)"]
          [mouse-pos-comp]
          [:hr]]
     #_[:div.allbody-wrapper]
     [:section.wrapper
      [:section#page
       [:div {:class "product-col clearfix"}
        [filters-view]
        [filtered-products-view]]]]
     [:div.veil]
     [modal-popup]]))


(defn essential-product-summary [{:keys [epp-id title product-name lead-times thumb-img-src]}]
  (let [lead-times-set (set lead-times)]
    ^{:key epp-id}
    [:li
     [:a.popup-modal {:href "#essentials-modal"
                      :on-click #(re-frame/dispatch [::events/product-selected epp-id])}
      [:div.product-col-image
       [:img {:src (str "https://knlprdwcsmgt.knoll.com" thumb-img-src) :data-no-retina ""}]]
      [:ul.lead-time-status
       (if (lead-times-set "quick")
         [:li.quick-lead-active])
       (if (lead-times-set "three-week")
         [:li.three-ship-active])
       (if (lead-times-set "std")
         [:li.standard-ship-active])]
      [:p title]]]))

(defn lead-time-filter-radio-button [{:keys [li-id li-class id lead-time label value]} filter]
  [:li {:key id :id li-id :class (str "lead-time-list-types " li-class)}
   [:input {:type "radio"
            :id id
            :class "check-in"
            :checked value
            :name "lead-times-radio"
            :on-change #(re-frame/dispatch [::events/lead-time-filter-radio-button-clicked lead-time])}]
   [:label.active {:for id} label]])

;;; render the Lead Time: filters 
(defn lead-time-filters []
  (let [filters (<sub [::subs/lead-time-filters])]
    [:<>
     [:h3 "Lead Time:"]
     [:ul.lead-time-list
      (map lead-time-filter-radio-button filters)]]))

(defn product-type-filter-group [filter-options filtered-prods]
  (let [did-mount-toggler (fn [comp]
                            (.click (.find (js/$ (reagent/dom-node comp)) "h4") 
                                    (fn [ev]
                                      (this-as this
                                        (let [$this (js/$ this)]
                                          (.toggleClass $this "open")
                                          (.slideToggle (.next $this ".product-type-check-list")))))))]
    
    (reagent/create-class
     {:display-name "product-type-filter-group"

      :reagent-render (fn [filter-options filtered-prods]
                        (let [{:keys [name description product-category items]} filter-options
                              available-categories (conj (set (select [ALL #(not (empty? (product-category %))) product-category ALL] filtered-prods)) "All")
                              disable-group? false]
                          [:div {:class "product-type-check has-filter-submenu"}
                           [:h4 (if disable-group? {:class "disable-filter"}) description]
                           [:ul {:class "product-type-check-list", :style {:display "none"}}
                            (for [i items]
                              (let [{:keys [label value]} i
                                    id (str name ":" label)]
                                ^{:key id}
                                [:li
                                 [:input {:type "checkbox"
                                          :id id
                                          :checked value
                                          :class (if (available-categories label) "" "disable-filter")
                                          :on-change #(re-frame/dispatch [::events/product-type-filter-checkbox-clicked id])}]
                                 [:label {:for id} (if (= "All" label)
                                                     (str label " " description)
                                                     label)]]))]]))

      :component-did-mount did-mount-toggler})))

(defn- get-filter-values [filter]
  (select [:items ALL :value] filter))

(defn- close-filter-slideout []
  (do (.. (js/$ ".select-wrap") (removeClass "open"))
      (.. (js/$ "html") (removeClass "hidescroll"))
      (.. (js/$ ".veil") (removeClass "overlay"))))

(defn- open-filter-slideout []
  (do (.. (js/$ ".select-wrap") (toggleClass "open"))
      (.. (js/$ "html") (addClass "hidescroll"))
      (.. (js/$ ".veil") (addClass "overlay"))
      (.. (js/$ ".veil.overlay") (click close-filter-slideout))))

(defn product-type-filters []
  (let [seating-filter-options (<sub [::subs/seating-filter-options])
        tables-filter-options (<sub [::subs/tables-filter-options])
        storage-filter-options (<sub [::subs/storage-filter-options])
        power-data-filter-options (<sub [::subs/power-data-filter-options])
        work-tools-filter-options (<sub [::subs/work-tools-filter-options])
        screen-board-filter-options (<sub [::subs/screen-board-filter-options])
        filtered-prods (<sub [::subs/filtered-products])
        
        show-reset? (some true? (concat (get-filter-values seating-filter-options)
                                        (get-filter-values tables-filter-options)
                                        (get-filter-values storage-filter-options)
                                        (get-filter-values power-data-filter-options)
                                        (get-filter-values work-tools-filter-options)
                                        (get-filter-values screen-board-filter-options)))]
    [:<>
     [:div {:class "filter-view-head"}
      [:h3 "Filter By"]
      [:p {:class "reset-filter-link"
           :style {:display (if show-reset? "block" "none")}
           :on-click #(re-frame/dispatch [::events/reset-product-type-filters])}
       "Reset"]]
     [product-type-filter-group seating-filter-options filtered-prods]
     [product-type-filter-group tables-filter-options filtered-prods]
     [product-type-filter-group storage-filter-options filtered-prods]
     [product-type-filter-group power-data-filter-options filtered-prods]
     [product-type-filter-group work-tools-filter-options filtered-prods]
     [product-type-filter-group screen-board-filter-options filtered-prods]

     [:div {:class "mobile-visible"}
      [:a {:class "apply_btn accordian_btn", :on-click close-filter-slideout} " < APPLY AND RETURN"]]]))

(defn filters-view []
  [:div.left-filter-col.researchPage
   [:div.select-wrap
    [lead-time-filters]
    [product-type-filters]]])

(defn filtered-product-type-section [{:keys [label products]}]
  (when (not (empty? products))
    ^{:key label}
    [:div.product-list
     [:h3.titleGreyborder (str label " (" (count products) ")")]
     [:ul.product-list
      (map essential-product-summary products)]]))


(defn- setup-popup []
  (.. (js/$ ".popup-modal")
      (magnificPopup #js {:type "inline"
                          :midClick true
                          :showCloseBtn false})))

(defn filtered-products-view []
  (reagent/create-class
   {:display-name "filtered-products-view"

    :reagent-render (fn []
                      (let [all-products (<sub [::subs/all-products])
                            filtered-seating-products (<sub [::subs/filtered-seating-products])
                            filtered-table-products (<sub [::subs/filtered-table-products])
                            filtered-storage-products (<sub [::subs/filtered-storage-products])
                            filtered-power-products (<sub [::subs/filtered-power-products])
                            filtered-work-products (<sub [::subs/filtered-work-products])
                            filtered-screen-products (<sub [::subs/filtered-screen-products])
                            loading-all-products? (<sub [::subs/loading-all-products])
                            no-results? (empty? (select [ALL :products ALL] (concat filtered-seating-products
                                                                                    filtered-table-products
                                                                                    filtered-storage-products
                                                                                    filtered-power-products
                                                                                    filtered-work-products
                                                                                    filtered-screen-products)))]
                        #_(println "count(all-products): " (count all-products))
                        [:div.right-product-col
                         [:div.right-product-content
                          [:div.filter-btn-wrap
                           [:span.filter_btn_left {:on-click open-filter-slideout}
                            "FILTERS"]]
                          (if (empty? all-products) ;loading-all-products?
                              [:div
                               [:h3.text-center "Loading..."]]
                              (if no-results?
                                [:div [:h3.text-center "No results found"]]
                                [:<>
                                 (map filtered-product-type-section filtered-seating-products)
                                 (map filtered-product-type-section filtered-table-products)
                                 (map filtered-product-type-section filtered-storage-products)
                                 (map filtered-product-type-section filtered-power-products)
                                 (map filtered-product-type-section filtered-work-products)
                                 (map filtered-product-type-section filtered-screen-products)]))]]))

    :component-did-mount setup-popup

    :component-did-update setup-popup}))

#_(defn old-modal-popup []
  (let [selected-product (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-product))]
    [:div#essentials-modal.white-popup-block.mfp-hide
     [:div.essentials-modal-wrap
      [:div.header-popup-view
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
            [:a {:href "javascript:;"} "View essentials brochure"]]]]]
        [:a.popup-modal-dismiss "Dismiss"]]]
      [:div.essentials-modal-content
       [:div.essentials-product-img
        [:div.essentials-product-img-wrap
         [:img {:src (str "https://knlprdwcsmgt.knoll.com" (:hero1-img-src selected-product)), :data-no-retina ""}]]
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

(defn modal-popup []
  (let [selected-product (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-product))]
    [:div#essentials-modal.white-popup-block.mfp-hide
     [:div.essentials-modal-wrap
      [:div.header-popup-view
       [:div.popup-action-list-wrap
        [:ul.popup-action-list-view
         [:li [:span.pop-action-icon]
          [:ul.popup-action-list
           [:li [:a {:href (str "https://www.knoll.com/product/" (:product-id selected-product) "?section=design") :target "_blank"} " Visit Full Product Page"]]
           [:li [:a {:href "javascript:;"} "Share"]] [:li [:a {:href "javascript:;"} "PRINT"]]
           [:li [:a {:href "javascript:;"} "View essentials brochure"]]]]]]
       [:a.popup-modal-dismiss {:on-click #(->> js/$ .-magnificPopup .close)} "Dismiss"]]
      [:div.owl-popup-div
       [:div.item
        [:div.essentials-modal-content
         [:div.essentials-product-img
          [:div.essentials-product-img-wrap
           [:img {:src (str "https://knlprdwcsmgt.knoll.com" (:hero1-img-src selected-product)) :data-no-retina ""}]]
          [:div.essentials-product-img-detail
           [:h2 (:product-name selected-product)]
           [:div {:dangerouslySetInnerHTML {:__html (:short-text selected-product)}}]]]
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
          #_[:select.tab-select-option
             [:option {:value "tab1"} "ESSENTIALS Quickship options"]
             [:option {:value "tab2"} "Essentials 3 week options"]
             [:option {:value "tab3"} "Standard Ship options"]]
          [:div.popup-tab-wrap.mCustomScrollbar
           [:div#tab1.popup-tab-content.selected
            [:div.options-list-wrap
             [:h4 "Options"]
             [:ul.options-list
              [:li " Dark frame finish"]
              [:li " Plastic or aluminum base"]]
             [:ul.options-list
              [:li " Three arms"]
              [:li " Lumbar support"]]]
            [:div.frame-finish-wrap
             [:h4 "Frame Finish"]
             [:ul.frame-list
              [:li [:div.swatch-div [:img {:src "/images/frames/k-frame-finish.jpg" :data-no-retina ""}]] [:p "Red"]]
              [:li [:div.swatch-div [:img {:src "/images/frames/k-frame-finish-2.jpg" :data-no-retina ""}]] [:p "Light Finish"]]
              [:li [:div.swatch-div [:img {:src "/images/frames/k-frame-finish-3.jpg" :data-no-retina ""}]] [:p "Light Finish"]]
              [:li [:div.swatch-div [:img {:src "/images/frames/k-frame-finish-4.jpg" :data-no-retina ""}]] [:p "Black"]]
              [:li [:div.swatch-div [:img {:src "/images/frames/k-frame-finish-5.jpg" :data-no-retina ""}]] [:p "Grey"]]]]
            [:div.upholstery-list-wrap
             [:h4 "Upholstery"]
             [:div.tab-main
              [:label "Grade:"]
              [:ul.upholstery-types-list
               [:li.selected {:data-tab "upholstery-tab-1"} [:a {:href "javascript:;"} "a"]]
               [:li {:data-tab "upholstery-tab-2"} [:a {:href "javascript:;"} "b"]]
               [:li {:data-tab "upholstery-tab-3"} [:a {:href "javascript:;"} "c"]]
               [:li [:a {:href "javascript:;"} "LEATHER & VINYL"]]]]
             [:div.upholstery-tab-wrap
              [:div#upholstery-tab-1.upholstery-tab-content.selected
               [:ul.upholstery-tetile-list
                [:li.selected.has-sub-tab {:data-tab "upholstery-tab-family-1"}
                 [:div.swatch-div
                  [:img {:src "images/upholstery-1.jpg" :data-no-retina ""}]] [:p "Textile 1"]]
                [:li.selected.has-sub-tab {:data-tab "upholstery-tab-family-2"}
                 [:div.swatch-div [:img.selected.has-sub-tab {:src "/images/upholstery-2.jpg" :data-no-retina ""}]] [:p "Textile 2"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-5.jpg" :data-no-retina ""}]] [:p "Textile 5"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-6.jpg" :data-no-retina ""}]] [:p "Textile 6"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-7.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
              [:div#upholstery-tab-2.upholstery-tab-content
               [:ul.upholstery-tetile-list
                [:li
                 [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li
                 [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
              [:div#upholstery-tab-3.upholstery-tab-content
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-7.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]
             [:div.sub-tab-wrap
              [:div#upholstery-tab-family-1.upholstery-tab-content
               [:ul.upholstery-types-sub-list
                [:li {:data-tab "upholstery-tab-1"} [:a {:href "javascript:;"} "Back to all grade a"]]]
               [:h5 "Alignment K394"]
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-11.jpg" :data-no-retina ""}]] [:p "1 Sand"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-12.jpg" :data-no-retina ""}]] [:p "2 Straw"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-13.jpg" :data-no-retina ""}]] [:p "3 Earth"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-14.jpg" :data-no-retina ""}]] [:p "4 Paprika"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-15.jpg" :data-no-retina ""}]] [:p "5 Aloe"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-16.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-17.jpg" :data-no-retina ""}]] [:p "Textile 8"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-18.jpg" :data-no-retina ""}]] [:p "Textile 8"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-19.jpg" :data-no-retina ""}]] [:p "Textile 8"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-20.jpg" :data-no-retina ""}]] [:p "Textile 8"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-21.jpg" :data-no-retina ""}]] [:p "Textile 8"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-22.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
              [:div#upholstery-tab-family-2.upholstery-tab-content
               [:ul.upholstery-types-sub-list
                [:li {:data-tab "upholstery-tab-1"} [:a {:href "javascript:;"} "Back to all grade a"]]]
               [:h5 "Alignment K395"]
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-12.jpg" :data-no-retina ""}]] [:p "2 Straw"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-13.jpg" :data-no-retina ""}]] [:p "3 Earth"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-14.jpg" :data-no-retina ""}]] [:p "4 Paprika"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-15.jpg" :data-no-retina ""}]] [:p "5 Aloe"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-16.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-17.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]]]
           [:div#tab2.popup-tab-content
            [:div.options-list-wrap
             [:h4 "Options"]
             [:ul.options-list
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Lumbar support"]]
             [:ul.options-list
              [:li " Three arm options, or armless"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]]
            [:div.upholstery-list-wrap [:h4 "Upholstery"]
             [:label "Grade:"]
             [:ul.upholstery-types-list
              [:li.selected {:data-tab "upholstery-tab-a-1"} [:a {:href "javascript:;"} "a"]]
              [:li {:data-tab "upholstery-tab-a-2"} [:a {:href "javascript:;"} "b"]]
              [:li {:data-tab "upholstery-tab-a-3"} [:a {:href "javascript:;"} "c"]]]
             [:div.upholstery-tab-wrap
              [:div#upholstery-tab-a-1.upholstery-tab-content.selected
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-1.jpg" :data-no-retina ""}]] [:p "Textile 1"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-2.jpg" :data-no-retina ""}]] [:p "Textile 2"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-5.jpg" :data-no-retina ""}]] [:p "Textile 5"]]]]
              [:div#upholstery-tab-a-2.upholstery-tab-content
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
              [:div#upholstery-tab-a-3.upholstery-tab-content
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-7.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]]]
           [:div#tab3.popup-tab-content
            [:div.options-list-wrap
             [:h4 "Options"]
             [:ul.options-list
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]
             [:ul.options-list
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]
              [:li " Three arm options, or armless"]
              [:li " Lumbar support"]]]
            [:div.upholstery-list-wrap
             [:h4 "Upholstery"] [:label "Grade:"]
             [:ul.upholstery-types-list
              [:li.selected {:data-tab "upholstery-tab-b-1"} [:a {:href "javascript:;"} "a"]]
              [:li {:data-tab "upholstery-tab-b-2"} [:a {:href "javascript:;"} "b"]]
              [:li {:data-tab "upholstery-tab-b-3"} [:a {:href "javascript:;"} "c"]]]
             [:div.upholstery-tab-wrap
              [:div#upholstery-tab-b-1.upholstery-tab-content.selected
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-1.jpg" :data-no-retina ""}]] [:p "Textile 1"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-2.jpg" :data-no-retina ""}]] [:p "Textile 2"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-5.jpg" :data-no-retina ""}]] [:p "Textile 5"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-6.jpg" :data-no-retina ""}]] [:p "Textile 6"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-7.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
              [:div#upholstery-tab-b-2.upholstery-tab-content
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
              [:div#upholstery-tab-b-3.upholstery-tab-content
               [:ul.upholstery-tetile-list
                [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-7.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
                [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]]]]]]]]]]))


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
