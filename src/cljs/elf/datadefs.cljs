(ns elf.datadefs)

(defonce lead-time-filters
  [{:lead-time "all" :id "all-lead" :label "All Lead Times" :value true :li-id "all" :li-class "all-lead-type"}
   {:lead-time "one-to-three-day" :id "1-3-day-ship" :label "1-3 Day" :value false :li-id "1-3-day-ship-list" :li-class "one-to-three-day-ship"}
   {:lead-time "quick" :id "quick-ship" :label "6-10 Day" :value false :li-id "quickshiplist" :li-class "quick-ship-type"}
   {:lead-time "three-week" :id "three-week-ship" :label "Three-week Ship" :value false :li-id "three-week-ship-list" :li-class "three-week-ship"}
   {:lead-time "std" :id "standard-ship" :label "Standard Ship<br/>(4 to 8+ Weeks)" :value false :li-id "standard-ship-list" :li-class "standard-ship"}])
