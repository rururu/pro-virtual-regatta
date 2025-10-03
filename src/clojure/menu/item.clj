(ns menu.item
(:use protege.core)
(:import clojuretab.ClojureTab))
(defn clojure-work []
  (println "START VR PLUGIN")
(println "1. Loading Clojure Programs..")
(if-let [wps (ClojureTab/findAnnotated (cls-instances "WorkingPrograms") "VR")]
  (do 
    (loop [i 1 pins (svs wps "cloPrograms")]
      (when (seq pins)
        (println (str " 1." i " " (sv (first pins) "title") " = " (ClojureTab/loadProgram (first pins))))
        (recur (inc i) (rest pins))))
    (println "2. Start Camera..")
    (if-let [vrc (ClojureTab/findAnnotated (cls-instances "VRControl") "VR")]
      (do
        (ClojureTab/invoke "nmea.server" "start-camera-control" vrc)
        (println "3. Start NMEA server..")
        (ClojureTab/invoke "nmea.server" "run-server"))
      (println "  Annotated with \"VR\" instance of VRControl not found!")))
  (println "  Annotated with \"VR\" instance of WorkingPrograms not found!")))

