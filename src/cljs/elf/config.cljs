(ns elf.config)

(def debug?
  ^boolean goog.DEBUG)

;; When debug? is true, this flag determines whether to use the local all-products.json file or
;; to use http://knlprdwcsmgt1.knoll.com/cs/Satellite?pagename=Knoll/Common/Utils/EssentialsPopupProductsJSON
(def use-local-products? true)

;; base URL for media (images)
(def media-url-base
  (if debug?
    "https://knlprdwcsmgt.knoll.com/media"
    (str (.. js/window -location -origin) "/media")))
