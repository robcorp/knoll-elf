(ns elf.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [elf.db :as db]
            [elf.config :as config]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [com.rpl.specter :refer [ALL collect-one multi-path walker] :refer-macros [select select-first setval transform] :as spctr]
            [ajax.core :as ajax]
            [clojure.string :as str]))


(declare load-textiles-approvals load-textiles-info load-fabric-data load-all-products-and-finishes load-filter-options filter-category-products)

(reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   (load-textiles-approvals)
   (load-textiles-info)
   (run! load-filter-options ["ELFSeatingSelector"
                              "ELFTableSelector"
                              "ELFStorageSelector"
                              "ELFPowerAndDataSelector"
                              "ELFWorkToolsSelector"
                              "ELFScreensAndBoardsSelector"
                              "ELFShipMethodSelector"
                              "ELFBrandSelector"])
   (load-all-products-and-finishes)
   (filter-category-products db/default-db)))

(defn- category-products [db selector]
  (let [products (:filtered-products db)
        selected-filter (selector db)
        category-key (:product-category selected-filter)
        cat-products (sort-by :title
                              #(compare (str/lower-case %1) (str/lower-case %2))
                              (filter #(seq (category-key %)) products))
        label (:description selected-filter)
        no-product-filters-selected? (not-any? true? (select (multi-path [:ELFSeatingSelector :items ALL :value]
                                                                         [:ELFTableSelector :items ALL :value]
                                                                         [:ELFStorageSelector :items ALL :value]
                                                                         [:ELFPowerAndDataSelector :items ALL :value]
                                                                         [:ELFWorkToolsSelector :items ALL :value]
                                                                         [:ELFScreensAndBoardsSelector :items ALL :value]) db))
        categories (if no-product-filters-selected?
                     (select [:items ALL :label #(not= "All" %)] selected-filter)
                     (select [:items ALL #(true? (:value %)) :label] selected-filter))]

    (for [category categories]
      {:product-category category
       :label (str label " / " category)
       :products (filter #((set (category-key %)) category) cat-products)})))

(defn- filter-category-products [db]
  (assoc db
         :filtered-seating-products (category-products db :ELFSeatingSelector)
         :filtered-table-products (category-products db :ELFTableSelector)
         :filtered-storage-products (category-products db :ELFStorageSelector)
         :filtered-power-products (category-products db :ELFPowerAndDataSelector)
         :filtered-work-products (category-products db :ELFWorkToolsSelector)
         :filtered-screen-products (category-products db :ELFScreensAndBoardsSelector)))

(reg-event-db
 ::set-all-products-and-finishes
 (fn-traced [db [_ products finishes]]
   (try (.setItem js/localStorage "all-products" products)
        (catch :default err (println err "Couldn't store all-products in localStorage.")))
   (-> db
       (assoc :all-products products
              :filtered-products products
              :finishes finishes)
       (filter-category-products))))

(reg-event-db
 ::set-filter-options
 (fn-traced [db [_ selector resp]]
   (let [selector-key (keyword selector)
         product-category (select-first [selector-key :product-category] db/default-db)
         desc (:description resp)
         items (setval [spctr/BEFORE-ELEM] "All" (:items resp))
         filter-options {:name selector :description desc :product-category product-category :items (mapv (fn [i] {:label i :value false}) items)}]
     (.setItem js/localStorage selector filter-options)
     (assoc db selector-key filter-options))))

(reg-event-db
 ::set-textiles-approvals
 (fn-traced [db [_ approvals]]
   (assoc db :textiles-approvals approvals)))

(reg-event-db
 ::set-textiles-info
 (fn-traced [db [_ info]]
   (assoc db :textiles-info info)))

(reg-event-db
 ::set-fabric-colors
 (fn [db [_ partnum colors]]
   (let [color-names-skus (map (fn [c] [(:SkuNumber c) (:ColorName c)]) colors)]
     (setval [:textiles-info ALL #(= partnum (:PartNum %)) :FabricColors] color-names-skus db))))

(defn- update-lead-time-filter-state [selected-filter filters]
  (mapv #(assoc-in % [:value] (= selected-filter (:lead-time %))) filters))

(defn- selected-lead-time []
  (->> @re-frame.db/app-db
       :lead-time-filters
       (filter :value)
       first
       :lead-time))

(defn- filter-products-by-lead-time [lead-time prods]
  (if (= lead-time "all")
    prods ;; return prods unfiltered
    (filter #((set (:lead-times %)) lead-time) prods)))

(defn- selected-ship-methods []
  (->> @re-frame.db/app-db
       :ELFShipMethodSelector
       :items
       (filter :value)
       (map :label)
       set))

(defn- selected-brands []
  (->> @re-frame.db/app-db
       :ELFBrandSelector
       :items
       (filter :value)
       (map :label)
       set))

(defn- filter-products-by-ship-methods [ship-methods prods]
  (let [has-ship-methods (set ship-methods)]
    (if (or (empty? has-ship-methods)
            (has-ship-methods "All"))
      prods
      (filter #(not-empty (clojure.set/intersection has-ship-methods (set (:sm1-3d %)))) prods))))

(defn- filter-products-by-brands [brands prods]
  (let [has-brands (set brands)]
    (if (or (empty? has-brands)
            (has-brands "All"))
      prods
      (filter #(not-empty (clojure.set/intersection has-brands (set (:brands %)))) prods))))

(reg-event-db
 ::lead-time-filter-radio-button-clicked
 (fn-traced [db [_ lead-time] event]
            (let [updated-lead-time-filters (update-lead-time-filter-state lead-time (:lead-time-filters db))]

              (-> db
                  (assoc
                   :lead-time-filters updated-lead-time-filters
                   :filtered-products (->> (:all-products db)
                                           (filter-products-by-lead-time lead-time)
                                           (filter-products-by-brands (selected-brands))))
                  (filter-category-products)))))

(defn- toggle-filter-state [selected-filter filters]
  (let [selected-filter-value (select-first [ALL #(= selected-filter (:label %)) :value] filters)
        all-value (select-first [ALL #(= "All" (:label %)) :value] filters)]

    (if (= "All" selected-filter)
      (setval [ALL :value] (not selected-filter-value) filters)

      (if (and all-value selected-filter-value)
        (setval [(multi-path
                  (walker #(= "All" (:label %)))
                  (walker #(= selected-filter (:label %)))) :value] false filters)

        (transform [ALL #(= selected-filter (:label %)) :value] not filters)))))

(reg-event-db
 ::filter-checkbox-clicked
 (fn-traced [db [_ filter-id] event]
   (let [[selector-str label] (str/split filter-id #":")
         selector (keyword selector-str)
         filters (:items (selector db))
         updated-filters (toggle-filter-state label filters)
         enable-all? (every? true? (select [ALL #(not= "All" (:label %)) :value] updated-filters))
         updated-db (if enable-all?
                      (setval [selector :items] (setval [ALL #(= "All" (:label %)) :value] true updated-filters) db)
                      (setval [selector :items] updated-filters db))]

     (filter-category-products updated-db))))

(reg-event-db
 ::brand-filter-checkbox-clicked
 (fn-traced [db [_ filter-id] event]
            (let [[selector-str label] (str/split filter-id #":")
                  selector (keyword selector-str)
                  filters (:items (selector db))
                  updated-filters (toggle-filter-state label filters)
                  enable-all? (every? true? (select [ALL #(not= "All" (:label %)) :value] updated-filters))
                  updated-db (if enable-all?
                               (setval [selector :items] (setval [ALL #(= "All" (:label %)) :value] true updated-filters) db)
                               (setval [selector :items] updated-filters db))
                  no-brand-filters-selected? (not-any? true? (select [:ELFBrandSelector :items ALL :value] updated-db))
                  current-filtered-prods (:filtered-products updated-db)
                  brands-to-filter (set (map :label (if no-brand-filters-selected?
                                                      updated-filters
                                                      (filter :value updated-filters))))]

              #_(.log js/console brands-to-filter)

              (-> db
                  (assoc-in [selector :items] updated-filters)
                  (assoc
                   :filtered-products (->> (:all-products db)
                                           (filter-products-by-lead-time (selected-lead-time))
                                           (filter-products-by-brands brands-to-filter)))
                  (filter-category-products)))))

(defn- clear-all-product-filters [db]
  (setval (multi-path [:ELFSeatingSelector :items ALL :value]
                      [:ELFTableSelector :items ALL :value]
                      [:ELFStorageSelector :items ALL :value]
                      [:ELFPowerAndDataSelector :items ALL :value]
                      [:ELFWorkToolsSelector :items ALL :value]
                      [:ELFScreensAndBoardsSelector :items ALL :value])
          false db))

(reg-event-db
 ::reset-product-type-filters
 (fn-traced [db _]
   (-> db
       (clear-all-product-filters)
       (filter-category-products))))

(defn- setup-popup []
  (.. js/$ -magnificPopup
      (open (clj->js {:type "inline"
                      :midClick true
                      :showCloseBtn false
                      :items {:src "#essentials-modal"}
                      :key "elf-popup"
                      :overflowY "scroll"
                      :alignTop false
                      :mainClass "elfPopup"
                      :callbacks {:close #(.replaceState js/history nil nil (.-pathname js/location))}}))))

(reg-event-db
 ::product-selected
 (fn-traced [db [_ label epp-id] event]
            ;; show the popup
            (when-not false #_config/debug?
                    (setup-popup))

            #_(setup-owl-carousel)

            
            ;; change the URL to include the pop param
            (.replaceState js/history nil nil (str (.-pathname js/location) "?pop=" epp-id))


            ;; update the :selected-epp-id in the app db with the selected product's
            ;; label and epp-id
            (assoc db :selected-epp-id [label epp-id])))

(reg-event-db
 ::show-fabric-skus
 (fn-traced [db [evt partnum]]
   ;; if FabricColors don't exist in the db for the partnum,
   ;; call textiles webservice with partnums.
   ;; Otherwise, just return the db untouched
   (if-not (spctr/select-first [:textiles-info ALL #(= partnum (:PartNum %)) :FabricColors] db)
     (load-fabric-data partnum))

   db))

(defn- load-all-products-and-finishes []
  (let [path "/cs/Satellite?pagename=Knoll/Common/Utils/EssentialsPopupProductsJSON"
        all-products-url (if config/debug?
                           (if config/use-local-products?
                             "/js/elf/all-products.json" ;; use the local file - this file should be updated periodically using the json from prod or staging
                             (str #_"http://knldev2wcsapp1a.knoll.com" "http://knlprdwcsmgt1.knoll.com" path)) ;; use staging url
                           (str (.. js/window -location -origin) path)) ;; use the host of the current browser window
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-all-products-and-finishes (:all-products resp) (:finishes resp)]))
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request to get all-products failed: " status " " status-text)))]

    (ajax/GET all-products-url {:timeout 180000 :handler success-handler :error-handler error-handler :response-format :json :keywords? true})))

(defn- load-filter-options [selector]
  (let [baseURL (if config/debug?
                  "http://knlprdwcsmgt1.knoll.com"
                  (str (.. js/window -location -origin)))
        presentationObjectItemsURL (str baseURL "/cs/Satellite?pagename=Knoll/Common/Utils/PresentationObjectItemsJSON&presentationObject=" selector)
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-filter-options selector (:presentationObjectItems resp)]))
      
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request failed: " status " " status-text)))]

    (ajax/GET presentationObjectItemsURL {:handler success-handler
                                          :error-handler error-handler
                                          :response-format :json
                                          :keywords? true})))

(defn- visible-product-ids
  "Returns a vector of product-ids of all the currently visible products based on the current filter selections.
  This is useful for cycling through products on the modal popup."
  [db]
  (select (multi-path [:filtered-seating-products ALL (collect-one :label) :products ALL :epp-id]
                      [:filtered-table-products ALL (collect-one :label) :products ALL :epp-id]
                      [:filtered-storage-products ALL (collect-one :label) :products ALL :epp-id]
                      [:filtered-power-products ALL (collect-one :label) :products ALL :epp-id]
                      [:filtered-work-products ALL (collect-one :label) :products ALL :epp-id]
                      [:filtered-screen-products ALL (collect-one :label) :products ALL :epp-id]) db))

(defn- next-visible-prod-id [db]
  (let [current-prod (:selected-epp-id db)
        visible-prods (visible-product-ids db)
        i (.indexOf visible-prods current-prod)]
    (visible-prods (min (inc i) (dec (count visible-prods))))))

(defn- previous-visible-prod-id [db]
  (let [current-prod (:selected-epp-id db)
        visible-prods (visible-product-ids db)
        i (.indexOf visible-prods current-prod)]
    (visible-prods (max (dec i) 0))))

(reg-event-db
 ::select-previous-product
 (fn [db _]
   (let [prev-prod (previous-visible-prod-id db)
         [_ epp-id] prev-prod]

     ;; change the URL to include the pop param
     (.replaceState js/history nil nil (str (.-pathname js/location) "?pop=" epp-id))

     (assoc db :selected-epp-id prev-prod))))

(reg-event-db
 ::select-next-product
 (fn [db _]
   (let [next-prod (next-visible-prod-id db)
         [_ epp-id] next-prod]

     ;; change the URL to include the pop param
     (.replaceState js/history nil nil (str (.-pathname js/location) "?pop=" epp-id))

     (assoc db :selected-epp-id next-prod))))


(defn- load-textiles-approvals []
  (let [path "/js/elf/knolltextiles_approvals.json"
        url (if config/debug?
              path
              (str (.. js/window -location -origin) path))
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-textiles-approvals resp]))
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request to get knolltextiles-approvals failed: " status " " status-text)))]

    (ajax/GET url {:timeout 90000 :handler success-handler :error-handler error-handler :response-format :json :keywords? true})))

(defn- load-textiles-info []
  (let [path "/js/elf/knolltextiles_info.json"
        url (if config/debug?
              path ;; use the local
              (str (.. js/window -location -origin) path))
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-textiles-info resp]))
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request to get knolltextiles-info failed: " status " " status-text)))]

    (ajax/GET url {:timeout 90000 :handler success-handler :error-handler error-handler :response-format :json :keywords? true})))

(defn- load-fabric-data
  "Call the textiles webservice with the provided partnum"
  [partnum]
  (let [baseURL (if config/debug?
                  "https://www.knoll.com" #_"http://knlprdwcsmgt1.knoll.com"
                  ;; prod server is fast, but staging server may have updated
                  ;; data not yet in prod.
                  (str (.. js/window -location -origin)))
        textileWSURL (str baseURL "/textiles/" (str/replace partnum #"^[a-zA-Z]*" ""))
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-fabric-colors partnum (:FabricColors resp)]))

        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request failed: " status " " status-text)))]

    (ajax/GET textileWSURL {:handler success-handler
                            :error-handler error-handler
                            :response-format :json
                            :keywords? true})))


(comment
  (re-frame/dispatch-sync [:elf.events/initialize-db])

  (re-frame/dispatch [:elf.events/lead-time-filter-radio-button-clicked "all"])
  (re-frame/dispatch [:elf.events/lead-time-filter-radio-button-clicked "one-to-three-day"])
  (re-frame/dispatch [:elf.events/lead-time-filter-radio-button-clicked "quick"])
  (re-frame/dispatch [:elf.events/lead-time-filter-radio-button-clicked "three-week"])

  (defn ap [] @(re-frame/subscribe [:elf.subs/all-products]))
  (defn fp [] @(re-frame/subscribe [:elf.subs/filtered-products]))
  (defn vfp [] @(re-frame/subscribe [:elf.subs/visible-filtered-products]))
  (defn sp [] @(re-frame/subscribe [:elf.subs/selected-product]))


  (count (ap))
  (count (fp))
  (count (vfp))
  (count (set (vfp)))

  (selected-lead-time)
  (selected-ship-methods)
  (selected-brands)

  (count (filter-products-by-lead-time (selected-lead-time) (ap)))

  (count (filter-products-by-brands (selected-brands) (fp)))

  (->> (ap)
       (filter-products-by-lead-time (selected-lead-time))
       (filter-products-by-brands (selected-brands)))

  (->> (sp)
       :sm1-3d)

  (->> (vfp)
       (filter-products-by-ship-methods (selected-ship-methods)))

  (->> (vfp)
       (map :sm1-3d))

  (->> (vfp)
       (take 5))

  )

