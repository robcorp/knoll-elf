(ns elf.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [elf.config :as config]
   [elf.events :as events]
   [elf.subs :as subs]
   [com.rpl.specter :refer [ALL collect-one] :refer-macros [select select-first] :as spctr]
   [clojure.string :as str]
   [cljsjs.clipboard]))

(def <sub (comp deref re-frame/subscribe)) ; permits using (<sub [::subs/name]) rather than @(subscribe [::subs/name])
(def evt> re-frame/dispatch)

(declare filters-view filtered-products-view modal-popup mouse-pos-comp)

(defn main-panel []
  (reagent/create-class
   {:display-name "main-panel"
    :component-did-mount #(let [parms (js/URLSearchParams. (.-search js/location))
                                pop (.get parms "pop")]
                            (when pop
                              ;; wait a sufficient amount of time for the page's javascript
                              ;; to finish loading and then "click" the selected product
                              ;; to trigger the popup
                              (.setTimeout js/window
                                           (fn [] (.click (js/$ (str "li#" pop))))
                                           1500)))
    :reagent-render (fn []
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
                            [filtered-products-view]]]]]))}))


(defn- essential-product-summary [label {:keys [epp-id title product-name lead-times thumb-img]}]
  (let [lead-times-set (set lead-times)]
    [:li {:id epp-id
          :on-click #(evt> [::events/product-selected label epp-id])}
     [:div.popup-modal {:style {:cursor "pointer"}}
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

(defn- lead-time-filter-radio-button [{:keys [li-id li-class id lead-time label value]} filter]
  [:li {:key id :id li-id :class ["lead-time-list-types" li-class]}
   [:input.check-in {:type "radio"
            :id id
            :checked value
            :name "lead-times-radio"
            :on-change #(evt> [::events/lead-time-filter-radio-button-clicked lead-time])}]
   [:label.active {:for id} label]])

;;; render the Lead Time: filters 
(defn- lead-time-filters []
  (let [filters (<sub [::subs/lead-time-filters])]
    [:<>
     [:h3 "Lead Time:"]
     [:ul.lead-time-list
      (map lead-time-filter-radio-button filters)]]))

(defn- product-type-filter-group [filter-options filtered-prods]
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

(defn- product-type-filters []
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

(defn- search-box []
  (let [search-box-id (str (gensym "search-box-"))
        make-autocomplete
        (fn []
          (let [src (->> (<sub [::subs/visible-filtered-products])
                         (map (fn [p] {:label (:title p) :id (:epp-id p)}))
                         (into #{})
                         (sort-by :label))]

            (.autocomplete (js/$ (str "input#" search-box-id))
                           (clj->js {:source src
                                     :autoFocus true
                                     :select (fn [evt ui]
                                               (.val (js/$ (str "input#" search-box-id)) "")
                                               (let [label (.. ui -item -label)
                                                     id (.. ui -item -id)]
                                                 (.. js/document
                                                     (getElementById id)
                                                     scrollIntoView)
                                                 (.. (js/$ (str "li#" id)) fadeOut fadeIn))
                                               false)}))))]

    (reagent/create-class
     {:display-name "search-box"

      :reagent-render (fn []
                        (let [prods (<sub [::subs/visible-filtered-products])]
                          [:div.ui-widget.search-box
                           [:label {:for search-box-id} "Find Product: "]
                           [:input {:id search-box-id}]]))

      :component-did-mount make-autocomplete

      :component-did-update make-autocomplete})))

(defn- filters-view []
  [:div.left-filter-col.researchPage
   [:div.select-wrap
    [search-box]
    [lead-time-filters]
    [product-type-filters]]])

(defn- filtered-product-type-section [{:keys [label products]}]
  (when (not (empty? products))
    ^{:key label}
    [:div.product-list
     [:h3.titleGreyborder (str label " (" (count products) ")")]
     [:ul.product-list
      (for [prod products]
        (let [epp-id (:epp-id prod)]
          ^{:key epp-id}
          [essential-product-summary label prod]))]]))


(defn- filtered-products-view []
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

(defn- fabric-swatch-clicked [evt]
  (let [target (js/$ (.-currentTarget evt))
        tab (.data target "tab")
        part (str/replace tab #"fabric-(.*)-tab" "$1")]

    (evt> [::events/show-fabric-skus part])
    (.hide (js/$ ".popup-tab-content.selected .upholstery-list-wrap .tab-main"))
    (.hide (js/$ ".popup-tab-content.selected .upholstery-list-wrap .upholstery-tab-wrap"))
    (.show (js/$ (str ".popup-tab-content.selected .upholstery-list-wrap .sub-tab-wrap #" tab)))))

(defn- create-fabric-swatch [i fab]
  (let [name (:Name fab)
        part (:PartNum fab)
        grade (:Grade fab)
        primarySku (:PrimarySKU fab)]

    ^{:key (str grade "-" part)}
    [:li {:class ["has-sub-tab" (if (= i 0) " selected")]
          :data-tab (str "fabric-" part "-tab")
          :on-click fabric-swatch-clicked}
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

(defn- return-to-fabrics-view [evt]
  (let [target (js/$ (.-currentTarget evt))
        tab (.data target "tab")]

    (.hide (js/$ (str ".popup-tab-content.selected .upholstery-list-wrap .sub-tab-wrap #" tab)))
    (.show (js/$ ".popup-tab-content.selected .upholstery-list-wrap .tab-main"))
    (.show (js/$ ".popup-tab-content.selected .upholstery-list-wrap .upholstery-tab-wrap"))))

(defn- essential-colors [fab]
  (let [part-len (count (:PartNum fab))
        fab-colors (:FabricColors fab)
        ess-skus (map #(subs (str/trim %) part-len)
                      (str/split (:EssntlSKUs fab) #","))
        ess-colors (filter not-empty (map #(some (fn [sku] (if (= (first sku) %) sku)) fab-colors) ess-skus))]

    ess-colors))

(defn- create-fabric-grade-sub-tab [lead-time [grade fabs]]
  (for [fab fabs]
    (let [part (:PartNum fab)
          name (:Name fab)
          colors (case lead-time
                   "std" (:FabricColors fab)
                   "three-week" (essential-colors fab))]

      ^{:key (str lead-time grade part)}
      [:div {:id (str "fabric-" part "-tab")
             :class ["upholstery-tab-content" (str "grade-" grade)]}
       (if (empty? colors)
         [:p "Loading..."]
         [:<>
          [:ul.upholstery-types-sub-list
           [:li
	        [:a {:href "javascript:;"
                 :data-tab (str "fabric-" part "-tab")
                 :on-click return-to-fabrics-view} (str "Back to all grade " grade)]]]
          [:h5 (str name " " part)]
          [:ul.upholstery-textile-list
           (for [[sku name] colors]
             ^{:key (str lead-time grade part sku name)}
             [:li {:class (str lead-time grade part sku name)}
              [:div.swatch-div [:img {:src (str "https://www.knoll.com/textileimages/th/" part sku ".jpg") :data-no-retina ""}]]
              [:p (str sku " " name)]])]
            ])])))

(defn- approved-fabrics [lead-time]
  (let [selected-prod (<sub [::subs/selected-product])
        fabs (case lead-time
               "std" (<sub [::subs/selected-product-all-textiles])
               "three-week" (<sub [::subs/selected-product-essential-textiles]))
        grades (->> fabs keys sort)]
    
    (when (> (count fabs) 0)
      [:div.upholstery-list-wrap
       [:h4 "Approved Fabrics"]
       [:div.tab-main
        [:label "Grade:"]
        [:ul.upholstery-types-list
         (map-indexed create-fabric-grade-pill grades)]]
       [:div.upholstery-tab-wrap
        (map-indexed create-fabric-grade-tab (sort fabs))]
       [:div.sub-tab-wrap
        (for [fab (sort fabs)]
            (create-fabric-grade-sub-tab lead-time fab))]])))

(defn- tab-contents [lead-time selected-prod lead-times-set selected?]
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

     (if (or (= lead-time "std")
             (and (= lead-time "three-week")
                  (not= "Y" (:excl3wk selected-prod))))
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

(defn- popup-tab-wrap []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))
        num-lead-times (count lead-times-set)
        epp-id (:epp-id selected-prod)
        first-tab (atom nil)]
    
    [:div.popup-tab-wrap
     (when (lead-times-set "quick")
       (if-not @first-tab (reset! first-tab "quick")) ;; if first-tab hasn't been set yet, set it to "quick"
       [tab-contents "quick" selected-prod lead-times-set (= @first-tab "quick")])

     (when (lead-times-set "three-week")
       (if-not @first-tab (reset! first-tab "three-week")) ;; if first-tab hasn't been set yet, set it to "three-week"
       [tab-contents "three-week" selected-prod lead-times-set (= @first-tab "three-week")])

     (when (lead-times-set "std")
       (if-not @first-tab (reset! first-tab "std")) ;; if first-tab hasn't been set yet, set it to "std"
       [tab-contents "std" selected-prod lead-times-set (= @first-tab "std")])]))

(defn- product-tabs []
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
                               (0 1) "std")
        first-tab (atom nil)]

    [:div.essentials-product-tabs
     ^{:key :epp-id}
     [:ul.essentials-tab-list
      (when (lead-times-set "quick")
        (if-not @first-tab (reset! first-tab "quick"))
        ^{:key (str epp-id "-" "quick")}
        [:li {:id (str epp-id "-" "quick")
              :data-tab "quick"
              :class (if (= @first-tab "quick") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.quick-lead-active]
         [:a.tab-nav "Essentials Quickship options"]])

      (when (lead-times-set "three-week")
        (if-not @first-tab (reset! first-tab "three-week"))
        ^{:key (str epp-id "-" "three-week")}
        [:li {:id (str epp-id "-" "three-week")
              :data-tab "three-week"
              :class (if (= @first-tab "three-week") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.three-ship-active]
         [:a.tab-nav "Essentials 3 week options "]])

      (when (lead-times-set "std")
        (if-not @first-tab (reset! first-tab "std"))
        ^{:key (str epp-id "-" "std")}
        [:li {:id (str epp-id "-" "std")
              :data-tab "std"
              :class (if (= @first-tab "std") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.standard-ship-active]
         [:a.tab-nav "Standard Ship options"]])]

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

(defn clipboard-button [label target]
  (let [clipboard-atom (atom nil)
        setup #(let [clipboard (new js/ClipboardJS (reagent/dom-node %))]
                 (reset! clipboard-atom clipboard))]
    
    (reagent/create-class
     {:display-name "clipboard-button"
      :component-did-mount #(let [clipboard (new js/ClipboardJS (reagent/dom-node %))]
                              (reset! clipboard-atom clipboard)
                              (.on clipboard "success" (fn [e]
                                                         (.show (js/$ "#copied-msg"))
                                                         (.setTimeout js/window (fn [] (.hide (js/$ "#copied-msg"))) 1000))))
      :component-did-update setup
      :component-will-unmount #(when-not (nil? @clipboard-atom)
                                 (.destroy @clipboard-atom)
                                 (reset! clipboard-atom nil))
      :reagent-render (fn []
                        [:li.clipboard {:data-clipboard-target target}
                         [:a {:href "javascript:;"} label [:span#copied-msg {:style {:display "none"
                                                                                     :font-size "75%"}} " (copied to clipboard)"]]])})))

(defn- modal-popup []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))]
    [:div#essentials-modal {:class ["white-popup-block" (if-not false #_config/debug? "mfp-hide")]}
     [:div.essentials-modal-wrap
      [:div.header-popup-view
       [:div.popup-action-list-wrap
        [:div#clipboard-target {:style {:position "absolute" :top "-1000px" :left "-1000px"}}
         (let [loc (.-location js/window)] (str (.-origin loc) (.-pathname loc) "?pop=" (:epp-id selected-prod)))]
        [:ul.popup-action-list-view
         [:li [:span.pop-action-icon]
          [:ul.popup-action-list
           [:li [:a {:href (str "https://www.knoll.com/product/" (:product-id selected-prod) "?section=design") :target "_blank"} " Visit Full Product Page"]]
           [clipboard-button "Share" "#clipboard-target"]
           [:li [:a {:href "javascript:;" :on-click #(.print js/window)} "PRINT"]]
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

(defn- mouse-pos-comp []
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
