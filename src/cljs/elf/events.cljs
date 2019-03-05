(ns elf.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [elf.db :as db]
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

(defn- category-products [products selector category-key]
  (let [category-products (filter #(not (empty? (category-key %))) products)
        label (:description selector)
        categories (select [:items ALL :label #(not= "All" %)] selector)]
    (for [category categories]
      {:label (str label " / " category) :products (filter #((set (category-key %)) category) category-products)})))

(reg-event-db
 ::set-all-products
 (fn-traced [db [_ products]]
   (assoc db
          :all-products products
          :filtered-products (group-by :product-type products)
          :filtered-seating-products (category-products products (:ELFSeatingSelector db) :seatingCategories)
          :filtered-table-products (category-products products (:ELFTableSelector db) :tableCategories)
          :filtered-storage-products (category-products products (:ELFStorageSelector db) :storageCategories)
          :filtered-power-products (category-products products (:ELFPowerAndDataSelector db) :powerCategories)
          :filtered-work-products (category-products products (:ELFWorkToolsSelector db) :workToolsCategories)
          :filtered-screen-products (category-products products (:ELFScreensAndBoardsSelector db) :screensCategories))))

(reg-event-db
 ::set-filter-options
 (fn-traced [db [_ selector resp]]
   (let [selector-key (keyword selector)
         desc (:description resp)
         items (setval [spctr/BEFORE-ELEM] "All" (:items resp))]
     (assoc db selector-key {:name selector :description desc :items (mapv (fn [i] {:label i :value false}) items)}))))

(reg-event-db
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
 (fn-traced [db [_ lead-time] event]
   (let [updated-filters (update-lead-time-filter-state lead-time (:lead-time-filters db))
         selected-lead-times (set (select [ALL #(true? (:value %)) :lead-time] updated-filters))
         filtered-products (->> (:all-products db)
                                (filter-products-by-lead-times selected-lead-times))]

     (assoc db
            :lead-time-filters updated-filters
            :filtered-products filtered-products
            :filtered-seating-products (category-products filtered-products (:ELFSeatingSelector db) :seatingCategories)
            :filtered-table-products (category-products filtered-products (:ELFTableSelector db) :tableCategories)
            :filtered-storage-products (category-products filtered-products (:ELFStorageSelector db) :storageCategories)
            :filtered-power-products (category-products filtered-products (:ELFPowerAndDataSelector db) :powerCategories)
            :filtered-work-products (category-products filtered-products (:ELFWorkToolsSelector db) :workToolsCategories)
            :filtered-screen-products (category-products filtered-products (:ELFScreensAndBoardsSelector db) :screensCategories)))))


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
         enable-all? (every? true? (select [ALL #(not= "All" (:label %)) :value] updated-filters))]
     (if enable-all?
       (setval [selector :items] (setval [ALL #(= "All" (:label %)) :value] true updated-filters) db)
       (setval [selector :items] updated-filters db)))))

(reg-event-db
 ::reset-product-type-filters
 (fn-traced [db _]
   (setval (multi-path [:ELFSeatingSelector :items ALL :value]
                       [:ELFTableSelector :items ALL :value]
                       [:ELFStorageSelector :items ALL :value]
                       [:ELFPowerAndDataSelector :items ALL :value]
                       [:ELFWorkToolsSelector :items ALL :value]
                       [:ELFScreensAndBoardsSelector :items ALL :value])
           false db)))

(reg-event-db
 ::product-selected
 (fn-traced [db [_ product-id] event]
   (assoc db :selected-product product-id)))


(defn- load-all-products []
  (let [all-products-url "http://knlprdwcsmgt1.knoll.com/cs/Satellite?pagename=Knoll/Common/Utils/EssentialsPopupProductsJSON"
        ;all-products-url "http://localhost:3449/all-products.json"
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-all-products (:all-products resp)]))
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request to get all-products failed: " status " " status-text))
                        (re-frame/dispatch [::use-default-db]))]
    (ajax/GET all-products-url {:handler success-handler :error-handler error-handler :response-format :json :keywords? true})))

(defn- load-filter-options [selector]
  (let [presentationObjectItemsURL (str "http://knlprdwcsmgt1.knoll.com/cs/Satellite?pagename=Knoll/Common/Utils/PresentationObjectItemsJSON"
                                        "&presentationObject=" selector)
        success-handler (fn [resp]
                          (re-frame/dispatch [::set-filter-options selector (:presentationObjectItems resp)]))
      
        error-handler (fn [{:keys [status status-text]}]
                        (.log js/console (str "Ajax request failed: " status " " status-text)))]
    (ajax/GET presentationObjectItemsURL {:handler success-handler
                                          :error-handler error-handler
                                          :response-format :json
                                          :keywords? true})))
