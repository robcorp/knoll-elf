(ns elf.db
  (:require [elf.datadefs :as dd]
            [cljs.reader :as rdr]))

(def default-db
  {:name "Knoll Essentials Lead Times & Finishes"
   :loading-all-products true
   :all-products (rdr/read-string (.getItem js/localStorage "all-products"))
   :filtered-products (rdr/read-string (.getItem js/localStorage "all-products"))
   :filtered-seating-products {}
   :filtered-table-products {}
   :filtered-storage-products {}
   :filtered-power-products {}
   :filtered-work-products {}
   :filtered-screen-products {}
   :selected-epp-id nil
   :lead-time-filters dd/lead-time-filters
   :ELFSeatingSelector (if-let [from-local-storage (rdr/read-string (.getItem js.localStorage "ELFSeatingSelector"))]
                         from-local-storage
                         {:name "ELFSeatingSelector"
                          :description "Seating-"
                          :product-category :seatingCats
                          :items [{:label "All" :value false}
                                  {:label "Work" :value false}
                                  {:label "Side and Multi-Use" :value false}
                                  {:label "Lounge" :value false}
                                  {:label "Stools" :value false}
                                  {:label "Outdoor" :value false}]})
   :ELFTableSelector (if-let [from-local-storage (rdr/read-string (.getItem js.localStorage "ELFTableSelector"))]
                       from-local-storage
                       {:name "ELFTableSelector"
                        :description "Tables-"
                        :product-category :tableCats
                        :items [{:label "All" :value false}
                                {:label "Meeting & Conference" :value false}
                                {:label "Desks & Benching" :value false}
                                {:label "Height-Adjustable" :value false}
                                {:label "Dining & Multi-use" :value false}
                                {:label "Training" :value false}
                                {:label "Side & Coffee" :value false}
                                {:label "Outdoor" :value false}]})
   :ELFStorageSelector (if-let [from-local-storage (rdr/read-string (.getItem js.localStorage "ELFStorageSelector"))]
                         from-local-storage
                         {:name "ELFStorageSelector"
                          :description "Storage-"
                          :product-category :storageCats
                          :items [{:label "All" :value false}
                                  {:label "Pedestals" :value false}
                                  {:label "Doublewide Pedestals" :value false}
                                  {:label "Credenzas Carts and Consoles" :value false}
                                  {:label "Lockers and Wardrobes" :value false}
                                  {:label "Towers" :value false}
                                  {:label "Mounted Storage" :value false}]})
   :ELFPowerAndDataSelector (if-let [from-local-storage (rdr/read-string (.getItem js.localStorage "ELFPowerAndDataSelector"))]
                              from-local-storage
                              {:name "ELFPowerAndDataSelector"
                               :description "Enhanced Power And Data-"
                               :product-category :powerCats
                               :items [{:label "All" :value false}
                                       {:label "Power & Technology Distribution" :value false}
                                       {:label "Interior Architecture" :value false}]})
   :ELFWorkToolsSelector (if-let [from-local-storage (rdr/read-string (.getItem js.localStorage "ELFWorkToolsSelector"))]
                           from-local-storage
                           {:name "ELFWorkToolsSelector"
                            :description "Accessories & Work Tools-"
                            :product-category :workCats
                            :items [{:label "All" :value false}
                                    {:label "Lighting" :value false}
                                    {:label "Technology Support" :value false}
                                    {:label "Desktop Management" :value false}]})
   :ELFScreensAndBoardsSelector (if-let [from-local-storage (rdr/read-string (.getItem js.localStorage "ELFScreensAndBoardsSelector"))]
                                  from-local-storage
                                  {:name "ELFScreensAndBoardsSelector"
                                   :description "Screens & Communication Boards-"
                                   :product-category :screensCats
                                   :items [{:label "All" :value false}
                                           {:label "Screens & Dividers" :value false}
                                           {:label "Communication Boards" :value false}]})})
