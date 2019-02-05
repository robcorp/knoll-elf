(ns elf.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [elf.db :as db]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval] :as spctr]))

(reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(defn- lead-time-filter-value [lead-time lead-time-filters]
  (select-first [ALL #(= (:lead-time %) lead-time) :value] lead-time-filters))

(defn- toggle-lead-time-filter-state [selected-filter filters]
  (let [selected-filter-value (lead-time-filter-value selected-filter filters)
        all-value (lead-time-filter-value :all filters)]
    (case selected-filter
      :all (setval [ALL :value] (not all-value) filters)

      ;; default
      (if (and all-value selected-filter-value)
        ;; then
        (setval [(multi-path
                  (walker #(= :all (:lead-time %)))
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
         enable-all? (every? true? (select [ALL #((:lead-time %) #{:std :quick :three-week}) :value] updated-filters))
         selected-filters (set (select [ALL #(true? (:value %)) :lead-time] updated-filters))
         filtered-products (->> (:all-products db)
                                (filter-products-by-lead-times selected-filters)
                                (group-by :product-type))]

     (assoc db
            :lead-time-filters (if enable-all?
                                 (setval [(walker #(= :all (:lead-time %))) :value] true updated-filters)
                                 updated-filters)
            
            :filtered-products filtered-products))))

(reg-event-db
 ::product-selected
 (fn-traced [db [_ product-id] event]
   (assoc  db :selected-product product-id)))

