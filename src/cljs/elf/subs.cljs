(ns elf.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::essentials-products
 (fn [db]
   (:essentials-products db)))

(re-frame/reg-sub
 ::all-lead
 (fn [db]
   (:filter-all-lead-times? db)))

(re-frame/reg-sub
 ::quick-ship
 (fn [db]
   (:filter-quick-ship-lead-times? db)))

(re-frame/reg-sub
 ::three-week-ship
 (fn [db]
   (:filter-three-week-ship-lead-times? db)))

(re-frame/reg-sub
 ::standard-ship
 (fn [db]
   (:filter-standard-ship-lead-times? db)))
