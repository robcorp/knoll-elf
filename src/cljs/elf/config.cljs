(ns elf.config)

(def debug?
  ^boolean goog.DEBUG)

;; When debug? is true, this flag determines whether to use the local all-products.json file or
;; to use http://knlprdwcsmgt1.knoll.com/cs/Satellite?pagename=Knoll/Common/Utils/EssentialsPopupProductsJSON
(def use-local-products? false)

;; base URL for media (images)
(def media-url-base
  (if debug?
    #_"http://knldev2wcsapp1a.knoll.com/media" "https://knlprdwcsmgt.knoll.com/media"
    (str (.. js/window -location -origin) "/media")))
