(ns p14n.http
  (:use [compojure.core :only [defroutes GET POST DELETE ANY]]
        [ring.middleware.json :only [wrap-json-body]]
        org.httpkit.server)
  (:require [compojure.route :as route]
            [mount.core :refer [defstate]]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia :refer [execute]]
            [p14n.graphql :as g]))

(defn gql-handler [request]
  (println (str "request:" request))
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (let [query (get-in request [:body :query])
               vars (get-in request [:body :variables])
               x (do (println vars))
               result (execute g/graphql-schema query vars vars)]
           (json/write-str result))})

(defroutes all-routes
  (GET "/graphql" [] gql-handler)
  (POST "/graphql" [] gql-handler)
  (route/files "/" {:root "/Users/p14n/dev/ws/journal/"})
  (route/not-found "Nope"))

(defn start-server []
  (run-server
   (-> all-routes
       (wrap-json-body {:keywords? true :bigdecimals? true}))
   {:port 8085}))

(defstate http :start (start-server)
               :stop (http :timeout 100))

