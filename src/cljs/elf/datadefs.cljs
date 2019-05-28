(ns elf.datadefs)

(defonce lead-time-filters
  [{:lead-time "all" :id "all-lead" :label "All Lead Times" :value true :li-id "all" :li-class "all-lead-type"}
   {:lead-time "quick" :id "quick-ship" :label "QuickShip<br/>(Next Week)" :value false :li-id "quickshiplist" :li-class "quick-ship-type"}
   {:lead-time "three-week" :id "three-week-ship" :label "Three-week Ship" :value false :li-id "three-week-ship-list" :li-class "three-week-ship"}
   {:lead-time "std" :id "standard-ship" :label "Standard Ship<br/>(4 to 8+ Weeks)" :value false :li-id "standard-ship-list" :li-class "standard-ship"}])

