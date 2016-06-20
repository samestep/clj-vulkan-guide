(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.baznex/imports "1.4.0"]
                 [commons-io "2.5"]
                 [samestep/boot-refresh "0.1.0" :scope "test"]])

(let [lwjgl-version "3.0.0"]
  (merge-env!
   :dependencies
   [['org.lwjgl/lwjgl lwjgl-version]
    ['org.lwjgl/lwjgl-platform lwjgl-version :classifier "natives-linux"]
    ['org.lwjgl/lwjgl-platform lwjgl-version :classifier "natives-windows"]]))

(require '[samestep.boot-refresh :refer [refresh]])
