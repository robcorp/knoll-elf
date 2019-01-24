(ns elf.views
  (:require
   [re-frame.core :as re-frame]
   [elf.subs :as subs]
   ))


(defn essential-product-summary [{:keys [id name]} product]
  ^{:key id}
  [:p.essential-product-summary {:id id} name])


(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])
        essentials-products (re-frame/subscribe [::subs/essentials-products])]
    #_(println essentials-products)
    [:div
     [:h1 "Knoll Essentials Lead Times & Finishes"]
     [:p "(built using the " @name " app framework.)"]
     [:hr]
     [:div.essential-product-summaries
      (map essential-product-summary @essentials-products)]]))
