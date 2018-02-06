(ns journal.logging
  (:require [mount.core :refer [defstate]]))

(defprotocol Logger
  (log [this msg]))

(defn map* [f logger]
  (reify Logger
    (log [this msg] (log logger (f msg)))))


(defrecord TestLogger [logs] Logger
  (log [this msg] (swap! logs conj msg)))

(defrecord StdoutLogger [] Logger
  (log [this msg] (println msg)))


(defstate systemlog :start (StdoutLogger.))

(defn stacktrace[e]
  (if e 
    (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
        (.printStackTrace e pw)
        (.toString sw))
    ""))

(defn log-msg-or-description [logger service level msg exception]
  (let [merged-msg (merge {:service service :level level} 
                          (if (map? msg) msg {:desc msg})
                          (if exception { :stacktrace (stacktrace exception)} {}))]
    (log logger merged-msg)))

(defn debug [logger service msg]
  (log-msg-or-description logger service :debug msg nil)
  msg)

(defn info [logger service msg]
  (log-msg-or-description logger service :info msg nil)
  msg)

(defn warn [logger service msg]
  (log-msg-or-description logger service :warn msg nil)
  msg)

(defn error 
  ([logger service msg] (error logger service msg nil))
  ([logger service msg exception] (log-msg-or-description logger service :error msg exception)))

(defn log-and-wrap 
  ([logger service msg err] (log-and-wrap logger service msg err nil))
  ([logger service msg err exception] (do (error logger service err exception) (assoc msg :errors [err]))))
