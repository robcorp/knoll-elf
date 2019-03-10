(ns elf.subs
  (:require
   [re-frame.core :refer [reg-sub] :as re-frame]
   [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval] :as spctr]))

(reg-sub
 ::name
 (fn [db]
   (:name db)))

(reg-sub
 ::loading-all-products
 (fn [db]
   (:loading-all-products db)))

(reg-sub
 ::all-products
 (fn [db]
   (:all-products db)))

(reg-sub
 ::filtered-products
 (fn [db]
   (:filtered-products db)))

(reg-sub
 ::filtered-seating-products
 (fn [db]
   (:filtered-seating-products db)))

(reg-sub
 ::filtered-table-products
 (fn [db]
   (:filtered-table-products db)))

(reg-sub
 ::filtered-storage-products
 (fn [db]
   (:filtered-storage-products db)))

(reg-sub
 ::filtered-power-products
 (fn [db]
   (:filtered-power-products db)))

(reg-sub
 ::filtered-work-products
 (fn [db]
   (:filtered-work-products db)))

(reg-sub
 ::filtered-screen-products
 (fn [db]
   (:filtered-screen-products db)))

(reg-sub
 ::selected-product
 (fn [db]
   (let [selected-product-id (:selected-product db)]
     (select-first [:all-products ALL #(= selected-product-id (:product-id %))] db))))

(reg-sub
 ::lead-time-filters
 (fn [db]
   (:lead-time-filters db)))

(reg-sub
 ::seating-filter-options
 (fn [db]
   (:ELFSeatingSelector db)))

(reg-sub
 ::tables-filter-options
 (fn [db]
   (:ELFTableSelector db)))

(reg-sub
 ::storage-filter-options
 (fn [db]
   (:ELFStorageSelector db)))

(reg-sub
 ::power-data-filter-options
 (fn [db]
   (:ELFPowerAndDataSelector db)))

(reg-sub
 ::work-tools-filter-options
 (fn [db]
   (:ELFWorkToolsSelector db)))

(reg-sub
 ::screen-board-filter-options
 (fn [db]
   (:ELFScreensAndBoardsSelector db)))
