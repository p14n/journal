(ns p14n.http
  (:use [compojure.core :only [defroutes GET POST DELETE ANY]]
        [ring.middleware.params :only [wrap-params]]
        org.httpkit.server)
  (:require [compojure.route :as route]
            [mount.core :refer [defstate]]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia :refer [execute]]
            [p14n.graphql :as g]))



(defn variable-map
  "Reads the `variables` query parameter, which contains a JSON string
  for any and all GraphQL variables to be associated with this request.
  Returns a map of the variables (using keyword keys)."
  [request]
  (let [variables (condp = (:request-method request)
                    ;; We do a little bit more error handling here in the case
                    ;; where the client gives us non-valid JSON. We still haven't
                    ;; handed over the values of the request object to lacinia
                    ;; GraphQL so we are still responsible for minimal error
                    ;; handling
                    :get (try (-> request
                                  (get-in [:query-params "variables"])
                                  (json/read-str :key-fn keyword))
                              (catch Exception e nil))
                    :post (try (-> request
                                   :body
                                   (json/read-str :key-fn keyword)
                                   :variables)
                               (catch Exception e nil)))]
    (if-not (empty? variables)
      variables
      {})))

(defn extract-query
  "Reads the `query` query parameters, which contains a JSON string
  for the GraphQL query associated with this request. Returns a
  string.  Note that this differs from the PersistentArrayMap returned
  by variable-map. e.g. The variable map is a hashmap whereas the
  query is still a plain string."
  [request]
  (case (:request-method request)
    :get  (get-in request [:query-params "query"])
    ;; Additional error handling because the clojure ring server still
    ;; hasn't handed over the values of the request to lacinia GraphQL
    :post (try (-> request
                   :body
                   (json/read-str :key-fn keyword)
                   :query)
               (catch Exception e ""))
    :else ""))

(defn ^:private graphql-handler
  "Accepts a GraphQL query via GET or POST, and executes the query.
  Returns the result as text/json."
  [compiled-schema]
  (fn [request]
    ;; include authorization key in context
    (let [vars (variable-map request)
          query (extract-query request)
          result (execute compiled-schema query vars nil)
          status (if (-> result :errors seq)
                   400
                   200)]
      {:status status
       :headers {"Content-Type" "application/json"}
       :body (json/write-str result)})))

(defn handler [request]
  (let [uri (:uri request)]
    (if (= uri "/graphql")
      ;; hits the proper uri, process request
      ((graphql-handler g/graphql-schema) request)
      ;; not serving any other requests
      {:status 404
       :headers {"Content-Type" "text/html"}
       :body (str "Only GraphQL JSON requests to /graphql are accepted on this server")})))

(defroutes all-routes
  (GET "/graphql" [] handler)
  (POST "/graphql" [] handler)
  (route/files "/" {:root "/Users/p14n/dev/ws/job/frontend"})
  (route/not-found "Nope"))

(defn start-server []
  (run-server
   (-> all-routes
       (wrap-params)  )
   {:port 8085}))

(defstate http :start (start-server)
               :stop (http :timeout 100))

