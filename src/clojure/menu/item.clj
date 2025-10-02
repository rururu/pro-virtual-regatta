(ns menu.item
(:use protege.core)
(:import clojuretab.ClojureTab))
(defn clojure-work []
  (println "INITIALIZING EXPERT SYSTEM")
(println "1. Loading Clojure Programs...")
(if-let [wps (ClojureTab/findAnnotated (cls-instances "WorkingPrograms") "VR")]
  (loop [i 1 pins (svs wps "cloPrograms")]
    (when (seq pins)
      (println (str " 1." i " " (sv (first pins) "title") " = " (ClojureTab/loadProgram (first pins)) ))
      (recur (inc i) (rest pins)) ) )
  (println "  Annotated with \"VR\" instance of WorkingPrograms not found!"))
(println "2. Starting Expert System.")
(if-let [run (ClojureTab/findAnnotated (cls-instances "Run") "VR")]
  (ClojureTab/invoke "ru.rules" "run-engine" run)
  (println "  Annotated with \"VR\" instance of Run not found!"))
(println "3. Show VRControl")
(when-let [vrc (ClojureTab/findAnnotated (cls-instances "VRControl") "VR")]
  (ssv vrc "status" "START")
  (.show *prj* vrc)
  (ClojureTab/invoke "nmea.server" "start-camera-control" vrc))
(println "EXPERT SYSTEM INITIALIZED"))

