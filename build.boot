(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.baznex/imports "1.4.0"]
                 [commons-io "2.5"]])

(let [lwjgl-version "3.0.0"]
  (merge-env!
   :dependencies
   [['org.lwjgl/lwjgl lwjgl-version]
    ['org.lwjgl/lwjgl-platform lwjgl-version :classifier "natives-linux"]
    ['org.lwjgl/lwjgl-platform lwjgl-version :classifier "natives-windows"]]))

(merge-env! :dependencies '[[org.clojure/tools.namespace "0.2.11"]])

(require '[clojure.tools.namespace.repl :as tns])

(deftask refresh []
  (with-pass-thru _
    (apply tns/set-refresh-dirs (get-env :directories))
    (with-bindings {#'*ns* *ns*}
      (tns/refresh))))
