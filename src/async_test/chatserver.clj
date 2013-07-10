(ns async-test.chatserver
  (:require [clojure.core.async :as async :refer :all]))

;;
;;
;; main loop
;; startup go routine that manages chat server state by listening to channel.  This is the "server" process and ;; his channel is the one used to communicate with him.
;;
;; main loop listen's to the server socket
;;
;; when server socket is created, create reader/writer for that socket
;; user process takes the reader and writer and server process channel
;; user process pulls username
;; user process registers the username and writer with the "server" process.
;; user process pulls commands from user
;;    if command is a broadcast msg, send the msg to the server "process" via channel with :type broadcast
;;    if command is a private msg, send the msg to the server "process" via channel with :type private :to <username>
;;    if command is quit, this user process sends unregister message via channel with :type quit
;;             and this "process" exits.
;;
;;

(defn send-to-user [writer text]
  (.write writer text)
  (.flush writer))

(defmulti handle-message :type)

(defmethod handle-message :register [msg usermap]
  (let [name (:name msg)
        writer (:writer msg)]
    (println "handle register message for "  name)
    (conj usermap {:name name :writer writer})))

(defmethod handle-message :unregister [msg usermap]
  (let [name (:name msg)
        writer (:writer msg)]
    (disj usermap {:name name :writer writer})))

(defmethod handle-message :broadcast [msg usermap]
  (let [body (:body msg)]
    (println "handling broadcast msg: " body)
    (doseq [m usermap]
      (send-to-user (:writer m) (str body "\n")))
    usermap))

(defmethod handle-message :private [msg usermap]
  (let [body (:body msg)]
    (println "handling private msg: " body)
    usermap))

(defn start-server-process []
  (let [c (chan)]
    (go
     (loop [msg (<! c)
            usermap #{}]
       (println "here3! Size of usermap: " (count usermap) )
       (println "set of maps: " usermap)
       (let [newmap (handle-message msg usermap)]
         (recur (<! c) newmap))))
    c))

(defn process-user-command [s writer reader c]
  (go
   (send-to-user writer "Your name: ")
   (let [name (.readLine reader)]
     (>! c {:type :register :writer writer :name name})
     (loop []
       (println "here2!")
       (let [cmd (.readLine reader)]
         (if (or (nil? cmd) (= "quit" cmd))
           (do
             (>! c {:type :broadcast :body (str name " has logged out.")})
             (>! c {:type :unregister :writer writer :name name })
             (.close s))
           (do (>! c {:type :broadcast :body (str name ": " cmd)})
               (recur))))))))

;; add user
(defn start-user-process [s c]
  (let [reader (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream s)))
        writer (java.io.PrintWriter. (java.io.BufferedWriter. (java.io.OutputStreamWriter. (.getOutputStream s))))]
    (process-user-command s writer reader c)))

(defn -main []
  (let [port 2001
        ss (java.net.ServerSocket. port)
        c  (start-server-process)]
    (println "Listening on " port " for connections.")
    (loop [s (.accept ss)]
      (println "connection from " s)
      (start-user-process s c)
      (recur (.accept ss)))))
