(ns com.example.weather
  (:require
    [cheshire.core :as json]
    [com.wsscode.pathom3.connect.indexes :as pci]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.smart-map :as psm]))

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
  [{:keys [:temperature]}]
  {:cold? (< temperature 0)})

(def env
  (pci/register [ip->lat-long
                 latlong->woeid
                 woeid->temperature
                 cold?]))

(defn print-node [[inputs outputs]]
  (str (vec inputs) " => " (->> outputs keys vec) "\n"))

(defn main [args-map]
  ; start smart maps with call args, if no args prints usage and graph
  (if (seq args-map)
    (let [output (:output args-map)
          input (dissoc args-map :output)
          sm (psm/smart-map env input)]
      (println (name output) ":" (sm output)))
    (println "USAGE: clj -X:ip-weather [INPUT key pairs] [:output OUTPUT]\n Available Nodes:\n" (->> env ::pci/index-io (map print-node)))))

(comment
  (main {:ip "198.29.213.3" :output :cold?})
  ; clj -X:ip-weather :ip '"198.29.213.3"' :output :cold?
  )