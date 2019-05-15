(ns elf.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub] :as re-frame]
   [com.rpl.specter :refer [ALL multi-path walker] :refer-macros [select select-first setval] :as spctr]))

(reg-sub ::name
         :name)
;; keywords are unary functions, so no need to wrap them in an
;; anonymous function so (reg-sub ::name :name) is essentially the
;; same as (reg-sub ::name (fn [db] (:name db)))

(reg-sub ::all-products
         :all-products)

(reg-sub ::filtered-products
         :filtered-products)

(reg-sub ::filtered-seating-products
         :filtered-seating-products)

(reg-sub ::filtered-table-products
         :filtered-table-products )

(reg-sub ::filtered-storage-products
         :filtered-storage-products)

(reg-sub ::filtered-power-products
         :filtered-power-products)

(reg-sub ::filtered-work-products
         :filtered-work-products)

(reg-sub ::filtered-screen-products
         :filtered-screen-products)

(reg-sub ::visible-filtered-products
 (fn [_]
   [(re-frame/subscribe [::filtered-seating-products])
    (re-frame/subscribe [::filtered-table-products])
    (re-frame/subscribe [::filtered-storage-products])
    (re-frame/subscribe [::filtered-power-products])
    (re-frame/subscribe [::filtered-work-products])
    (re-frame/subscribe [::filtered-screen-products])])

 (fn [all-visible-prods]
   (select [ALL ALL :products ALL] all-visible-prods)))

(reg-sub ::textiles-info
         :textiles-info )

(reg-sub ::textiles-approvals
         :textiles-approvals )

(reg-sub ::selected-product
 (fn [db]
   (let [[_ selected-epp-id] (:selected-epp-id db)]
     (select-first [:all-products ALL #(= selected-epp-id (:epp-id %))] db))))

(reg-sub ::selected-product-all-textiles
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

(reg-sub ::selected-product-essential-textiles
 (fn [_]
   (re-frame/subscribe [::selected-product-all-textiles]))

 (fn [textiles]
   (group-by :Grade (select [spctr/MAP-VALS ALL #(not-empty (:EssntlSKUs %))] textiles))))

(reg-sub ::lead-time-filters
         :lead-time-filters)

(reg-sub ::seating-filter-options
         :ELFSeatingSelector)

(reg-sub ::tables-filter-options
         :ELFTableSelector)

(reg-sub ::storage-filter-options
         :ELFStorageSelector)

(reg-sub ::power-data-filter-options
         :ELFPowerAndDataSelector)

(reg-sub ::work-tools-filter-options
         :ELFWorkToolsSelector)

(reg-sub ::screen-board-filter-options
         :ELFScreensAndBoardsSelector)
