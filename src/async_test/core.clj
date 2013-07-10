(require '[clojure.core.async :as async :refer :all])

(def cntr (atom 0))

(defn sleepy-func [x]
  (Thread/sleep 1000)
  x)

(defn sleepy-func2 [x]
  (Thread/sleep 10)
  x)

(defn test-me []
  (let [c (chan)]

    (doseq [_ (range 200)]
      (go
       (println "cntr is now: " @cntr ", thread-id: " (.getName (Thread/currentThread)))
       (flush)

       (println "got val: "(<! c) " counter at: " @cntr ", thread-id: " (.getName (Thread/currentThread)))
       (flush)))
    (doseq [i (range 100)]
      (go
       (>! c (sleepy-func i))
       (swap! cntr inc)
       ))
    (doseq [i (range 100)]
      (go
       (>! c (str "hello " i))
       (println "made it out, thread-id: " (.getName (Thread/currentThread))))))
  (Thread/sleep 15000))
