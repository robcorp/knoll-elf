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
       [:div.product-col.clearfix
        [filters-view]
        [filtered-products-view]]]]]))


(defn essential-product-summary [label {:keys [epp-id title product-name lead-times thumb-img]}]
  (let [lead-times-set (set lead-times)]
    [:li
     [:a.popup-modal {:href "#essentials-modal"
                      :on-click #(evt> [::events/product-selected label epp-id])}
      [:div.product-col-image
       [:img {:src (str config/media-url-base thumb-img) :data-no-retina ""}]]
      [:ul.lead-time-status
       (if (lead-times-set "quick")
         [:li.quick-lead-active])
       (if (lead-times-set "three-week")
         [:li.three-ship-active])
       (if (lead-times-set "std")
         [:li.standard-ship-active])]
      [:p title]]]))

(defn lead-time-filter-radio-button [{:keys [li-id li-class id lead-time label value]} filter]
  [:li {:key id :id li-id :class ["lead-time-list-types" li-class]}
   [:input.check-in {:type "radio"
            :id id
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
                          [:div.product-type-check.has-filter-submenu
                           [:h4 (if disable-group? {:class "disable-filter"}) description]
                           [:ul.product-type-check-list {:style {:display "none"}}
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
     [:div.filter-view-head
      [:h3 "Filter By"]
      [:p.reset-filter-link
       {:style {:display (if show-reset? "block" "none")}
        :on-click #(evt> [::events/reset-product-type-filters])}
       "Reset"]]
     [product-type-filter-group seating-filter-options filtered-prods]
     [product-type-filter-group tables-filter-options filtered-prods]
     [product-type-filter-group storage-filter-options filtered-prods]
     [product-type-filter-group power-data-filter-options filtered-prods]
     [product-type-filter-group work-tools-filter-options filtered-prods]
     [product-type-filter-group screen-board-filter-options filtered-prods]

     [:div.mobile-visible
      [:a.apply_btn.accordian_btn {:on-click close-filter-slideout} " < APPLY AND RETURN"]]]))

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
              [:img {:src (str config/media-url-base img)}])]]))]]))


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
    ^{:key (str "finish-" title "-tab")}
    [:div {:id (str "finish-" (str/replace title #"[^a-zA-Z0-9-]" ""))
           :class ["finish-tab-content" (if (= i 0) "selected")]}
     [:h5.print-show title]
     [:ul.frame-list
           (for [fin fins]
             ^{:key (:id fin)}
             [:li
              [:div.swatch-div
               [:img {:src (str config/media-url-base (:img fin)) :data-no-retina ""}]]
              [:p (:color fin)]])]]))

(defn- fabric-grade-pill-clicked [evt]
  (let [target (js/$ (.-currentTarget evt))
        tab (.data target "tab")
        tab-content (str ".popup-tab-content.selected " "#" tab ".upholstery-tab-content")]

    (.removeClass (js/$ ".popup-tab-content.selected .upholstery-types-list > li") "selected") ; deselect the current pill
    (.removeClass (js/$ ".popup-tab-content.selected .upholstery-tab-content") "selected") ; and hide the current pill's tab contents
    (.addClass target "selected") ; select the new tab
    (.addClass (js/$ tab-content) "selected")))

(defn- create-fabric-grade-pill [i grade]
  ^{:key (str "grade-" grade "-pill")}
  [:li {:class (if (= i 0) "selected" "")
        :data-tab (str "grade-" grade)
        :on-click fabric-grade-pill-clicked}
   [:a {:href "javascript:;"} grade]])

(defn- create-fabric-swatch [i fab]
  (let [name (:Name fab)
        part (:PartNum fab)
        grade (:Grade fab)
        primarySku (:PrimarySKU fab)]

    ^{:key (str grade "-" part)}
    [:li {:class ["has-sub-tab" (if (= i 0) " selected")]
          :on-click #(evt> [::events/show-fabric-skus part])}
     [:div.swatch-div
      [:img {:src (str "https://www.knoll.com/textileimages/th/" part primarySku ".jpg")}]]
     [:p name]]))

(defn- create-fabric-grade-tab [i [grade fabs]]
  (when grade
    ^{:key (str "grade-" grade "-tab")}
    [:div {:id (str "grade-" grade)
           :class ["upholstery-tab-content" (if (= i 0) "selected")]}
     [:h5.print-show (str "Grade " grade)]
     [:ul.upholstery-textile-list
      (map-indexed create-fabric-swatch (sort-by :Name fabs))]]))

(defn approved-fabrics [lead-time]
  (let [selected-prod (<sub [::subs/selected-product])
        fabs (case lead-time
               "std" (<sub [::subs/selected-product-all-textiles])
               "three-week" (<sub [::subs/selected-product-essential-textiles]))
        grades (->> fabs keys sort)]
    
    (println "in approved-fabrics lead-time: " lead-time ", grades: " grades)
    (when (> (count fabs) 0)
      [:div.upholstery-list-wrap
       [:h4 "Fabrics"]
       [:div.tab-main
        [:label "Grade:"]
        [:ul.upholstery-types-list
         (map-indexed create-fabric-grade-pill grades)]]
       [:div.upholstery-tab-wrap
        (map-indexed create-fabric-grade-tab (sort fabs))]])))

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
         (map-indexed create-finish-types-tab avail-fin-mods)]])

     (if (not= "quick" lead-time)
       [approved-fabrics lead-time])]))

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

(defn- lead-time-dropdown-selection-changed [evt]
  (let [tab (.. evt -target -value)
        tab-content (str "#" tab ".popup-tab-content")]

    (.removeClass (js/$ ".essentials-tab-list > li") "selected") ; deselect the current tab
    (.removeClass (js/$ ".popup-tab-content") "selected") ; and hide the current tab's contents
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
        lead-times-count (count lead-times-set)
        epp-id (:epp-id selected-prod)
        tab-width (case lead-times-count
                    (0 1) "100%"
                    2 "50%"
                    3 "33.33%")
        select-default-value (case lead-times-count
                               3 "quick"
                               2 "three-week"
                               (0 1) "std")]

    [:div.essentials-product-tabs
     ^{:key :epp-id}
     [:ul.essentials-tab-list
      (if (lead-times-set "quick")
        (do
          ^{:key (str epp-id "-" "quick")}
          [:li {:id (str epp-id "-" "quick")
                :data-tab "quick"
                :class (if (= 3 lead-times-count) "selected")
                :style {:width tab-width}
                :on-click lead-time-tab-clicked}
           [:span.tab-color.quick-lead-active]
           [:a.tab-nav "Essentials Quickship options"]]))

      (if (lead-times-set "three-week")
        (do
          ^{:key (str epp-id "-" "three-week")}
          [:li {:id (str epp-id "-" "three-week")
                :data-tab "three-week"
                :class (if (= 2 lead-times-count) "selected")
                :style {:width tab-width}
                :on-click lead-time-tab-clicked}
           [:span.tab-color.three-ship-active]
           [:a.tab-nav "Essentials 3 week options "]]))

      (if (lead-times-set "std")
        (do
          ^{:key (str epp-id "-" "std")}
          [:li {:id (str epp-id "-" "std")
                :data-tab "std"
                :class (if (= 1 lead-times-count) "selected")
                :style {:width tab-width}
                :on-click lead-time-tab-clicked}
           [:span.tab-color.standard-ship-active]
           [:a.tab-nav "Standard Ship options"]]))]

     ^{:key (str "select-" epp-id)}
     [:select.tab-select-option {:defaultValue select-default-value
                                 :on-change lead-time-dropdown-selection-changed}
      (if (lead-times-set "quick")
        [:option {:value "quick"} "ESSENTIALS QUICKSHIP OPTIONS"])

      (if (lead-times-set "three-week")
        [:option {:value "three-week"} "ESSENTIALS 3 WEEK OPTIONS"])

      (if (lead-times-set "std")
        [:option {:value "std"} "STANDARD SHIP OPTIONS"])]

     [popup-tab-wrap selected-prod lead-times-set]]))

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
             [:img {:src (str config/media-url-base (:hero1-img selected-prod)) :data-no-retina ""}])]
          [:div.essentials-product-img-detail
           [:h2 {:class (:apprvId selected-prod)} (:title selected-prod)]
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
