(ns elf.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [elf.config :as config]
   [elf.events :as events]
   [elf.subs :as subs]
   [com.rpl.specter :refer [ALL collect-one multi-path walker] :refer-macros [select select-first] :as spctr]
   [clojure.string :as str]))

(def <sub (comp deref re-frame/subscribe)) ; permits using (<sub [::subs/name]) rather than @(subscribe [::subs/name])
(def evt> re-frame/dispatch)

(declare filters-view filtered-products-view modal-popup mouse-pos-comp)

(defn main-panel []
  (let [name (<sub [::subs/name])]
    [:<> ; this allows sibling elements without needing to wrap in a separate [:div]
     (when false #_config/debug?
       [:section.body_container]
       [:div
          [:h1 name]
          [:p "(built using the re-frame app framework.)"]
          [mouse-pos-comp]
          [:hr]])
     #_[:div.allbody-wrapper]
     [:section.wrapper
      [:section#page
       [:div {:class "product-col clearfix"}
        [filters-view]
        [filtered-products-view]]]]
     [:div.veil]
     [modal-popup]]))


(defn essential-product-summary [label {:keys [epp-id title product-name lead-times thumb-img]}]
  (let [lead-times-set (set lead-times)]
    [:li
     [:a.popup-modal {:href "#essentials-modal"
                      :on-click #(evt> [::events/product-selected label epp-id])}
      [:div.product-col-image
       [:img {:src (str "https://knlprdwcsmgt.knoll.com/media" thumb-img) :data-no-retina ""}]]
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
            :on-change #(evt> [::events/lead-time-filter-radio-button-clicked lead-time])}]
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
                                          :on-change #(evt> [::events/product-type-filter-checkbox-clicked id])}]
                                 [:label {:for id} (if (= "All" label)
                                                     (str label " " description)
                                                     label)]]))]]))

      :component-did-mount did-mount-toggler})))

(defn- get-filter-values [filter]
  (select [:items ALL :value] filter))

(defn- close-filter-slideout []
  (do (.removeClass (js/$ ".select-wrap") "open")
      (.removeClass (js/$ "html") "hidescroll")
      (.removeClass (js/$ ".veil") "overlay")))

(defn- open-filter-slideout []
  (do (.toggleClass (js/$ ".select-wrap") "open")
      (.addClass (js/$ "html") "hidescroll")
      (.addClass (js/$ ".veil") "overlay")
      (.click (js/$ ".veil.overlay") close-filter-slideout)))

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
           :on-click #(evt> [::events/reset-product-type-filters])}
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
      (for [prod products]
        (let [epp-id (:epp-id prod)]
          ^{:key epp-id}
          [essential-product-summary label prod]))]]))


(defn- setup-popup []
  #_(.. (js/$ ".popup-modal")
      (magnificPopup #js {:type "inline"
                          :midClick true
                          :showCloseBtn false})))

(defn filtered-products-view []
  (let [all-products (<sub [::subs/all-products])
                            filtered-seating-prods (<sub [::subs/filtered-seating-products])
                            filtered-table-prods (<sub [::subs/filtered-table-products])
                            filtered-storage-prods (<sub [::subs/filtered-storage-products])
                            filtered-power-prods (<sub [::subs/filtered-power-products])
                            filtered-work-prods (<sub [::subs/filtered-work-products])
                            filtered-screen-prods (<sub [::subs/filtered-screen-products])
                            no-results? (empty? (select [ALL :products ALL] (concat filtered-seating-prods
                                                                                    filtered-table-prods
                                                                                    filtered-storage-prods
                                                                                    filtered-power-prods
                                                                                    filtered-work-prods
                                                                                    filtered-screen-prods)))]

                        [:div.right-product-col
                         [:div.right-product-content
                          [:div.filter-btn-wrap
                           [:span.filter_btn_left {:on-click open-filter-slideout}
                            "FILTERS"]]
                          (if (empty? all-products)
                              [:div
                               [:h3.text-center "Loading..."]]
                              (if no-results?
                                [:div [:h3.text-center "No results found"]]
                                [:<>
                                 (map filtered-product-type-section filtered-seating-prods)
                                 (map filtered-product-type-section filtered-table-prods)
                                 (map filtered-product-type-section filtered-storage-prods)
                                 (map filtered-product-type-section filtered-power-prods)
                                 (map filtered-product-type-section filtered-work-prods)
                                 (map filtered-product-type-section filtered-screen-prods)]))]]))


(defn tab-contents [lead-time]
  (let [selected-product (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-product))
        avail-fin-mods (select [:availFinMods ALL #(not= "Options" (:title %)) (collect-one :title) (keyword lead-time) :fins] selected-product)
        [optsTitle opts] (select-first [:availFinMods ALL #(= "options" (str/lower-case (:title %))) (collect-one :title) (keyword lead-time)] selected-product)]
    #_(println "avail-fin-mods: " avail-fin-mods)
    [:<>
     [:div {:id lead-time :class "popup-tab-content selected"}
      (if opts
        [:div.options-list-wrap
         [:h4 optsTitle]
         [:div {:dangerouslySetInnerHTML {:__html (:optsTxt opts)}}]])

      (for [[title fins] avail-fin-mods]
        (if (> (count fins) 0)
          ^{:key title}
          [:div.frame-finish-wrap
           [:h4 title]
           [:ul.frame-list
            (for [fin fins]
              ^{:key (:id fin)}
              [:li
               [:div.swatch-div
                [:img {:src (str "https://knlprdwcsmgt.knoll.com/media" (:img fin)) :data-no-retina ""}]]
               [:p (:color fin)]])]]))

      #_[:div.upholstery-list-wrap
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
            [:li [:div.swatch-div [:img {:src "/images/upholstery-17.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]]]]))

(defn- lead-time-tab-clicked [evt]
  (let [target (js/$ (.-currentTarget evt))
        tab (.data target "tab")]
    (.removeClass (js/$ ".essentials-tab-list li") "selected") ; deselect the current tab
    (.removeClass (js/$ ".popup-tab-wrap .popup-tab-content") "selected") ; and hide the current tab's contents
    (.addClass target "selected") ; select the new tab
    (.addClass (js/$ (str ".popup-tab-content#" tab)) "selected"))) ; and show the new tab's contents

(defn product-tabs []
  (let [selected-product (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-product))]

    [:div.essentials-product-tabs

     [:ul.essentials-tab-list
      (if (lead-times-set "quick")
        [:li.selected {:data-tab "quick" :on-click lead-time-tab-clicked}
         [:span.tab-color.quick-lead-active]
         [:a.tab-nav "Essentials Quickship options"]])

      (if (lead-times-set "three-week")
        [:li {:data-tab "three-week" :on-click lead-time-tab-clicked}
         [:span.tab-color.three-ship-active]
         [:a.tab-nav "Essentials 3 week options "]])

      (if (lead-times-set "std")
        [:li {:data-tab "std" :on-click lead-time-tab-clicked}
         [:span.tab-color.standard-ship-active]
         [:a.tab-nav "Standard Ship options"]])]

     [:select.tab-select-option
      (if (lead-times-set "quick")
        [:option {:value "tab1"} "ESSENTIALS Quickship options"])

      (if (lead-times-set "three-week")
        [:option {:value "tab2"} "Essentials 3 week options"])

      (if (lead-times-set "std")
        [:option {:value "tab3"} "Standard Ship options"])]

     [:div.popup-tab-wrap.mCustomScrollbar

      (if (lead-times-set "quick")
        [:<> [tab-contents "quick"]])

      (if (lead-times-set "three-week")
        [:<> [tab-contents "three-week"]])

      (if (lead-times-set "std")
        [:<> [tab-contents "std"]])

      #_[:div#tab2.popup-tab-content
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
      #_[:div#tab3.popup-tab-content
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
             [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]]]]]))

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
      [:div.owl-popup-div ;.owl-carousel.owl-theme.owl-responsive--1.owl-loaded
       [:div.item
        [:div.essentials-modal-content
         [:div.essentials-product-img
          [:div.essentials-product-img-wrap
           [:img {:src (str "https://knlprdwcsmgt.knoll.com/media" (:hero1-img selected-product)) :data-no-retina ""}]]
          [:div.essentials-product-img-detail
           [:h2 (:title selected-product)]
           [:div {:dangerouslySetInnerHTML {:__html (:short-text selected-product)}}]]]

         [product-tabs]]]]]]))


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
