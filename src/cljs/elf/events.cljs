(ns elf.events
  (:require
   [re-frame.core :as re-frame]
   [elf.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(defn toggle-filter-state [db filter]
  (assoc db filter (not (filter db))))

(re-frame/reg-event-db
 ::check-box-clicked
 (fn [db event]
   (let [[_ id] event]
     #_(println id "checkbox was clicked.")
     (cond
       (= id "all-lead") (toggle-filter-state db :filter-all-lead-times?)
       (= id "standard-ship") (toggle-filter-state db :filter-standard-ship-lead-times?)
       (= id "three-week-ship") (toggle-filter-state db :filter-three-week-ship-lead-times?)
       (= id "quick-ship") (toggle-filter-state db :filter-quick-ship-lead-times?)))))
