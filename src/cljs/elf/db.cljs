(ns elf.db
  (:require [elf.datadefs :as dd]))

(def default-db
  {:name "re-frame"
   :essentials-products dd/essentials-products
   :filter-all-lead-times? true
   :filter-quick-ship-lead-times? false
   :filter-three-week-ship-lead-times? false
   :filter-standard-ship-lead-times? false})
