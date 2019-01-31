(ns elf.subs
  (:require
   [re-frame.core :refer [reg-sub] :as re-frame]))

(reg-sub
 ::name
 (fn [db]
   (:name db)))

(reg-sub
 ::essentials-products
 (fn [db]
   (:essentials-products db)))

(reg-sub
 ::filtered-products
 (fn [db]
   (:filtered-products db)))

(reg-sub
 ::lead-time-filters
 (fn [db]
   (:lead-time-filters db)))
