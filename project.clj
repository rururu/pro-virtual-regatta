(defproject pro-virtual-regatta "0.1.0-SNAPSHOT"
      :description "Protege Virtual Regatta project"
      :dependencies [[org.clojure/clojure "1.12.1"]
                     [protege/protege "3.5.0"]
                     [protege/JGo "3.5.0"]
                     [protege/JGoLayout "3.5.0"]
                     [protege/looks "3.5.0"]
                     [protege/unicode_panel "3.5.0"]
                     [protege/standard-extensions "3.5.0"]
                     [protege/ProtegeExtensions "3.5.0"]
                     [protege/ClojureTab "1.5.0"]
                     [protege/OpenMapTab "5.2.1"]
                     [protege/OMTExtensions "0.1.0"]
                     [protege/ScenaSupport "0.1.0"]
                     [AisMessages/AisMessages "0.1.0"]
                     [AisLibMessages/AisLibMessages "0.1.0"]
                     [GeographicLib/GeographicLib "1.42.0"]
                     [org.openmap-java/openmap "5.1.15"]
                     [enav-model/enav-model "0.1.0"]
                     [VRDnmea/VRDnmea "0.1.0"]
                     [rete/rete "5.3.0-SNAPSHOT"]
                     [ring/ring-core "1.14.2"]
                     [ring/ring-jetty-adapter "1.14.2"]
                     [ring/ring-defaults "0.7.0"]
                     [compojure/compojure "1.7.1"]
                     [http-kit/http-kit "2.8.1"]]
      :repositories {"local" ~(str (.toURI (java.io.File. "repo"))) :checksum :warn}
      :main ^:skip-aot virtual-regatta.core
      :target-path "target/%s"
      :source-paths ["src" "src/clojure"]
      :profiles {:uberjar {:aot :all}})
