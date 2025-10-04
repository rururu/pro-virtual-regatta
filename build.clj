;; build.clj
(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'pro/virtual-regatta)
(def version "0.1.0") ; Replace with your project's version
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "src/clojure" "resources"] :target-dir class-dir})
  (b/compile-clj {:basis @basis :ns-compile '[virtual-regatta.core] :class-dir class-dir})
  (b/uber {:class-dir class-dir :uber-file uber-file :basis @basis :main 'virtual-regatta.core}))
