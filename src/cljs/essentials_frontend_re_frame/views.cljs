(ns essentials-frontend-re-frame.views
  (:require
   [re-frame.core :as re-frame]
   [essentials-frontend-re-frame.subs :as subscriptions]
   ))

(defn main-panel []
  (let [name (re-frame/subscribe [::subscriptions/name])]
    [:div
     [:h1 "Knoll Essentials Lead Times & Finishes"]
     [:p "(built using the " @name " app framework.)"]]))
