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
     [modal-popup]
     [:div.veil]
     (when false #_config/debug?
           [:section.body_container]
           [:div
            [:h1 name]
            [:p "(built using the re-frame app framework.)"]
            [mouse-pos-comp]
            [:hr]])
     [:section.wrapper.essentials
      [:section#page
       [:div {:class "product-col clearfix"}
        [filters-view]
        [filtered-products-view]]]]]))


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
                                                                filtered-screen-prods)))
        ;;all-swatches (into #{} (select [ALL :availFinMods ALL (spctr/multi-path [:quick] [:std] [:three-weeik]) :fins ALL :img] all-products))
        ]

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
           (map filtered-product-type-section filtered-screen-prods)
           #_[:div {:class "hidden"}
            (for [img all-swatches]
              ^{:key img}
              [:img {:src (str "http://knlprdwcsmgt1.knoll.com/media" img)}])]]))]]))


(defn- finish-types-pill-clicked [evt]
  (let [target (js/$ (.-currentTarget evt))
        tab (.data target "tab")
        tab-content (str ".popup-tab-content.selected " "#" tab ".finish-tab-content")]

    (.removeClass (js/$ ".popup-tab-content.selected .finish-types-list > li") "selected") ; deselect the current pill
    (.removeClass (js/$ ".finish-tab-content") "selected") ; and hide the current pill's tab contents
    (.addClass target "selected") ; select the new tab
    (.addClass (js/$ tab-content) "selected")))

(defn- create-finish-types-pill [i [title fins]]
  (if (> (count fins) 0)
    ^{:key (str "finish-" title - "pill")}
    [:li {:class (if (= i 0) "selected" "")
          :data-tab (str "finish-" (str/replace title #"[^a-zA-Z0-9-]" ""))
          :on-click finish-types-pill-clicked}
     [:a {:href "javascript:;"} title]]))

(defn- create-finish-types-tab [i [title fins]]
  (if (> (count fins) 0)
    ^{:key (str "finiish-" title "-tab")}
    [:div {:id (str "finish-" (str/replace title #"[^a-zA-Z0-9-]" "")) :class ["finish-tab-content" (if (= i 0) "selected")]}
     [:h5.print-show title]
     [:ul.frame-list
           (for [fin fins]
             ^{:key (:id fin)}
             [:li
              [:div.swatch-div
               [:img {:src (str "https://knlprdwcsmgt.knoll.com/media" (:img fin)) :data-no-retina ""}]]
              [:p (:color fin)]])]]))

(defn tab-contents [lead-time selected-prod lead-times-set selected?]
  (let [avail-fin-mods (select [:availFinMods ALL #(not= "Options" (:title %)) (collect-one :title) (keyword lead-time) :fins] selected-prod)
        [optsTitle opts] (select-first [:availFinMods ALL #(= "options" (str/lower-case (:title %))) (collect-one :title) (keyword lead-time)] selected-prod)
        tab-content-class (if selected? "selected" "")
        print-show-text (case (keyword lead-time)
                          :quick "Essentials Quickship options"
                          :three-week "Essentials 3 week options"
                          :std "Standard Ship options")]

    ^{:key (str (:epp-id selected-prod) "-" lead-time)}
    [:div.popup-tab-content {:id lead-time :class tab-content-class}
     [:h3.print-show.print-show-h3
      [:a.tab-nav print-show-text]]
     
     (if opts
       [:div.options-list-wrap
        [:h4 optsTitle]
        [:div {:dangerouslySetInnerHTML {:__html (:optsTxt opts)}}]])

     (if (> (count avail-fin-mods) 0)
       [:div.finish-list-wrap
        [:h4 "Finishes"]
        [:div.tab-main
         [:ul.finish-types-list
          (map-indexed create-finish-types-pill avail-fin-mods)]]

        [:div.finish-tab-wrap
         (map-indexed create-finish-types-tab avail-fin-mods)
         
         #_[:div#upholstery-tab-1.upholstery-tab-content.selected
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
         #_[:div#upholstery-tab-2.upholstery-tab-content
            [:ul.upholstery-tetile-list
             [:li
              [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
             [:li
              [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
             [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]
         #_[:div#upholstery-tab-3.upholstery-tab-content
            [:ul.upholstery-tetile-list
             [:li [:div.swatch-div [:img {:src "/images/upholstery-3.jpg" :data-no-retina ""}]] [:p "Textile 3"]]
             [:li [:div.swatch-div [:img {:src "/images/upholstery-4.jpg" :data-no-retina ""}]] [:p "Textile 4"]]
             [:li [:div.swatch-div [:img {:src "/images/upholstery-7.jpg" :data-no-retina ""}]] [:p "Textile 7"]]
             [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]
        #_[:div.sub-tab-wrap
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
             [:li [:div.swatch-div [:img {:src "/images/upholstery-17.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]])]))

(defn- lead-time-tab-clicked [evt]
  (let [target (js/$ (.-currentTarget evt))
        tab (.data target "tab")
        tab-content (str "#" tab ".popup-tab-content")]

    (.removeClass (js/$ ".essentials-tab-list > li") "selected") ; deselect the current tab
    (.removeClass (js/$ ".popup-tab-content") "selected") ; and hide the current tab's contents
    (.addClass target "selected")             ; select the new tab
    (.addClass (js/$ tab-content) "selected") ; show the new tab's content
    (let [selected-pill (.data (js/$ ".popup-tab-content.selected .finish-types-list > li.selected") "tab")]
      (.removeClass (js/$ ".popup-tab-content.selected .finish-tab-wrap .finish-tab-content") "selected") ; make sure only the selected pill's contents are showing
      (.addClass (js/$ (str ".finish-tab-wrap " "#" selected-pill)) "selected"))))

(defn popup-tab-wrap []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))
        num-lead-times (count lead-times-set)
        epp-id (:epp-id selected-prod)]
    
    [:div#style-2.scrollbar
     [:div.popup-tab-wrap.force-overflow
      (if (lead-times-set "quick")
        [tab-contents "quick" selected-prod lead-times-set (= 3 num-lead-times)])

      (if (lead-times-set "three-week")
        [tab-contents "three-week" selected-prod lead-times-set (= 2 num-lead-times)])

      (if (lead-times-set "std")
        [tab-contents "std" selected-prod lead-times-set (= 1 num-lead-times)])]]))

(defn product-tabs []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))
        epp-id (:epp-id selected-prod)
        tab-width (case (count lead-times-set)
                    (0 1) "100%"
                    2 "50%"
                    3 "33.33%")]

    [:div.essentials-product-tabs
     ^{:key :epp-id}
     [:ul.essentials-tab-list
      (if (lead-times-set "quick")
        (do
          ^{:key (str epp-id "-" "quick")}
          [:li {:id (str epp-id "-" "quick")
                :data-tab "quick"
                :class (if (= 3 (count lead-times-set)) "selected")
                :style {:width tab-width}
                :on-click lead-time-tab-clicked}
           [:span.tab-color.quick-lead-active]
           [:a.tab-nav "Essentials Quickship options"]]))

      (if (lead-times-set "three-week")
        (do
          ^{:key (str epp-id "-" "three-week")}
          [:li {:id (str epp-id "-" "three-week")
                :data-tab "three-week"
                :class (if (= 2 (count lead-times-set)) "selected")
                :style {:width tab-width}
                :on-click lead-time-tab-clicked}
           [:span.tab-color.three-ship-active]
           [:a.tab-nav "Essentials 3 week options "]]))

      (if (lead-times-set "std")
        (do
          ^{:key (str epp-id "-" "std")}
          [:li {:id (str epp-id "-" "std")
                :data-tab "std"
                :class (if (= 1 (count lead-times-set)) "selected")
                :style {:width tab-width}
                :on-click lead-time-tab-clicked}
           [:span.tab-color.standard-ship-active]
           [:a.tab-nav "Standard Ship options"]]))]

     [:select.tab-select-option
      (if (lead-times-set "quick")
        [:option {:value "tab1"} "ESSENTIALS Quickship options"])

      (if (lead-times-set "three-week")
        [:option {:value "tab2"} "Essentials 3 week options"])

      (if (lead-times-set "std")
        [:option {:value "tab3"} "Standard Ship options"])]

     [popup-tab-wrap selected-prod lead-times-set]
     
     #_[:div.popup-tab-wrap {:style {:height "450px"}}

        (if (lead-times-set "quick")
          [:<> [tab-contents "quick"]])

        (if (lead-times-set "three-week")
          [:<> [tab-contents "three-week"]])

        (if (lead-times-set "std")
          [:<> [tab-contents "std"]])]

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
            [:li [:div.swatch-div [:img {:src "/images/upholstery-8.jpg" :data-no-retina ""}]] [:p "Textile 8"]]]]]]]]))

(defn modal-popup []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))]
    [:div#essentials-modal.white-popup-block.mfp-hide
     [:div.essentials-modal-wrap
      [:div.header-popup-view
       [:div.popup-action-list-wrap
        [:ul.popup-action-list-view
         [:li [:span.pop-action-icon]
          [:ul.popup-action-list
           [:li [:a {:href (str "https://www.knoll.com/product/" (:product-id selected-prod) "?section=design") :target "_blank"} " Visit Full Product Page"]]
           [:li [:a {:href "javascript:;"} "Share"]] [:li [:a {:href "javascript:;" :on-click #(.print js/window)} "PRINT"]]
           [:li [:a {:href "javascript:;"} "View essentials brochure"]]]]]]
       [:a.popup-modal-dismiss {:on-click #(->> js/$ .-magnificPopup .close)} "Dismiss"]]

      [:div.owl-popup-div
       [:div.item
        [:div.essentials-modal-content
         [:div.essentials-product-img
          [:div.essentials-product-img-wrap
           (when (:hero1-img selected-prod)
             ^{:key (str (:epp-id selected-prod) "-" (:hero1-img selected-prod))}
             [:img {:src (str "https://knlprdwcsmgt.knoll.com/media" (:hero1-img selected-prod)) :data-no-retina ""}])]
          [:div.essentials-product-img-detail
           [:h2 (:title selected-prod)]
           [:div {:dangerouslySetInnerHTML {:__html (:short-text selected-prod)}}]]]

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
