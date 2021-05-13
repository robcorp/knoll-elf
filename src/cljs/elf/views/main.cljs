(ns elf.views.main
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [elf.config :as config]
   [elf.events :as events]
   [elf.subs :as subs]
   [elf.views.popup :refer [modal-popup]]
   [com.rpl.specter :refer [ALL] :refer-macros [select] :as spctr]
   [cljsjs.clipboard] ; required in order to make the global js/ClipboardJS available
   ))

(def <sub (comp deref re-frame/subscribe)) ; permits using (<sub [::subs/name]) rather than @(subscribe [::subs/name])
(def evt> re-frame/dispatch)

(declare filters-view filtered-products-view mouse-pos-comp)

(defn main-panel []
  (reagent/create-class
   {:display-name "main-panel"
    :component-did-mount #(do
                            ;; set up Top button
                            (set! (.-onscroll js/window)
                                  (fn []
                                    (let [scrollTop (.. js/document -documentElement -scrollTop)]
                                      (.css (js/$ "#top-button") "display" (if (> scrollTop 100) "block" "none")))))

                            ;; If pop param is present, open the popup on that product
                            (let [parms (js/URLSearchParams. (.-search js/location))
                                  pop (.get parms "pop")]
                              (when pop
                                ;; wait a sufficient amount of time for the page's javascript
                                ;; to finish loading and then "click" the selected product
                                ;; to trigger the popup
                                (.setTimeout js/window
                                             (fn [] (.click (js/$ (str "li#" pop))))
                                             1500))))
    :reagent-render (fn []
                      (let [name (<sub [::subs/name])]
                        [:<> ; this allows sibling elements without needing to wrap in a separate [:div]
                         [modal-popup]
                         [:div.veil]
                         [:button#top-button {:title "Go to top"
                                              :on-click #(.animate (js/$ "body, html") #js{:scrollTop 0} 400)}
                          "Top"]

                         (when config/debug?
                           [:section.body_container]
                           [:div
                            [:h1 name]
                            [:p "(built using the re-frame app framework.)"]
                            #_[mouse-pos-comp]
                            [:hr]])
                         [:section.wrapper.essentials
                          [:section#page
                           [:div.product-col.clearfix
                            [filters-view]
                            [filtered-products-view]]]]]))}))

(defn- essential-product-summary [label {:keys [epp-id title lead-times thumb-img brands]}]
  (let [has-lead-time (->> lead-times (remove #(= % "std")) set)
        has-brand (set brands)]
    (when (not-empty has-lead-time)
      [:li {:id epp-id
            :on-click #(evt> [::events/product-selected label epp-id])}
       [:div.popup-modal {:style {:cursor "pointer"}}
        [:div.product-col-image
         [:img {:src (str config/media-url-base thumb-img) :data-no-retina ""}]]
        [:ul.lead-time-status
         (when (has-lead-time "one-to-three-day")
           [:li.one-to-three-day-lead-active])
         (when (has-lead-time "quick")
           [:li.quick-lead-active])
         (when (has-lead-time "three-week")
           [:li.three-ship-active])
         (when (has-lead-time "std")
           [:li.standard-ship-active])]
        (when (has-brand "Muuto")
          [:div
           [:img.brand-logo {:src "/images/muuto-logo.svg" :alt "Muuto logo" :data-no-retina ""}]])
        [:p title]]])))

(defn- lead-time-filter-radio-button [{:keys [li-id li-class id lead-time label value]}]
  (when-not (= lead-time "std")
    [:li {:key id :id li-id :class ["lead-time-list-types" li-class]}
     [:input.check-in {:type "radio"
                       :id id
                       :checked value
                       :name "lead-times-radio"
                       :on-change #(evt> [::events/lead-time-filter-radio-button-clicked lead-time])}]
     [:label {:for id
                     :dangerouslySetInnerHTML {:__html label}}]]))


;;; render the Lead Time: filters 
(defn- lead-time-filters []
  (let [filters (<sub [::subs/lead-time-filters])]
    [:<>
     [:h3 "Lead Time:"]
     [:ul.filter-list
      (map lead-time-filter-radio-button filters)]]))

(defn- ship-method-filters []
  (let [{:keys [name description items]} (<sub [::subs/ship-method-filters])
        #_#_available-brands (conj (set (select [ALL #(not (empty? (product-category %))) product-category ALL] filtered-prods)) "All")]
    [:<>
     [:h3 description ":"]
     [:ul.filter-check-list
      (for [i items]
            (let [{:keys [label value]} i
                  id (str name ":" label)]
              ^{:key id}
              [:li
               [:input {:type "checkbox"
                        :id id
                        #_#_:checked (if (available-categories label)
                                   value
                                   false)
                        #_#_:class (if (available-categories label) "" "disable-filter")
                        :on-change #(evt> [::events/product-type-filter-checkbox-clicked id])}]
               [:label {:for id} (if (= "All" label)
                                   (str label " " description "s")
                                   label)]]))]]))

(defn- brand-filters []
  (let [{:keys [name description items]} (<sub [::subs/brand-filters])]
    [:<>
     [:h3 description ":"]
     [:ul.filter-check-list
      (for [i items]
            (let [{:keys [label value]} i
                  id (str name ":" label)]
              ^{:key id}
              [:li
               [:input {:type "checkbox"
                        :id id
                        #_#_:checked (if (available-categories label)
                                   value
                                   false)
                        #_#_:class (if (available-categories label) "" "disable-filter")
                        :on-change #(evt> [::events/brand-filter-checkbox-clicked id])}]
               [:label {:for id} (if (= "All" label)
                                   (str label " " description "s")
                                   label)]]))]]))

(defn- product-type-filter-group [_ _]
  (let [open? (reagent/atom false)] ; local state indicating whether the filter UI is open or closed.
    (fn [filter-options filtered-prods]
      (let [{:keys [name description product-category items]} filter-options
            has-selection? #_true (some #(true? (:value %)) items)
            available-categories (conj (set (select [ALL #(not (empty? (product-category %))) product-category ALL] filtered-prods)) "All")]

        [:div.product-type-check.has-filter-submenu
         [:h4 {:class [(when @open? "open") (when (true? has-selection?) "has-selection")]
               :on-click (fn [evt]
                            (swap! open? not)
                            (.slideToggle (.next (js/$ (.-currentTarget evt)) ".filter-check-list")))}
          description]
         [:ul.filter-check-list {:style {:display "none"}}
          (for [i items]
            (let [{:keys [label value]} i
                  id (str name ":" label)]
              ^{:key id}
              [:li
               [:input {:type "checkbox"
                        :id id
                        :checked (if (available-categories label)
                                   value
                                   false)
                        :class (if (available-categories label) "" "disable-filter")
                        :on-change #(evt> [::events/product-type-filter-checkbox-clicked id])}]
               [:label {:for id} (if (= "All" label)
                                   (str label " " description)
                                   label)]]))]]))))

(defn- close-filter-slideout []
  (.removeClass (js/$ ".select-wrap") "open")
  (.removeClass (js/$ "html") "hidescroll")
  (.removeClass (js/$ ".veil") "overlay"))

(defn- open-filter-slideout []
  (.toggleClass (js/$ ".select-wrap") "open")
  (.addClass (js/$ "html") "hidescroll")
  (.addClass (js/$ ".veil") "overlay")
  (.click (js/$ ".veil.overlay") close-filter-slideout))

(defn- product-type-filters []
  (let [filtered-prods (<sub [::subs/filtered-products])
        all-filter-options (<sub [::subs/all-filter-options])
        show-reset? (<sub [::subs/show-reset?])]

    [:div.filter-list
     [:div.filter-view-head
      [:h3 "Filter By"]
      (when show-reset?
        [:p.reset-filter-link
         {:on-click #(evt> [::events/reset-product-type-filters])}
         "Reset"])]

     (for [filter-options all-filter-options]
       ^{:key (str "product-type-filter-group-" (:name filter-options))}
       [product-type-filter-group filter-options filtered-prods])]))

(defn- search-box []
  (let [search-box-id (str (gensym "search-box-"))
        make-autocomplete
        (fn []
          (let [src (->> (<sub [::subs/visible-filtered-products])
                         (map (fn [p] {:label (:title p) :id (:epp-id p)}))
                         set
                         (sort-by :label))]

            (.autocomplete (js/$ (str "input#" search-box-id))
                           (clj->js {:source src
                                     :autoFocus false
                                     :select (fn [_ ui]
                                               (.val (js/$ (str "input#" search-box-id)) "")
                                               (let [id (.. ui -item -id)]
                                                 (.animate (js/$ "html, body")
                                                           #js {:scrollTop (.. (js/$ (str "#" id)) offset -top)} 400
                                                           #(.fadeToggle (js/$ (str "li#" id)))))
                                               false)}))))]

    (reagent/create-class
     {:display-name "search-box"

      :reagent-render (fn []
                        (let [_ (<sub [::subs/visible-filtered-products])]
                                        ; This <sub forces the
                                        ; re-rendering of this
                                        ; component wheneve the
                                        ; filtering changes.  The _
                                        ; means we don't actually use
                                        ; the <sub here.
                          [:div.ui-widget.elf-search-box
                           [:label {:for search-box-id} "Find Product: "]
                           [:input {:id search-box-id}]]))

      :component-did-mount make-autocomplete

      :component-did-update make-autocomplete})))

(defn- filters-view []
  [:div.left-filter-col.researchPage
   [:div.select-wrap
    [search-box]
    [lead-time-filters]
    [ship-method-filters]
    [product-type-filters]
    [brand-filters]
    [:div.mobile-visible
      [:a.apply_btn.accordian_btn {:on-click close-filter-slideout} " < APPLY AND RETURN"]]]])

(defn- filtered-product-type-section [{:keys [label products]}]
  (when (seq products)
    ^{:key label}
    [:div.product-list
     [:h3.titleGreyborder label " (" (count products) ")"]
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
        no-results? (<sub [::subs/no-results?])]

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
