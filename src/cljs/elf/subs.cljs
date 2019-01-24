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
