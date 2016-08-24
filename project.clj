(defproject isidgacles "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [selmer "0.8.2"]
                 [com.taoensso/timbre "3.4.0"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.66"]
                 [environ "1.0.0"]
                 [json-html "0.3.1"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-session-timeout "0.1.0"]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.2"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [liberator "0.13"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [br.usp.icmc.caed/cljhozo "0.1.0-SNAPSHOT"]
                 [formative "0.8.8"]
                 [ring-server "0.4.0"]
                 
                 [jumblerg/ring.middleware.cors "1.0.1"]

                 [jp.hozo/hozo-api "1.0.11"]]
;  :resource-paths ["lib/*"]
;  :resource-paths ["checkouts/clj-hozo/lib/*"]
  :min-lein-version "2.0.0"
  :uberjar-name "isidgacles.jar"
  :jvm-opts ["-server"]
;;enable to start the nREPL server when the application launches
;:env {:repl-port 7001}
  :main br.usp.icmc.caed.isidgacles.core
  :plugins [[lein-ring "0.9.1"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.5"]
            [lein-cljsbuild "1.0.4"]]
  
  :cljsbuild {:builds
              {:app {:source-paths ["src-cljs"]
                     :compiler {:output-to "resources/public/js/app.js"
                                :output-dir "resources/public/js/out"
                                :source-map "resources/public/js/out.js.map"
                                :externs ["react/externs/react.js"]
                                :optimizations :none
                                :pretty-print  true}}}}
  
  :ring {:handler isidgacles.handler/app
         :init    isidgacles.handler/init
         :destroy isidgacles.handler/destroy
         :uberwar-name "isidgacles.war"}

  :profiles {:uberjar {:omit-source true
                       :env {:production true}
                       :aot :all
                       :hooks ['leiningen.cljsbuild] ;; add for clsj
                       :cljsbuild {:jar true
                                   :builds
                                   {:app {:compiler {:optimizations :advanced
                                                     :pretty-print false}}}}}

  :dev {:dependencies [[ring-mock "0.1.5"]
                       [ring/ring-devel "1.3.2"]
                       [pjstadig/humane-test-output "0.7.0"]]
        :repl-options {:init-ns br.usp.icmc.caed.isidgacles.core}
        :injections [(require 'pjstadig.humane-test-output)
                     (pjstadig.humane-test-output/activate!)]
        :env {:dev true}}})

