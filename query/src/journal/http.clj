(ns journal.http
  (:use [compojure.core :only [routes GET POST DELETE ANY]]
        [ring.middleware.json :only [wrap-json-body]]
        org.httpkit.server)
  (:require [compojure.route :as route]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia :refer [execute]]))

(defn gql-handler [gql-schema]
  (fn [request]
    (println (str "request:" request))
    (try {:status 200
          :headers {"Content-Type" "application/json"}
          :body (let [query (get-in request [:body :query])
                      vars (get-in request [:body :variables])
                      x (do (println vars))
                      result (execute gql-schema query vars vars)]
                  (json/write-str result))}
         (catch Exception e (do (.printStackTrace e) (throw e))))))

(defn start-server [gql-schema]
  (let [gh (gql-handler gql-schema)]
    (run-server
     (-> (routes 
          (GET "/graphql" [] gh)
          (POST "/graphql" [] gh)
          (route/files "/" {:root "/Users/p14n/dev/ws/journal/"})
          (route/not-found "Nope"))
         (wrap-json-body {:keywords? true :bigdecimals? true}))
     {:port 8085})))

