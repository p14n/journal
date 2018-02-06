(ns journal.http
  (:use [compojure.core :only [routes GET POST DELETE ANY]]
        [ring.middleware.json :only [wrap-json-body]]
        org.httpkit.server)
  (:require [compojure.route :as route]
            [clojure.data.json :as json]
            [journal.logging :as l]
            [com.walmartlabs.lacinia :refer [execute]]))

(def reqid (java.util.concurrent.atomic.AtomicInteger. 0))

(defn gql-handler [gql-schema]
  (fn [request]
    (let [req-id-val (.getAndIncrement reqid)
          logger (l/map* #(assoc % :reqid req-id-val) l/systemlog)]
    (l/info logger :http/graphql request)
      (try {:status 200
            :headers {"Content-Type" "application/json"}
            :body (let [query (get-in request [:body :query])
                        vars (get-in request [:body :variables])
                        result (execute gql-schema query vars {:logger logger})]
                    (json/write-str result))}
           (catch Exception e (do (l/error logger :http/graphql "graphql query failed" e) (throw e)))))))

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

