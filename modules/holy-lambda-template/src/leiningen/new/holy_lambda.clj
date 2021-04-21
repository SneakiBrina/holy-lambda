(ns leiningen.new.holy-lambda
  (:require
   [leiningen.new.templates :refer [renderer name-to-path ->files]]
   [leiningen.core.main :as main]))

(def render (renderer "holy-lambda"))

(defn holy-lambda
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}
        render* #(render % data)]
    (main/info "Generating fresh 'lein new' holy-lambda project.")
    (->files data
             ["src/{{sanitized}}/core.clj" (render* "core.clj")]
             [".clj-kondo/config.edn" (render* "clj-kondo/config.edn")]
             [".clj-kondo/clj_kondo/holy_lambda.clj" (render* "clj-kondo/clj_kondo/holy_lambda.clj")]
             ["resources/native-deps/.gitkeep" (render* "gitkeep")]
             ["resources/native-agents-payloads/1.edn" (render* "1.edn")]
             ["Makefile" (render* "Makefile")]
             ["README.md" (render* "README.md")]
             ["project.clj" (render* "project.clj")]
             ["template.yml" (render* "template.yml")]
             ["template-native.yml" (render* "template-native.yml")]
             ["resources/bootstrap-native" (render* "bootstrap-native")]
             [".editorconfig" (render* "editorconfig")]
             [".gitignore" (render* "gitignore")])))
