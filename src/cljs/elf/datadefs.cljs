(ns elf.datadefs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:require-macros [cljs.spec.test.alpha :as st]))

(comment
  (s/def :lead-time #{:std :quick :three-week})
  (s/def ::lead-times (s/coll-of ::lead-time :distinct true :min-count 1 :max-count 3))
  (s/def ::ProductTypeFamily #{"Seating" "Tables" "Storage" "Power & Data" "Work Tools & Accessories" "Screens & Communications Boards"})

  (s/def ::product-id keyword?)
  (s/def ::product-name string?)
  (s/def ::product (s/keys :req [::product-id ::product-name ::lead-times]))
  (s/def ::products (s/coll-of ::product)))


(def essentials-products
  [{:product-id :barcelona-chair, :product-name "Barcelona Chair" :lead-times [:std]}
   {:product-id :pollock-chair, :product-name "Pollock Chair" :lead-times [:quick]}
   {:product-id :wassily-chair, :product-name "Wassily Chair" :lead-times [:three-week]}
   ])
