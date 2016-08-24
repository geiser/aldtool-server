(ns br.usp.icmc.caed.isidgacles.kb.core
  (:require [clojure.java.io :as io]))

(def kb-store (str (.getName (io/file ".")) "/site.kb"))



