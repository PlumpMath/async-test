(ns async-test.chatserver
  (:require [async-test.user :as user]
            [async-test.server :as server]))

(defn -main []
  (let [port 2001
        ss (java.net.ServerSocket. port)
        c  (server/start-server-process)]
    (println "Listening on " port " for connections.")
    (loop [s (.accept ss)]
      (user/start-user-process s c)
      (recur (.accept ss)))))
