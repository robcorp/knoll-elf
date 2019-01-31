(ns elf.db
  (:require [elf.datadefs :as dd]))

(def default-db
  {:name "re-frame"
   :essentials-products dd/essentials-products
   :filtered-products dd/filtered-products
   :lead-time-filters dd/lead-time-filters})
