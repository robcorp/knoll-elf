(ns elf.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [elf.db :as db]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [com.rpl.specter :refer [ALL FIRST select transform] :as spctr]))

(reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(defn- lead-time-filter-value [lead-time lead-time-filters]
  (spctr/select-first [ALL #(= (:lead-time %) lead-time) :value] lead-time-filters))

(defn- toggle-lead-time-filter-state [selected-filter filters]
  (let [selected-filter-value (lead-time-filter-value selected-filter filters)
        all-value (lead-time-filter-value :all filters)]
    (case selected-filter
      :all (spctr/setval [ALL :value] (not all-value) filters)

      ;; default
      (if (and all-value selected-filter-value)
        ;; then
        (spctr/setval [(spctr/multi-path
                        (spctr/walker #(= :all (:lead-time %)))
                        (spctr/walker #(= selected-filter (:lead-time %)))) :value] false filters)
        ;; else
        (spctr/setval [(spctr/walker #(= selected-filter (:lead-time %))) :value] (not selected-filter-value) filters)))))

(defn- filter-products-by-lead-times [lead-times prods]
  (if (empty? lead-times)
    prods ;; return prods unfiltered
    (select [ALL #(some lead-times (:lead-times %))] prods)))

(reg-event-db
 ::lead-time-filter-check-box-clicked
 (fn [db [_ lead-time] event]
   (let [updated-filters (toggle-lead-time-filter-state lead-time (:lead-time-filters db))
         enable-all? (every? true? (select [ALL #((:lead-time %) #{:std :quick :three-week}) :value] updated-filters))
         selected-filters (set (select [ALL #(true? (:value %)) :lead-time] updated-filters))
         filtered-products (->> (:essentials-products db)
                                (filter-products-by-lead-times selected-filters)
                                (group-by :product-type))]
     #_(println selected-filters)
     #_(println filtered-products)
     (assoc db
            :lead-time-filters
            (if enable-all?
              (spctr/setval [(spctr/walker #(= :all (:lead-time %))) :value] true updated-filters)
              updated-filters)
            
            :filtered-products filtered-products))))
