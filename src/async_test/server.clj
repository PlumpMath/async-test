(ns async-test.server
  (:require [clojure.core.async :as async :refer :all]
            [async-test.util :as util]))

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
      (util/send-to-user (:writer m) (str body "\n")))
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
       (let [newmap (handle-message msg usermap)]
         (recur (<! c) newmap))))
    c))
