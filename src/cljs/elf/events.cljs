(ns elf.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [elf.db :as db]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval] :as spctr]
            [ajax.core :as ajax]))

(reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            (let [all-products-url "http://127.0.0.1:7070/571268536.060299" ;; this will change each time Smart JSON Editor is launched
                  success-handler (fn [resp]
                                    (let [product-id (:product-id (first resp))]
                                      (re-frame/dispatch [::set-all-products resp])))
                  error-handler (fn [{:keys [status status-text]}]
                                  (.log js/console (str "Ajax request to get all-products failed: " status " " status-text))
                                  (re-frame/dispatch [::use-default-db]))]
              (ajax/GET all-products-url {:handler success-handler :error-handler error-handler :response-format :json :keywords? true})
     
              db/default-db)))

(reg-event-db
 ::use-default-db
 (fn-traced [_ _]
   (.log js/console "Using db/default-db.")
   db/default-db))

(reg-event-db
 ::set-all-products
 (fn-traced [db [_ products]]
   (assoc db :all-products products)))

(reg-event-db
 ::set-filtered-products
 (fn-traced [db [_ products]]
   (assoc db :filtered-products (group-by :product-type products))))

(defn- lead-time-filter-value [lead-time lead-time-filters]
  (select-first [ALL #(= (:lead-time %) lead-time) :value] lead-time-filters))

(defn- toggle-lead-time-filter-state [selected-filter filters]
  (let [selected-filter-value (lead-time-filter-value selected-filter filters)
        all-value (lead-time-filter-value "all" filters)]

    (case selected-filter
      "all" (setval [ALL :value] (not all-value) filters)

      ;; default
      (if (and all-value selected-filter-value)
        ;; then
        (setval [(multi-path
                  (walker #(= "all" (:lead-time %)))
                  (walker #(= selected-filter (:lead-time %)))) :value] false filters)
        ;; else
        (setval [(walker #(= selected-filter (:lead-time %))) :value] (not selected-filter-value) filters)))))

(defn- filter-products-by-lead-times [lead-times prods]
  (if (empty? lead-times)
    prods ;; return prods unfiltered
    (select [ALL #(some lead-times (:lead-times %))] prods)))

(reg-event-db
 ::lead-time-filter-check-box-clicked
 (fn-traced [db [_ lead-time] event]
   (let [updated-filters (toggle-lead-time-filter-state lead-time (:lead-time-filters db))
         enable-all? (every? true? (select [ALL #(#{"std" "quick" "three-week"} (:lead-time %)) :value] updated-filters))
         selected-filters (set (select [ALL #(true? (:value %)) :lead-time] updated-filters))
         filtered-products (->> (:all-products db)
                                (filter-products-by-lead-times selected-filters)
                                (group-by :product-type))]

     (assoc db
            :lead-time-filters (if enable-all?
                                 (setval [(walker #(= "all" (:lead-time %))) :value] true updated-filters)
                                 updated-filters)
            
            :filtered-products filtered-products))))

(reg-event-db
 ::product-selected
 (fn-traced [db [_ product-id] event]
   (assoc  db :selected-product product-id)))



#_(let [presentationObjectItemsURL (str "http://knlprdwcsmgt1.knoll.com/cs/Satellite?pagename=Knoll/Common/Utils/PresentationObjectItemsJSON"
                                      "&presentationObject=ELFLeadTimeSelector")
      success-handler (fn [resp]
                        (println "ajax resp: " + resp)
                        (println "(type resp) = " (type resp))
                        (println "name: " (-> resp
                                              :presentationObjectItems
                                              :items)))
      
      error-handler (fn [{:keys [status status-text]}]
                      (.log js/console (str "Ajax request failed: " status " " status-text)))]
  (ajax/GET presentationObjectItemsURL {:handler success-handler
                                        :error-handler error-handler
                                        :response-format :json
                                        :keywords? true}))
