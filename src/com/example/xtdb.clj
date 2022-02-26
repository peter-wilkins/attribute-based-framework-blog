(ns com.example.xtdb
  (:require [clojure.java.io :as io]
            [xtdb.api :as xt]))

(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file dir)
                        :sync? true}})]
    (xt/start-node
      {:xtdb/tx-log (kv-store "data/dev/tx-log")
       :xtdb/document-store (kv-store "data/dev/doc-store")
       :xtdb/index-store (kv-store "data/dev/index-store")})))

(defn fixtures [xtdb-node]
  (xt/submit-tx xtdb-node [[::xt/put
                            {:xt/id "wilker"
                             :user/name "Wilker Lucio"
                             :user/ip "198.29.213.3"}]]))

(defn stop-xtdb! [xtdb-node]
  (.close xtdb-node))

