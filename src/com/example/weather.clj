(ns com.example.weather
  (:require
    [cheshire.core :as json]
    [com.wsscode.pathom3.connect.indexes :as pci]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.smart-map :as psm]
    [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
    [com.example.xtdb :as x]
    [xtdb.api :as xt]
    [com.wsscode.pathom3.plugin :as p.plugin]
    [com.wsscode.pathom3.connect.built-in.plugins :as pbip]))

(pco/defresolver ip->lat-long
  [{:keys [ip]}]
  {::pco/output [:latitude :longitude]}
  (-> (slurp (str "https://get.geojs.io/v1/ip/geo/" ip ".json"))
      (json/parse-string keyword)
      (select-keys [:latitude :longitude])))

(pco/defresolver latlong->woeid
  [{:keys [latitude longitude]}]
  {:woeid
   (-> (slurp
         (str "https://www.metaweather.com/api/location/search/?lattlong="
              latitude "," longitude))
       (json/parse-string keyword)
       first
       :woeid)})

(pco/defresolver woeid->temperature
  [{:keys [woeid]}]
  {:temperature
   (-> (slurp (str "https://www.metaweather.com/api/location/" woeid))
       (json/parse-string keyword)
       :consolidated_weather
       first
       :the_temp)})

(pco/defresolver cold?
  [{:keys [temperature]}]
  {:cold? (< temperature 0)})

(pco/defresolver user
  [{:keys [xtdb-node]} {:keys [xt/id]}]
  {:user/ip (:user/ip (xt/entity (xt/db xtdb-node) id))
   :user/name (:user/name (xt/entity (xt/db xtdb-node) id))})

(def env
  (pci/register [ip->lat-long
                 latlong->woeid
                 woeid->temperature
                 cold?
                 user
                 (pbir/alias-resolver :user/ip :ip)
                 ]))

(defn print-node [[inputs outputs]]
  (str (vec inputs) " => " (->> outputs keys vec) "\n"))

(defn main [args-map]
  ; start smart maps with call args, if no args prints usage and graph
  (if (seq args-map)
    (let [xtdb-node (x/start-xtdb!)
          _ (x/fixtures xtdb-node)
          output (:output args-map)
          input (dissoc args-map :output)
          env (p.plugin/register env [(pbip/env-wrap-plugin #(assoc % :xtdb-node xtdb-node))])
          sm (psm/smart-map env input)]
      (println (name output) ":" (sm output))
      (x/stop-xtdb! xtdb-node))
    (println "USAGE: clj -X:ip-weather [INPUT key pairs] [:output OUTPUT]\n Available Nodes:\n"
             (->> env ::pci/index-io (map print-node)))))

(comment
  (main {:xt/id "wilker" :output :cold?})
  (main {:xt/id "wilker" :output :user/name})

  ; clj -X:ip-weather :ip '"198.29.213.3"' :output :cold?

  ; clj -X:ip-weather :xt/id '"wilker"' :output :user/name
  ; clj -X:ip-weather :xt/id '"wilker"' :output :temperature

  )