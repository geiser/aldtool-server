(ns br.usp.icmc.caed.isidgacles.routes.home
  (:require [br.usp.icmc.caed.isidgacles.layout :as layout]
            [compojure.core :refer [defroutes GET ANY]]
            [hiccup.core :as h]
            [formative.core :as f]
            [json-html.core :as j]
            [br.usp.icmc.caed.cljhozo.core :as hz]
            [br.usp.icmc.caed.cljhozo.values :as hz-v]
            [ring.util.http-response :refer [ok]]
            [clojure.string :refer [trim]]
            [liberator.core :refer [resource defresource]]
            [liberator.representation :refer [ring-response]]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import [jp.hozo.core HozoObject]))

(defn- check-arg [pred [f & r :as a]] (if (pred f) [f r] [nil a]))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"))

(def hz-human
  (hz/retrieve "resources/ontologies/site.xml"
                           {:search {:label "Human" :type :concept}
                            :only-one true}))

(def hz-human-1
  (hz/retrieve "resources/ontologies/site-ins.xml"
                           {:search {:label "Human_1"}
                            :only-one true}))

(def hz-human-2
  (hz/retrieve "resources/ontologies/site-ins.xml"
                           {:search {:label "Human_2"}
                            :only-one true}))

(def example-form
    {:fields [{:name :secret-code :type :hidden :datatype :int}
              {:name :email :type :email}
              {:name :password :type :password}
              {:name :remember :type :checkbox}]
     :validations [[:required [:secret-code :email :password]]
                   [:min-length 8 :password]]
     :values {:secret-code 1234
              :remember true}})

;(defn- constraint->field
;  [constr f-name]
;  (let [[f-type f-datatype]
;        (condp #(isa? hz-v/h-types %2 %1)
;          (get-in constr [:concept :type])
;          :value (condp #(= %2 %1)
;                   (get-in constr [:concept :label])
;                   "boolean" [:checkbox :boolean]
;                   "date" [:date-select :date]
;                   "decimal" [:text :decimal]
;                   "float" [:text :float]
;                   "integer" [:text :int]
;                   "number" [:text :long]
;                   "string" [:text :str]
;                   "time" [:time-select :time]
;                   "uri" [:text :str])
;          :concept [:select nil])
        ;f-options (if (= f-type :select) (vec (map hz-model-object->option (retrieve {:search instancias-subinstancias-one-concepts (get-in slot [:constraint :concept])}))))
;        f-options  nil]
;    [(->> {:name f-name :label (get-in slot [:role :label])
;           :type f-type :datatype f-datatype :options f-options}
;          (filter second) (into {})) nil]))

(defn- concept->form
  [concept & [spec]]
  (merge
    (if-not (nil? spec) spec {})
    (loop [[slot & r-slots] (:slots concept) result {}]
      (if (nil? slot) result
        (recur
          r-slots
          (loop [n 0 result {}]
            (if (= n (get-in slot [:cardinality :min])) result
              (let [name (keyword (str (:id slot ":" n)))
                    [field value] [nil nil]
                    ;;(constraint->field (:constraint slot) name)
                    fields (conj (get result :fields []) field)
                    values (if (nil? value)
                             (get result :values {})
                             (assoc (get result :values {}) name value))]
                (recur (+ n 1) {:fields fields :values values})))))))))

;(defn form-test-page []
;  (layout/render "home.html"
;                 {:docs (h/html  (f/render (concept->form hz-human)))}))

(defn retrieve-test-ont []
  (h/html [:head [:style (-> "json.human.css" clojure.java.io/resource slurp)]]
  (j/edn->html hz-human)
  (j/edn->html (meta hz-human))))

(defn retrieve-test-mdl [id]
  (let [hz-human (if (= id "1") hz-human-1 hz-human-2)]
    (h/html [:head [:style (-> "json.human.css" clojure.java.io/resource slurp)]]
            (j/edn->html hz-human)
            (j/edn->html (meta hz-human)))))

(defn aware-value-writer [key value]
  (cond
  	(= key :class) (.toString value)
  	(= key :object) (.toString value)
  	:else value))

;;;;;;;;;;;;;;;;;;;;;

(def ont (hz/load-ontology "resources/ontologies/OntoGaCLeS.xml"))
(def mdl (hz/load-model "resources/ontologies/OntoGaCLeS-ins.xml"))

(def hz-resources (ref {}))

(defn- hz-print-result
  ([ctx format callback result]
      (hz-print-result ctx format callback result false))
  ([ctx format callback result include-meta]
    (let [r (if-not (seq? result)
              (if include-meta {:data result :meta (meta result)} result)
              (map #(if include-meta {:data % :meta (meta %)} %) result))]
      (cond (= "json" format) (json/write-str r :value-fn aware-value-writer)
            (= "jsonp" format) (str callback "(" (json/write-str r) ")")
            :else (h/html [:head [:style (-> "json.human.css"
                                             clojure.java.io/resource slurp)]]
                                             (j/edn->html r))))))

(def hz-base-resource
  {:available-media-types ["text/javascript", "application/javascript",
                          "application/json", "text/plain", "text/html"]
   :malformed? (fn [ctx]
                 (let [format (get-in ctx [:request :params :format])]
                   (if-not (nil? format)
                     (not (.contains ["jsonp" "json"] format)) false)))})

(defresource ont-tree-resource [ctype cid]
  hz-base-resource
  :allowed-methods [:get] ; [:get :post]
  :handle-ok
  (fn [ctx]
    (let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          ctype (keyword ctype)
          include-meta (boolean (get-in ctx [:request :params :meta]))
          result (hz/retrieve ont {:type ctype
                                   :upper-concept {:id (if (nil? cid) :root cid)
                                                   :type ctype}})]
      (hz-print-result ctx format callback result include-meta))))

(defresource ont-list-resource [ctype]
  hz-base-resource
  :allowed-methods [:get]  ; [:get :post]
  :handle-ok
  (fn [ctx]
  	(let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          ctype (keyword ctype)
          include-meta (boolean (get-in ctx [:request :params :meta]))
          params (get-in ctx [:request :params])
          result (hz/retrieve ont (merge {:type ctype}
          	                             (if-not (nil? (:label params))
          	                             	{:label (:label params)})))]
      (hz-print-result ctx format callback result include-meta))))

(defresource ont-entry-resource [ctype cid]
  hz-base-resource
  :allowed-methods [:get] ; [:get :put delete]
  :handle-ok
  (fn [ctx]
    (let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          ctype (keyword ctype)
          include-meta (boolean (get-in ctx [:request :params :meta]))
          result (hz/retrieve ont {:id cid :type ctype} true true)]
      (hz-print-result ctx format callback result include-meta))))

(defresource mdl-dep-list-resource [ctype cid]
  hz-base-resource
  :new? (fn [ctx] (nil? (get-in ctx [:request :body-params :id])))
  :respond-with-entity? true
  :allowed-methods [:get :post] ; [:get :post]
  :post!
  (fn [ctx]
    (dosync
      (alter hz-resources assoc-in [:mdl-dep-list-resource :new]
             (hz/create
               mdl
               (clojure.walk/keywordize-keys
                 (merge (get-in ctx [:request :body-params])
                        {:ontology-object {:id cid :type (keyword ctype)}}))
               true true)))
    (hz/save-model mdl))
  :handle-created
  (fn [ctx]
    (let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          include-meta (boolean (get-in ctx [:request :params :meta]))
          result (get-in @hz-resources [:mdl-dep-list-resource :new])]
      (hz-print-result ctx format callback result include-meta)))
  :handle-ok
  (fn [ctx]
    (let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          ctype (keyword ctype)
          include-meta (boolean (get-in ctx [:request :params :meta]))
          result (hz/retrieve mdl {:ontology-object {:id cid :type ctype}}
                              false false)]
      (hz-print-result ctx format callback result include-meta))))

(defresource mdl-dep-entry-resource [ctype cid id]
  hz-base-resource
  :new? false ; (fn [ctx] (nil? (get-in ctx [:request :body-params :id])))
  :respond-with-entity? true
  :allowed-methods [:get :put :delete] ; [:get :put :delete]
  :put!
  (fn [ctx]
    (dosync
      (alter hz-resources assoc-in [:mdl-dep-entry-resource :update]
             (hz/hz-update
               mdl
               (clojure.walk/keywordize-keys
                 (merge (get-in ctx [:request :body-params])
                        {:ontology-object {:id cid :type (keyword ctype)}}))
               true)))
    (hz/save-model mdl))
  :delete!
  (fn [ctx]
      (hz/delete mdl {:id id
                      :ontology-object {:id cid :type (keyword ctype)}} true)
      (hz/save-model mdl))
    :handle-ok
    (fn [ctx]
      (let [format (get-in ctx [:request :params :format])
            callback (get-in ctx [:request :params :callback])
            include-meta (boolean (get-in ctx [:request :params :meta]))
            search {:id id :ontology-object {:id cid :type (keyword ctype)}}
            result (cond
                   (= :put (get-in ctx [:request :request-method]))
                   (get-in @hz-resources [:mdl-dep-entry-resource :update])
                   (= :delete (get-in ctx [:request :request-method])) search
                   :else (hz/retrieve mdl search true true))]
      (hz-print-result ctx format callback result include-meta))))

(defresource mdl-list-resource [itype]
  hz-base-resource
  :allowed-methods [:get] ; [:get :post]
  :handle-ok
  (fn [ctx]
    (let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          itype (keyword itype)
          include-meta (boolean (get-in ctx [:request :params :meta]))
          result (hz/retrieve mdl {:type itype})]
      (hz-print-result ctx format callback result include-meta))))

(defresource mdl-entry-resource [itype id]
  hz-base-resource
  :allowed-methods [:get] ; [:get :put delete]
  :handle-ok
  (fn [ctx]
    (let [format (get-in ctx [:request :params :format])
          callback (get-in ctx [:request :params :callback])
          itype (keyword itype)
          include-meta (boolean (get-in ctx [:request :params :meta]))
          result (hz/retrieve mdl {:id id :type itype} true true)]
      (hz-print-result ctx format callback result include-meta))))

(defroutes home-routes
  (ANY "/hz-api/ont/:ctype/tree" [ctype]
    (ont-tree-resource ctype nil))
  (ANY "/hz-api/ont/:ctype/tree/:cid" [ctype cid]
    (ont-tree-resource ctype cid))
  (ANY "/hz-api/ont/:ctype" [ctype]
    (ont-list-resource ctype))
  (ANY "/hz-api/ont/:ctype/:cid" [ctype cid]
    (ont-entry-resource ctype cid))

  (ANY "/hz-api/ont/:ctype/:cid/mdl/instances" [ctype cid]
    (mdl-dep-list-resource ctype cid))
  (ANY "/hz-api/ont/:ctype/:cid/mdl/instances/:id" [ctype cid id]
    (mdl-dep-entry-resource ctype cid id))

  (ANY "/hz-api/mdl/:itype" [itype]
    (mdl-list-resource itype))
  (ANY "/hz-api/mdl/:itype/:id" [itype id]
    (mdl-entry-resource itype id))

;  (ANY "/retrieve-test/ont" [] (retrieve-test-ont))
;  (ANY "/retrieve-test/mdl/:id" [id] (retrieve-test-mdl id))

  ;(GET "/form-test" [] (form-test-page))
  ;(GET "/" [] (home-page))
  (ANY "/about" [] (about-page)))
