(ns elf.subs
  (:require
   [re-frame.core :refer [reg-sub] :as re-frame]
   [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval] :as spctr]))

(reg-sub
 ::name
 (fn [db]
   (:name db)))

(reg-sub
 ::all-products
 (fn [db]
   (:all-products db)))

(reg-sub
 ::filtered-products
 (fn [db]
   (:filtered-products db)))

(reg-sub
 ::selected-product
 (fn [db]
   (let [selected-product-id (:selected-product db)]
     (select-first [:all-products ALL #(= selected-product-id (:product-id %))] db))))

(reg-sub
 ::lead-time-filters
 (fn [db]
   (:lead-time-filters db)))
