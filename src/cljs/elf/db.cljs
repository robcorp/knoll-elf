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
                        :items []}
   :ELFTableSelector {:description "Tables"
                      :items []}
   :ELFStorageSelector {:description "Storage"
                        :items []}
   :ELFPowerAndDataSelector {:description "Power & Data"
                             :items []}
   :ELFWorkToolsSelector {:description "Work Tools & Accessories"
                          :items []}
   :ELFScreensAndBoardsSelector {:description "Screen & Communication Boards"
                                 :items []}})
