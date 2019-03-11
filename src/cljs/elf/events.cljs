(ns elf.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [elf.db :as db]
            [elf.config :as config]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval transform] :as spctr]
            [ajax.core :as ajax]
            [clojure.string :as str]))


(declare load-all-products load-filter-options)

(reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   (load-all-products)
   (load-filter-options "ELFSeatingSelector")
   (load-filter-options "ELFTableSelector")
   (load-filter-options "ELFStorageSelector")
   (load-filter-options "ELFPowerAndDataSelector")
   (load-filter-options "ELFWorkToolsSelector")
   (load-filter-options "ELFScreensAndBoardsSelector")
   db/default-db))

(reg-event-db
 ::use-default-db
 (fn-traced [_ _]
   (.log js/console "Using db/default-db.")
   db/default-db))

(defn- category-products [db products selector]
  (let [selected-filter (selector db)
        category-key (:product-category selected-filter)
        cat-products (filter #(not (empty? (category-key %))) products)
        label (:description selected-filter)
        no-product-filters-selected? (not (some true? (select (multi-path [:ELFSeatingSelector :items ALL :value]
                                                                          [:ELFTableSelector :items ALL :value]
                                                                          [:ELFStorageSelector :items ALL :value]
                                                                          [:ELFPowerAndDataSelector :items ALL :value]
                                                                          [:ELFWorkToolsSelector :items ALL :value]
                                                                          [:ELFScreensAndBoardsSelector :items ALL :value]) db)))
        categories (if no-product-filters-selected?
                     (select [:items ALL :label #(not= "All" %)] selected-filter)
                     (set (select [:items ALL #(true? (:value %)) :label] selected-filter)))]

    (for [category categories]
      {:product-category category
       :label (str label " / " category)
       :products (filter #((set (category-key %)) category) cat-products)})))

(defn- filter-category-products [db products]
  (assoc db
         :filtered-seating-products (category-products db products :ELFSeatingSelector)
         :filtered-table-products (category-products db products :ELFTableSelector)
         :filtered-storage-products (category-products db products :ELFStorageSelector)
         :filtered-power-products (category-products db products :ELFPowerAndDataSelector)
         :filtered-work-products (category-products db products :ELFWorkToolsSelector)
         :filtered-screen-products (category-products db products :ELFScreensAndBoardsSelector)))

(reg-event-db
 ::set-all-products
 (fn-traced [db [_ products]]
   (-> db
       (assoc :loading-all-products false
              :all-products products
              :filtered-products products)
       (filter-category-products products))))

(reg-event-db
 ::set-filter-options
 (fn-traced [db [_ selector resp]]
   (let [selector-key (keyword selector)
         product-category (select-first [selector-key :product-category] db/default-db)
         desc (:description resp)
         items (setval [spctr/BEFORE-ELEM] "All" (:items resp))]
     (assoc db selector-key {:name selector :description desc :product-category product-category :items (mapv (fn [i] {:label i :value false}) items)}))))

#_(reg-event-db
 ::set-filtered-products
 (fn-traced [db [_ products]]
   (assoc db :filtered-products (group-by :product-type products))))


(defn- update-lead-time-filter-state [selected-filter filters]
  (->> filters
       (setval [ALL :value] false)
       (setval [(walker #(= selected-filter (:lead-time %))) :value] true)))

(defn- filter-products-by-lead-times [lead-times prods]
  (if (lead-times "all")
    prods ;; return prods unfiltered
    (select [ALL #(some lead-times (:lead-times %))] prods)))

(reg-event-db
 ::lead-time-filter-radio-button-clicked
 (fn [db [_ lead-time] event]
   (let [updated-lead-time-filters (update-lead-time-filter-state lead-time (:lead-time-filters db))
         selected-lead-times (set (select [ALL #(true? (:value %)) :lead-time] updated-lead-time-filters))
         filtered-products (->> (:all-products db)
                                (filter-products-by-lead-times selected-lead-times))]

     (-> db
         (assoc
          :lead-time-filters updated-lead-time-filters
          :filtered-products filtered-products)
         (filter-category-products filtered-products)))))


(defn- toggle-product-type-filter-state [selected-filter filters]
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
 ::product-type-filter-checkbox-clicked
 (fn-traced [db [_ filter-id] event]
   (let [[selector-str label] (str/split filter-id #":")
         selector (keyword selector-str)
         filters (:items (selector db))
         updated-filters (toggle-product-type-filter-state label filters)
         enable-all? (every? true? (select [ALL #(not= "All" (:label %)) :value] updated-filters))
         updated-db (if enable-all?
                      (setval [selector :items] (setval [ALL #(= "All" (:label %)) :value] true updated-filters) db)
                      (setval [selector :items] updated-filters db))]

     (filter-category-products updated-db (:filtered-products updated-db)))))

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
       (filter-category-products (:filtered-products db)))))

(reg-event-db
 ::product-selected
 (fn-traced [db [_ product-id] event]
   (assoc db :selected-product product-id)))


(defn- load-all-products []
  (let [baseURL (if config/debug?
                  "http://knlprdwcsmgt1.knoll.com"
                  (str (.. js/window -location -origin)))
        all-products-url (str baseURL "/cs/Satellite?pagename=Knoll/Common/Utils/EssentialsPopupProductsJSON")
        ;all-products-url "http://localhost:3449/all-products.json"
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-all-products (:all-products resp)]))
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request to get all-products failed: " status " " status-text))
                        (re-frame/dispatch [::use-default-db]))]

    (ajax/GET all-products-url {:handler success-handler :error-handler error-handler :response-format :json :keywords? true})))

(defn- load-filter-options [selector]
  (let [baseURL (if config/debug?
                  "http://knlprdwcsmgt1.knoll.com"
                  (str (.. js/window -location -origin) ))
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
  (select (multi-path [:filtered-seating-products ALL :products ALL :product-id]
                      [:filtered-table-products ALL :products ALL :product-id]
                      [:filtered-storage-products ALL :products ALL :product-id]
                      [:filtered-power-products ALL :products ALL :product-id]
                      [:filtered-work-products ALL :products ALL :product-id]
                      [:filtered-screen-products ALL :products ALL :product-id]) db))

(defn- next-visible-prod-id [db]
  (let [current-prod (:selected-product db)
        visible-prods (visible-product-ids db)
        i (.indexOf visible-prods current-prod)]
    (visible-prods (min (inc i) (dec (count visible-prods))))))

(defn- previous-visible-prod-id [db]
  (let [current-prod (:selected-product db)
        visible-prods (visible-product-ids db)
        i (.indexOf visible-prods current-prod)]
    (visible-prods (max (dec i) 0))))
