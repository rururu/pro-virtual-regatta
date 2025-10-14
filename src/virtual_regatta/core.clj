(ns virtual-regatta.core
  (:gen-class))

(defn -main [& args]
  (println "\nProtege-3.5")
  (println "A free, open-source ontology editor and framework for building intelligent systems")
  (println "(http://protege.stanford.edu/)\n")
  (edu.stanford.smi.protege.Application/main (into-array String ["pprj_examples/VirtualRegatta.pprj"]))
  (Thread/sleep 1000)
  (load-file "src/clojure/protege/core.clj")
  (load-file "src/clojure/menu/item.clj")
  (clojuretab.ClojureTab/invoke "protege.core" "init-protege")
  (clojuretab.ClojureTab/invoke "menu.item" "clojure-work"))
