(ns async-test.user
  (:require [clojure.core.async :as async :refer :all]
            [async-test.util :as util])
  (:import ( java.io BufferedReader
                     InputStreamReader
                     BufferedWriter
                     OutputStreamWriter)))

;; Right now the only command is "quit",
;; everythign else is assumed to be a broadcast message.
;; Future commands could be added later.
(defn process-commands [s writer reader c]
  (thread
   (util/send-to-user writer "Your name: ")
   (let [name (.readLine reader)]
     (>!! c {:type :register :writer writer :name name})
     (loop []
       (let [cmd (.readLine reader)]
         (if (or (nil? cmd) (= "quit" cmd))
           (do ;; If the cmd is null or "quit", then disconnect the user.
             (>!! c {:type :broadcast :body (str name " has logged out.")})
             (>!! c {:type :unregister :writer writer :name name })
             (.close s))
           (do ;; Else, broadcast the message to everyone
             (>!! c {:type :broadcast :body (str name ": " cmd)})
             (recur))))))))

;;
;; process in this case refers to "light-weight process" like go routines.
;; When a user joins, start a lightweight process to handle the user
;;
(defn start-user-process [s c]
  (let [reader (BufferedReader. (InputStreamReader. (.getInputStream s)))
        writer (BufferedWriter. (OutputStreamWriter. (.getOutputStream s)))]
    (process-commands s writer reader c)))
