(ns menu.item
(:use protege.core)
(:import clojuretab.ClojureTab))
(defn clojure-work []
  (println "INIT VR PLUGIN")
(init-protege)
(println "1. Loading Clojure Programs..")
(if-let [wps (fainst (cls-instances "WorkingPrograms") "VR")]
  (do 
    (loop [i 1 pins (svs wps "cloPrograms")]
      (when (seq pins)
        (println (str " 1." i " " (sv (first pins) "title") " = " (ClojureTab/loadProgram (first pins))))
        (recur (inc i) (rest pins))))
    (println "2. Start Camera..")
    (if-let [vrc (fainst (cls-instances "VRControl") "VR")]
      (let [omt (ru.igis.omtab.OpenMapTab/getOpenMapTab)]
        (ClojureTab/invoke "nmea.server" "start-camera-control" vrc)
        (println "3. Start NMEA server..")
        (ClojureTab/invoke "nmea.server" "run-server")
        (javax.swing.JOptionPane/showMessageDialog
           (ru.igis.omtab.OpenMapTab/getOpenMapTab)
           "<html><h3>Initialization is complete.</h3>
              Click the \"Camera Control\" button<br>
              and follow the instructions."))
      (println "  Annotated with \"VR\" instance of VRControl not found!")))
  (println "  Annotated with \"VR\" instance of WorkingPrograms not found!")))

