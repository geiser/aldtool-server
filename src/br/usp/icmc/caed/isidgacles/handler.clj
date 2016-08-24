(ns br.usp.icmc.caed.isidgacles.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [br.usp.icmc.caed.isidgacles.routes.home :refer [home-routes]]

            [ring.middleware.cors :refer [wrap-cors]]
            
            [br.usp.icmc.caed.isidgacles.middleware :as middleware]
            [br.usp.icmc.caed.isidgacles.session :as session]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [liberator.dev :refer [wrap-trace]]
            [clojure.tools.nrepl.server :as nrepl]))

(defonce nrepl-server (atom nil))

(defroutes base-routes
           (route/resources "/")
           (route/not-found "Not Found"))

(defn start-nrepl
  "Start a network repl for debugging when the :repl-port is set in the environment."
  []
  (when-let [port (env :repl-port)]
    (try
      (reset! nrepl-server (nrepl/start-server :port port))
      (timbre/info "nREPL server started on port" port)
      (catch Throwable t
        (timbre/error "failed to start nREPL" t)))))

(defn stop-nrepl []
  (when-let [server @nrepl-server]
    (nrepl/stop-server server)))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []

  (timbre/set-config!
    [:appenders :rotor]
    {:min-level             (if (env :dev) :trace :info)
     :enabled?              true
     :async?                false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn                    rotor/appender-fn})

  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "br/usp/icmc/caed/isidgacles.log" :max-size (* 512 1024) :backlog 10})

  (if (env :dev) (parser/cache-off!))
  (start-nrepl)
  ;;start the expired session cleanup job
  (session/start-cleanup-job!)
  (timbre/info "\n-=[ isidgacles started successfully"
               (when (env :dev) "using the development profile") "]=-"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "isidgacles is shutting down...")
  (stop-nrepl)
  (timbre/info "shutdown complete!"))

(def app-routes
  (routes
    ;(wrap-trace #'home-routes :header :ui)
    ;(wrap-cors home-routes #".*")
    home-routes
    base-routes))

(def app (wrap-cors (middleware/wrap-base #'app-routes) #".*"))

;(def app
;  (-> (routes
;        (wrap-routes home-routes middleware/wrap-csrf)
;        base-routes)
;      middleware/wrap-base))

