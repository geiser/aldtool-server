(ns br.usp.icmc.caed.isidgacles.kb.user ; model not use knowledge base
  (:require [clojure.java.io :as io]
            [br.usp.icmc.caed.clj-hozo-core :as hz]
            [br.usp.icmc.caed.clj-hozo-values :as values]))

(def kb-spec
  {:mdl-src (str (.getName (io/file ".")) "/site.xml")
   :ont-src (str (.getName (io/file ".")) "/ontologies/BIKE_sample.xml")})

(def kb-spec {:src mdl-src
              : 
              store kb-store
               :ont-object {:label "user" :ref-ont {:label "bicycle.xml"}}})
; (can be nil, one map with the-object in clojure form, or an HozoOntologyObject)

(defentity Users
  (kb {:mdl :ont})
  (concept "Person") ;; label, id, //or query to 
  
  )

(defn create-user [kb-spec user]
  (hz/create-instance Users (values user)))

;(defn create [])

(defn retrieve [kb-spec id-label] ;; id or label as string {:key-arrays } 
  (let [hz-model-obj (hz-core/retrieve id)]
    (hz-values/convert hz-model-obj))) ;; use list and read to retrive datas

;(defn retrieve-list [kb-spec id-label]
;  (hz-values/read))

;(defn delete [] )


