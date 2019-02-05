(ns elf.db
  (:require [elf.datadefs :as dd]))

(def default-db
  {:name "re-frame"
   :all-products dd/all-products
   :filtered-products dd/filtered-products
   :selected-product (:product-id (first dd/all-products))
   :lead-time-filters dd/lead-time-filters})
