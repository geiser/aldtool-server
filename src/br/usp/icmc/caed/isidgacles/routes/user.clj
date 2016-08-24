(ns br.usp.icmc.caed.isidgacles.routes.user
  (:require [br.usp.icmc.caed.isidgacles.layout :as layout]
            ;[br.usp.icmc.caed.isidgacles.util :as util]
            ;[br.usp.icmc.caed.isidgacles.kb.user :as kao]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]))

;(defn user-list []
;  (let [users (hz/list "users")]
;    (layout/render "list.html" {:content users})))

(defn list-page []
  (layout/render "list.html" {:list users}))

(defn form-page [id]
  (let [user (kao/retrieve id)]
    (layout/render "form.html" {:form user})))

(defn about-page []
  (layout/render "about.html"))

;(defn form-page []
;  (html [:div.contenr [:p (util/md->html "/md/paragraph.md")]]))

(defroutes home-routes
  (GET "/list" [] (list-page))
  (GET "/about" [] (about-page))
  (GET "/form/{id}" [:id] (form-page id)))

