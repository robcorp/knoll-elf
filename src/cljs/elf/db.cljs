(ns elf.db)

(def essentials-products
  [
   {:id :barcelona-chair, :name "Barcelona Chair"}
   {:id :pollock-chair, :name "Pollock Chair"}
   {:id :wassily-chair, :name "Wassily Chair"}
   ])

(def default-db
  {:name "re-frame"
   :essentials-products essentials-products})
