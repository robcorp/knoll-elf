(defproject elf "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [cljs-ajax "0.8.0"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [com.rpl/specter "1.1.2"]
                 [cljsjs/clipboard "2.0.4-0"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}

  :profiles {:dev
             {:dependencies [[binaryage/devtools "0.9.10"]
                             [day8.re-frame/re-frame-10x "0.3.7"]
                             [day8.re-frame/tracing "0.5.1"]
                             [figwheel-sidecar "0.5.18"]
                             [cider/piggieback "0.4.0"]]

              :plugins      [[lein-figwheel "0.5.18"]]}
             :prod { :dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}}

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs"]
                :figwheel     {:on-jsload "elf.core/mount-root"}
                :compiler     {:main                 elf.core
                               :output-to            "resources/public/js/compiled/elf.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :asset-path           "js/compiled/out"
                               :source-map-timestamp true
                               :preloads             [devtools.preload
                                                      day8.re-frame-10x.preload]
                               :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true
                                                      "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                               :external-config      {:devtools/config {:features-to-install :all}}}}

               {:id           "simple"
                :source-paths ["src/cljs"]
                :compiler     {:main            elf.core
                               :output-to       "resources/public/js/compiled/elf.js"
                               :output-dir      "resources/public/js/compiled/simple-out"
                               :optimizations   :simple
                               :closure-defines {goog.DEBUG false}
                               :pretty-print    false}}
               {:id           "prod"
                :source-paths ["src/cljs"]
                :compiler     {:main            elf.core
                               :output-to       "resources/public/js/compiled/elf.js"
                               :output-dir      "resources/public/js/compiled/advanced-out"
                               :optimizations   :advanced
                               :closure-defines {goog.DEBUG false}
                               :externs         ["jquery-1.9-externs.js" "externs.js"]
                               :pretty-print    false}}]})
