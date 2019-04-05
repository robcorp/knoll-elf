(ns elf.subs
  (:require
   [clojure.string :as str]
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
 ::textiles-info
 (fn [db]
   (:textiles-info db)))

(reg-sub
 ::textiles-approvals
 (fn [db]
   (:textiles-approvals db)))

(reg-sub
 ::selected-product
 (fn [db]
   (let [[_ selected-epp-id] (:selected-epp-id db)]
     (select-first [:all-products ALL #(= selected-epp-id (:epp-id %))] db))))

(reg-sub
 ::selected-product-all-textiles
 (fn [_]
   [(re-frame/subscribe [::selected-product])
    (re-frame/subscribe [::textiles-info])
    (re-frame/subscribe [::textiles-approvals])])

 (fn [[selected-prod info approvals]]
   (let [partnums (->> selected-prod
                       :apprvId
                       keyword
                       (get approvals)
                       (filter not-empty)
                       (filter #(not-empty (str/replace % #"[a-zA-Z]*" ""))))
         get-textiles-info (fn [partnum]
                             (select-first [ALL #(= (:PartNum %) partnum)] info))
         textiles (group-by :Grade (map get-textiles-info partnums))]

     (filter #(not-empty (key %)) textiles))))

(reg-sub
 ::selected-product-essential-textiles
 (fn [_]
   (re-frame/subscribe [::selected-product-all-textiles]))

 (fn [textiles]
   (group-by :Grade (select [spctr/MAP-VALS ALL #(not-empty (:EssntlSKUs %))] textiles))))

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
