(ns elf.config)

(def debug?
  ^boolean goog.DEBUG)

;; When debug? is true, this flag determines whether to use the local all-products.json file or
;; to use http://knlprdwcsmgt1.knoll.com/cs/Satellite?pagename=Knoll/Common/Utils/EssentialsPopupProductsJSON
(def use-local-products? true)
