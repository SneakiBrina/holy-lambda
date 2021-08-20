(ns fierycod.holy-lambda.agent
  "Provides utils for generating native-configurations via GraalVM agent.
   GraalVM agent is convenient tool for complex project, so that
   user does not have to put each entry of reflective call to configuration by hand."
  (:require
   [fierycod.holy-lambda.util :as u]
   [clojure.edn :as edn]
   [clojure.string :as s]
   [clojure.java.io :as io])
  (:import
   [java.io File]))

(def ^:private PAYLOADS
  (try (or (io/file (io/resource "native-agents-payloads"))
           (io/file "resources/native-agents-payloads"))
       (catch Exception _
         (io/file "resources/native-agents-payloads"))))

(def ^:private AGENT_EXECUTOR "native-agent")

(defmacro in-context
  "Executes body in safe agent context for native configuration generation.
  Useful when it's hard for agent payloads to cover all logic branches.

  **In order to generate native-configuration run:**

  ```
  bb native:conf
  ```

  **Usage:**

  ```
  (in-context
    (some-body-which-has-to-be-inspected-via-graalvm))
  ```

  You can safely leave agent-context calls in the code. Agent context not set results in no code being generated by macro.
  "
  [& body]
  (if-not (System/getenv "USE_AGENT_CONTEXT")
    nil
    `(when (= (System/getProperty "executor") @#'fierycod.holy-lambda.agent/AGENT_EXECUTOR)
       (try (do ~@body)
            (catch Exception err#
              (println "Exception in agent-context: " err#))))))

(defn- agents-payloads->invoke-map
  []
  (->> (file-seq PAYLOADS)
       (filterv #(.isFile ^File %))
       (filterv #(s/includes? (.toString ^File %) ".edn"))
       (mapv #(-> % slurp edn/read-string (assoc :path (.toString ^File %))))
       (sort-by :path)))

(defn- routes->reflective-call!
  [routes]
  (doseq [{:keys [request path propagate] :as invoke-map} (agents-payloads->invoke-map)
          :let [callable-var (routes (:name invoke-map))]]
    (if-not callable-var
      (do
        (println "[holy-lambda] Lambda" (:name invoke-map) "does not exists in classpath. Incorrect name?")
        (u/exit!))

      (do
        (println "[holy-lambda] Calling lambda" (:name invoke-map) "with payloads from" (re-find #"(?<=.*)[A-Za-z0-9-_]*\..*" path))
        (if propagate
          (u/call callable-var request)
          (try
            (u/call callable-var request)
            (catch Exception _err nil)))
        (println "[holy-lambda] Succesfully called" (:name invoke-map) "with payloads from" (re-find #"(?<=.*)[A-Za-z0-9-_]*\..*" path)))))
  (println "[holy-lambda] Succesfully called all the lambdas"))
