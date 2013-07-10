(ns async-test.util)

(defn send-to-user [writer text]
  "Given a writer (BufferedWriter or PrintWriter) and a string
   called 'text' , write the text to it"
  (.write writer text)
  (.flush writer))
