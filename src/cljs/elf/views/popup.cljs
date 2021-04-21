(ns elf.views.popup
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [elf.config :as config]
   [elf.events :as events]
   [elf.subs :as subs]
   [com.rpl.specter :refer [ALL collect-one] :refer-macros [select select-first] :as spctr]
   [clojure.string :as str]
   [cljsjs.clipboard] ; required in order to make the global js/ClipboardJS available
   ))

(def <sub (comp deref re-frame/subscribe)) ; permits using (<sub [::subs/name]) rather than @(subscribe [::subs/name])
(def evt> re-frame/dispatch)


(defn- create-finish-types-pill [i [title fins]]
  (let [finish-types-pill-clicked (fn [evt]
                                    (let [target (js/$ (.-currentTarget evt))
                                          tab (.data target "tab")
                                          tab-content (str ".popup-tab-content.selected " "#" tab ".finish-tab-content")]

                                      (.removeClass (js/$ ".popup-tab-content.selected .finish-types-list > li") "selected") ; deselect the current pill
                                      (.removeClass (js/$ ".finish-tab-content") "selected") ; and hide the current pill's tab contents
                                      (.addClass target "selected") ; select the new tab
                                      (.addClass (js/$ tab-content) "selected")))]

    (when (pos? (count fins))
      ^{:key (str "finish-" title - "pill")}
      [:li {:class (if (= i 0) "selected" "")
            :data-tab (str "finish-" (str/replace title #"[^a-zA-Z0-9-]" ""))
            :on-click finish-types-pill-clicked}
       [:a {:href "javascript:;"} title]])))

(defn- create-finish-types-tab [i [title fins]]
  (when (pos? (count fins))
    ^{:key (str "finish-" title "-tab")}
    [:div {:id (str "finish-" (str/replace title #"[^a-zA-Z0-9-]" ""))
           :class ["finish-tab-content" (when (= i 0) "selected")]}
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
        primarySku (:PrimarySKU fab)
        fabric-swatch-clicked (fn [evt]
                                (let [target (js/$ (.-currentTarget evt))
                                      tab (.data target "tab")
                                      part (str/replace tab #"fabric-(.*)-tab" "$1")]

                                  (evt> [::events/show-fabric-skus part])
                                  (.hide (js/$ ".popup-tab-content.selected .upholstery-list-wrap .tab-main"))
                                  (.hide (js/$ ".popup-tab-content.selected .upholstery-list-wrap .upholstery-tab-wrap"))
                                  (.show (js/$ (str ".popup-tab-content.selected .upholstery-list-wrap .sub-tab-wrap #" tab)))))]

    ^{:key (str grade "-" part)}
    [:li {:class ["has-sub-tab" (when (= i 0) " selected")]
          :data-tab (str "fabric-" part "-tab")
          :on-click fabric-swatch-clicked}
     [:div.swatch-div
      [:img {:src (str "https://www.knoll.com/textileimages/th/" part primarySku ".jpg")}]]
     [:p name]]))

(defn- create-leather-swatch [leather]
  (let [name (:Name leather)
        part (:PartNum leather)
        grade (:Grade leather)
        image (:LeatherImage leather)]

    ^{:key (str grade "-" part)}
    [:li {:data-tab (str "fabric-" part "-tab")}
     [:div.swatch-div
      [:img {:src (str "https://www.knoll.com/nkdc/images/essentials/essentials-leathers/" image)}]]
     [:p name]]))

(defn- create-fabric-grade-tab [i [grade fabs]]
  (when grade
    ^{:key (str "grade-" grade "-tab")}
    [:div {:id (str "grade-" grade)
           :class ["upholstery-tab-content" (when (= i 0) "selected")]}
     [:h5.print-show (str "Grade " grade)]
     [:ul.upholstery-textile-list
      (map-indexed create-fabric-swatch (sort-by :Name fabs))]]))

(defn- essential-colors [fab]
  (let [part-len (count (:PartNum fab))
        fab-colors (:FabricColors fab)
        ess-skus (map #(subs (str/trim %) part-len)
                      (str/split (:EssntlSKUs fab) #","))
        ess-colors (filter not-empty (map #(some (fn [sku] (when (= (first sku) %) sku)) fab-colors) ess-skus))]

    ess-colors))

(defn- create-fabric-grade-sub-tab [lead-time [grade fabs]]
  (for [fab fabs]
    (let [part (:PartNum fab)
          fab-name (:Name fab)
          colors (case lead-time
                   "std" (:FabricColors fab)
                   "three-week" (essential-colors fab))
          return-to-fabrics-view (fn [evt]
                                   (let [target (js/$ (.-currentTarget evt))
                                         tab (.data target "tab")]

                                     (.hide (js/$ (str ".popup-tab-content.selected .upholstery-list-wrap .sub-tab-wrap #" tab)))
                                     (.show (js/$ ".popup-tab-content.selected .upholstery-list-wrap .tab-main"))
                                     (.show (js/$ ".popup-tab-content.selected .upholstery-list-wrap .upholstery-tab-wrap"))))]

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
                 :on-click return-to-fabrics-view}
             "Back to all grade " grade]]]
          [:h5 fab-name " " part]
          [:ul.upholstery-textile-list
           (for [[sku color-name] colors]
             ^{:key (str lead-time grade part sku color-name)}
             [:li
              [:a {:href (str "https://www.knoll.com/knolltextileproductdetail/" (-> fab-name
                                                                                     (str/replace " " "+")
                                                                                     (str/replace "'" "")) "?sku=" sku) :target "_blank"}
               [:div.swatch-div [:img {:src (str "https://www.knoll.com/textileimages/th/" part sku ".jpg") :data-no-retina ""}]]
               [:p sku " " color-name]]])]])])))

(defn- approved-fabrics [lead-time]
  (let [fabs (case lead-time
               "std" (<sub [::subs/selected-product-all-textiles])
               "three-week" (<sub [::subs/selected-product-essential-textiles]))
        leathers (case lead-time
                   "std" (<sub [::subs/selected-product-all-leathers])
                   "three-week" (<sub [::subs/selected-product-essential-leathers]))
        grades (->> fabs keys sort)]

    (when (pos? (+ (count fabs) (count leathers)))
      [:div.upholstery-list-wrap
       [:h4 "Approved Fabrics"]
       [:div.tab-main
        [:label "Grade:"]
        [:ul.upholstery-types-list
         (map-indexed create-fabric-grade-pill grades)
         (when (seq leathers)
           [:li {:class (if-not (seq fabs) "selected" "") ;; if no fabs, then autoselect Leathers
                 :data-tab (str "grade-" "leather")
                 :on-click fabric-grade-pill-clicked}
            [:a {:href "javascript:;"} "Leather"]])]]
       [:div.upholstery-tab-wrap
        (map-indexed create-fabric-grade-tab (sort fabs))
        (when (seq leathers)
          [:div {:id "grade-leather"
                 :class ["upholstery-tab-content" (if-not (seq fabs) "selected" "")] }
           [:h5.print-show "Leather"]
           [:ul.upholstery-textile-list
            (map create-leather-swatch (sort-by :Name leathers))]])]
       [:div.sub-tab-wrap
        (for [fab (sort fabs)]
          (create-fabric-grade-sub-tab lead-time fab))]])))

(defn- tab-contents [lead-time selected-prod selected?]
  (let [avail-fin-mods (select [:availFinMods ALL #(not= "Options" (:title %)) (collect-one :title) (keyword lead-time) :fins] selected-prod)
        [optsTitle opts] (select-first [:availFinMods ALL #(= "options" (str/lower-case (:title %))) (collect-one :title) (keyword lead-time)] selected-prod)
        tab-content-class (if selected? "selected" "")
        print-show-text (case (keyword lead-time)
                          :one-to-three-day "Essentials 1-3 Day options"
                          :quick "Essentials Quickship options"
                          :three-week "Essentials 3 week options"
                          :std "Standard Ship options")]

    ^{:key (str (:epp-id selected-prod) "-" lead-time)}
    [:div.popup-tab-content {:id lead-time :class tab-content-class}
     [:h3.print-show.print-show-h3
      [:a.tab-nav print-show-text]]
     
     (when opts
       [:div.options-list-wrap
        [:h4 optsTitle]
        [:div {:dangerouslySetInnerHTML {:__html (:optsTxt opts)}}]])

     (when (pos? (count avail-fin-mods))
       [:div.finish-list-wrap
        [:h4 "Finishes"]
        [:div.tab-main
         [:ul.finish-types-list
          (map-indexed create-finish-types-pill avail-fin-mods)]]

        [:div.finish-tab-wrap
         (map-indexed create-finish-types-tab avail-fin-mods)]])

     (when (or (= lead-time "std")
             (and (= lead-time "three-week")
                  (not= "Y" (:excl3wk selected-prod))))
       [approved-fabrics lead-time])]))

(defn- popup-tab-wrap []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))
        first-tab (atom nil)]
    
    [:div.popup-tab-wrap
     (when (lead-times-set "one-to-three-day")
       (if-not @first-tab (reset! first-tab "one-to-three-day")) ;; if first-tab hasn't been set yet, set it to "quick"
       [tab-contents "one-to-three-day" selected-prod (= @first-tab "one-to-three-day")])

     (when (lead-times-set "quick")
       (if-not @first-tab (reset! first-tab "quick")) ;; if first-tab hasn't been set yet, set it to "quick"
       [tab-contents "quick" selected-prod (= @first-tab "quick")])

     (when (lead-times-set "three-week")
       (if-not @first-tab (reset! first-tab "three-week")) ;; if first-tab hasn't been set yet, set it to "three-week"
       [tab-contents "three-week" selected-prod (= @first-tab "three-week")])

     (when (lead-times-set "std")
       (if-not @first-tab (reset! first-tab "std")) ;; if first-tab hasn't been set yet, set it to "std"
       [tab-contents "std" selected-prod (= @first-tab "std")])]))

(defn- product-tabs []
  (let [selected-prod (<sub [::subs/selected-product])
        lead-times-set (set (:lead-times selected-prod))
        lead-times-count (count lead-times-set)
        epp-id (:epp-id selected-prod)
        tab-width (case lead-times-count
                    (0 1) "100%"
                    2 "50%"
                    3 "33.33%"
                    4 "25%")
        select-default-value (case lead-times-count
                               4 "one-to-three-day"
                               3 "quick"
                               2 "three-week"
                               (0 1) "std")
        first-tab (atom nil)
        lead-time-tab-clicked (fn [evt]
                                (let [target (js/$ (.-currentTarget evt))
                                      tab (.data target "tab")
                                      tab-content (str "#" tab ".popup-tab-content")]

                                  (.removeClass (js/$ ".essentials-tab-list > li") "selected") ; deselect the current tab
                                  (.removeClass (js/$ ".popup-tab-content") "selected") ; and hide the current tab's contents
                                  (.addClass target "selected") ; select the new tab
                                  (.addClass (js/$ tab-content) "selected") ; show the new tab's content
                                  (.removeClass (js/$ ".popup-tab-content.selected .finish-tab-wrap .finish-tab-content") "selected") ; make sure only the selected pill's contents are showing
                                  (when-let [selected-pill (.data (js/$ ".popup-tab-content.selected .finish-types-list > li.selected") "tab")]
                                    (.addClass (js/$ (str ".finish-tab-wrap " "#" selected-pill)) "selected"))))

        lead-time-dropdown-selection-changed (fn [evt]
                                               (let [tab (.. evt -target -value)
                                                     tab-content (str "#" tab ".popup-tab-content")]

                                                 (.removeClass (js/$ ".essentials-tab-list > li") "selected") ; deselect the current tab
                                                 (.removeClass (js/$ ".popup-tab-content") "selected") ; and hide the current tab's contents
                                                 (.addClass (js/$ tab-content) "selected") ; show the new tab's content
                                                 (let [selected-pill (.data (js/$ ".popup-tab-content.selected .finish-types-list > li.selected") "tab")]
                                                   (.removeClass (js/$ ".popup-tab-content.selected .finish-tab-wrap .finish-tab-content") "selected") ; make sure only the selected pill's contents are showing
                                                   (.addClass (js/$ (str ".finish-tab-wrap " "#" selected-pill)) "selected"))))]

    [:div.essentials-product-tabs
     ^{:key :epp-id}
     [:ul.essentials-tab-list
      (when (lead-times-set "one-to-three-day")
        (if-not @first-tab (reset! first-tab "one-to-three-day"))
        ^{:key (str epp-id "-" "one-to-three-day")}
        [:li {:id (str epp-id "-" "one-to-three-day")
              :data-tab "one-to-three-day"
              :class (when (= @first-tab "one-to-three-day") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.one-to-three-day-lead-active]
         [:a.tab-nav "Essentials 1-3 Day options"]])

      (when (lead-times-set "quick")
        (if-not @first-tab (reset! first-tab "quick"))
        ^{:key (str epp-id "-" "quick")}
        [:li {:id (str epp-id "-" "quick")
              :data-tab "quick"
              :class (when (= @first-tab "quick") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.quick-lead-active]
         [:a.tab-nav "Essentials Quickship options"]])

      (when (lead-times-set "three-week")
        (if-not @first-tab (reset! first-tab "three-week"))
        ^{:key (str epp-id "-" "three-week")}
        [:li {:id (str epp-id "-" "three-week")
              :data-tab "three-week"
              :class (when (= @first-tab "three-week") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.three-ship-active]
         [:a.tab-nav "Essentials 3 week options "]])

      (when (lead-times-set "std")
        (if-not @first-tab (reset! first-tab "std"))
        ^{:key (str epp-id "-" "std")}
        [:li {:id (str epp-id "-" "std")
              :data-tab "std"
              :class (when (= @first-tab "std") "selected")
              :style {:width tab-width}
              :on-click lead-time-tab-clicked}
         [:span.tab-color.standard-ship-active]
         [:a.tab-nav "Standard Ship options"]])]

     ^{:key (str "select-" epp-id)}
     [:select.tab-select-option {:defaultValue select-default-value
                                 :on-change lead-time-dropdown-selection-changed}
      (when (lead-times-set "one-to-three-day")
        [:option {:value "one-to-three-day"} "ESSENTIALS 1-3 DAY OPTIONS"])

      (when (lead-times-set "quick")
        [:option {:value "quick"} "ESSENTIALS QUICKSHIP OPTIONS"])

      (when (lead-times-set "three-week")
        [:option {:value "three-week"} "ESSENTIALS 3 WEEK OPTIONS"])

      (when (lead-times-set "std")
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
                              (.on clipboard "success" (fn [_]
                                                         (.show (js/$ "#copied-msg"))
                                                         (.setTimeout js/window (fn [] (.hide (js/$ "#copied-msg"))) 1000))))
      :component-did-update setup
      :component-will-unmount #(when-not (nil? @clipboard-atom)
                                 (.destroy @clipboard-atom)
                                 (reset! clipboard-atom nil))
      :reagent-render (fn []
                        [:li.clipboard {:data-clipboard-target target}
                         [:a {:href "javascript:;"} label
                          [:span#copied-msg {:style {:display "none" :font-size "75%"}}
                           " (copied to clipboard)"]]])})))

(defn modal-popup []
  (let [selected-prod (<sub [::subs/selected-product])
        loc (.-location js/window)
        orig (.-origin loc)
        path (.-pathname loc)]
    [:div#essentials-modal {:class ["white-popup-block" (when-not false #_config/debug? "mfp-hide")]}
     [:div.essentials-modal-wrap

      [:div.owl-controls
       [:div.owl-nav
        [:div.owl-prev {:on-click #(evt> [::events/select-previous-product])} "prev"]
        [:div.owl-next {:on-click #(evt> [::events/select-next-product])} "next"]]]
      
      [:div.header-popup-view
       [:div.popup-action-list-wrap
        [:div#clipboard-target {:style {:position "absolute" :top "-1000px" :left "-1000px"}}
         (str orig path "?pop=" (:epp-id selected-prod))]
        [:ul.popup-action-list-view
         [:li [:span.pop-action-icon]
          [:ul.popup-action-list
           [:li [:a {:href (str (when config/debug? "https://knlprdwcsmgt.knoll.com") "/product/" (:product-id selected-prod) "?section=design")
                     :target "_blank"} " Visit Full Product Page"]]
           [clipboard-button "Share" "#clipboard-target"]
           [:li [:a {:href "javascript:;" :on-click #(.print js/window)} "PRINT"]]
           [:li [:a {:href (str (when config/debug? "https://knlprdwcsmgt.knoll.com") "/design-plan/knoll-essentials/collections")} "ESSENTIALS COLLECTIONS"]]]]]]
       [:a.popup-modal-dismiss {:on-click #(->> js/$ .-magnificPopup .close)} "Dismiss"]]

      [:div.essentials-modal-content
       [:div.essentials-product-img
        [:div.essentials-product-img-wrap
         (when (:hero1-img selected-prod)
           ^{:key (str (:epp-id selected-prod) "-" (:hero1-img selected-prod))}
           [:img {:src (str config/media-url-base (:hero1-img selected-prod)) :data-no-retina ""}])]
        [:div.essentials-product-img-detail
         [:h2 {:class (:apprvId selected-prod)} (:title selected-prod)]
         [:div {:dangerouslySetInnerHTML {:__html (:short-text selected-prod)}}]]]

       [product-tabs]]]]))

