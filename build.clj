(ns build
  (:require [clojure.tools.build.api :as b]))

(defn uber []
  (b/uber {:uber-file uber-file
           :main 'virtual-regatta.core
           :src-dirs ["src" "src/clojure"]
           :resource-dirs ["resources"]}))
