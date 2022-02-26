(ns com.example.graphviz
  (:require [tangle.core :as t]
            [clojure.java.io :as io]
            [com.example.weather :as w]
            [com.wsscode.pathom3.connect.indexes :as pci]))

(defn make-edge [index-attributes node]
  (when-let [reached-via (-> index-attributes node ::pci/attr-reach-via)]
    (for [input (->> reached-via keys (mapcat identity))]
      [input node])))

(defn make-graph []
  (let [index-attributes (->> w/env ::pci/index-attributes)
        nodes (->> index-attributes keys (filter keyword?))
        edges (mapcat (partial make-edge index-attributes) nodes)
        dot (t/graph->dot nodes edges {:node {:shape :box}
                                       :node->id (fn [n] (if (keyword? n) (name n) (:id n)))
                                       :node->descriptor (fn [n] (when-not (keyword? n) n))
                                       :directed? true
                                       })]
    (io/copy (t/dot->image dot "png") (io/file "images/weather.png"))))

(comment
  (make-graph)
  )