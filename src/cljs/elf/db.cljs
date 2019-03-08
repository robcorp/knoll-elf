(ns elf.db
  (:require [elf.datadefs :as dd]))

(def default-db
  {:name "Knoll Essentials Lead Times & Finishes"
   :all-products []
   :filtered-products nil
   :filtered-seating-products {}
   :filtered-table-products {}
   :filtered-storage-products {}
   :filtered-power-products {}
   :filtered-work-products {}
   :filtered-screen-products {}
   :selected-product nil
   :lead-time-filters dd/lead-time-filters
   :ELFSeatingSelector {:description "Seating"
                        :product-category :seatingCategories
                        :items []}
   :ELFTableSelector {:description "Tables"
                      :product-category :tableCategories
                      :items []}
   :ELFStorageSelector {:description "Storage"
                        :product-category :storageCategories
                        :items []}
   :ELFPowerAndDataSelector {:description "Power & Data"
                             :product-category :powerCategories
                             :items []}
   :ELFWorkToolsSelector {:description "Work Tools & Accessories"
                          :product-category :workToolsCategories
                          :items []}
   :ELFScreensAndBoardsSelector {:description "Screen & Communication Boards"
                                 :product-category :screensCategories
                                 :items []}})
