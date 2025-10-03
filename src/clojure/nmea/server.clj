(ns nmea.server
(:use
  protege.core)
(:require 
  [org.httpkit.server :as http]
  [compojure.core :refer [defroutes GET POST]]
  [compojure.route :refer [files not-found]]
  [compojure.handler :refer [site]]
  [ring.util.response :as resp]
  [clojure.java.io :as io]
  [rete.core :as rete]
  [ru.rules :as rr]
  [kml.genvr :as kml])
(:import
  clojuretab.ClojureTab
  java.net.URL
  javax.swing.ImageIcon
  ru.igis.omtab.OpenMapTab
  ru.igis.omtab.OMT
  ru.igis.omtab.MapOb
  ru.igis.omtab.Util
  ru.igis.omtab.ext.RosetteHandler
  ru.igis.omtab.ext.SpinnerHandler
  ru.igis.omtab.ext.AddonListener
  dk.dma.ais.sentence.Vdm
  dk.dma.ais.message.AisMessage
  ru.igis.ais.AIVDMProcessor
  ru.vrd.nmea.VRdNMEAReciever
  edu.stanford.smi.protege.ui.DisplayUtilities))
(def defonce-SERVER-RUN (defonce SERVER-RUN false))
(def FLEET ;;(volatile! (read-string (slurp "data/vr/FLEET.clj")))
(volatile! {}))
(def MYBOAT ;;(volatile! (read-string (slurp "data/vr/MYBOAT.clj")))
(volatile! [nil nil]))
(def PORT 8081)
(def IMG-URL {"NOB" "file:resources/public/img/yachtg.png"
 "ONB" "file:resources/public/img/yachtr.png"})
(defn round-speed [s]
  (let [s (* s 100)
       s (Math/round s)]
  (double (/ s 100))))

(defn parse-coord [c1 c2]
  (let [c (read-string c1)
      mf (- c (int c))
      dm (/ (int c) 100)
      d (int dm)
      m (int (* (- dm d) 100))
      m (+ m mf)
      d (if (or (= c2 "S") (= c2 "W")) (str "-" d) d)
      r (MapOb/getDeg (str d " " m))]
  (if (and (> r 0) (or (= c2 "S") (= c2 "W"))) 
    (- r)
    r)))

(defn diff-time? [ndata odata]
  (let [[tim1 & _] ndata
       [tim2 & _] odata]
  (not= tim1 tim2)))

(defn round-sog [sog]
  (float (/ sog 10)))

(defn parse-GPRMC [gprmc]
  (try
  (let [d (.split gprmc ",")]
    (if (>= (count d) 10)
      (let [[_ time sts lat1 lat2 lon1 lon2 spd crs date] d]
        (if (not= time (second @MYBOAT))
          (let [gprmc [time
                                 (parse-coord lat1 lat2)
                                 (parse-coord lon1 lon2)
                                 (round-speed (read-string spd))
                                 (read-string crs)
                                 date]]
            (if (not= time (first @MYBOAT))
              (println "My boat GPRMC data" gprmc))
            (vreset! MYBOAT gprmc))))))
  (catch Exception e
    (println :gprmc-data :FAILURE)
    nil)))

(defn parse-AIVDM [aivdm]
  (letfn [(to-map [s]
             (let [s (.substring s 1 (dec (count s)))
                    s (.split s " ")
                    s (remove #(.startsWith % "pos") s)
                    s (remove #(.startsWith % "=") s)
                    s (remove #(.startsWith % "(") s)
                    s (apply str s)
                    s (.replaceAll s "=" " ")
                    s (str "{" s "}")]   
               (read-string s)))]
  (try
    (let [vdm (Vdm.)
           _ (.parse vdm aivdm)
           mes (AisMessage/getInstance vdm)
           pos (.getValidPosition mes)
           tos (.toString mes)
           mp (to-map tos)
           mp (assoc mp 'pos [(.getLatitude pos) (.getLongitude pos)])
           imo (mp 'userId)]
       (vswap! FLEET assoc imo (merge mp (@FLEET imo))))
    (catch Exception e
      (let [lio (.lastIndexOf aivdm "*")]
        (when (= (.substring aivdm (dec lio) lio) "4")
          (AIVDMProcessor/process aivdm)
          (let [imo (AIVDMProcessor/getIMO)
                 snm (AIVDMProcessor/getShipName)]
            (vswap! FLEET assoc imo (assoc (@FLEET imo) 'name snm))) )) ))))

(defn static-file-handler [req]
  (let [uri (:uri req)
        file-path (str (.substring uri 6))
        file (io/file file-path)]
    (if (.exists file)
      (resp/file-response file-path)
      (resp/not-found "Not Found"))))

(defn handler [req]
  (cond 
  (clojure.string/starts-with? (:uri req) "/file:") 
    (static-file-handler req)
  (= (:request-method req) :post)
    (let [race-id (-> req :uri (subs 6 9) Integer/parseInt)
            body (slurp (:body req))]
        ;;(ctpl (str :BODY " " body))
        ;;(ctpl (str :RACE " " race-id))
        (cond 
          (.startsWith body "!AIVDM") (parse-AIVDM body)
          (.startsWith body "$GPRMC") (parse-GPRMC body))
        {:status 204
         :headers {"Access-Control-Allow-Origin" "*"}})
  (= (:request-method req) :get)
     {:status 200
      :headers {"Access-Control-Allow-Origin" "*"
                         "Content-type" "text/kml"}
      :body (cond 
                    (= (:uri req) "/camera")
                      (kml/create-onboard-kml)
                    (= (:uri req) "/fleet")
                      (kml/create-fleet-kml))}))

(defn run-server
  ([]
  (if (not SERVER-RUN)
    (run-server PORT)))
([port]
  (println "Starting HTTP server on port" port "..")
  (http/run-server handler {:port port})
  (def SERVER-RUN true)))

(defn modify-status [ins sts]
  (ssv ins "status" sts)
(rr/modify-instances [ins]))

(defn create-or-update-mo [name lat lon crs spd]
  (let [moi (or (fifos "FLEET" "label" name)
                   (let [ins (shallow-copy (fifos "NavOb" "label" "yg"))]
                     (ssv ins "label" name)
                     (change-cls ins "FLEET")
                     ins))
         mo (OMT/getOrAdd moi)]
  (.setLatitude mo (double lat))
  (.setLongitude mo (double lon))
  (.setCourse mo (int crs))
  (.setSpeed mo (double spd))))

(defn assert-fleet-boats [flmp time]
  (doseq [[imo bmp] flmp]
  (try
    (let [[lat lon]  (bmp 'pos)
            name  (bmp 'name) 
            crs (int (/ (bmp 'cog) 10))
            spd (round-sog (bmp 'sog))]
      (create-or-update-mo name lat lon crs spd)
      (rete/assert-frame ['BOAT 
        'name name
        'lat lat
        'lon lon
        'crs crs
        'spd spd
        'time time
        'imo imo]))
    (catch Exception e
      (println "Corrupted or incomplete information:")
      (println "  " :BMP bmp)))))

(defn assert-my-boat [boat sec]
  (let [[vrt lat lon spd crs date :as bot] boat]
  (when (> (count bot) 2)
    (rete/assert-frame ['MYBOAT 
      'lat lat
      'lon lon
      'crs (int crs)
      'spd spd
      'date date
      'time sec]))))

(defn remove-fleet-boats [flmp myb]
  (let [nn (for [[imo bmp] flmp] (bmp 'name))]
  (doseq [i (cls-instances "FLEET")]
    (let [lab (sv i "label")]
      (if (not (or (some #{lab} nn) myb))
        (OMT/removeMapOb i true))))))

(defn set-mo-image [lab url]
  (if-let [mo (OMT/getMapOb lab)]
  (let [url (URL. url)
          imi (ImageIcon. url)
          lm (.getLocationMarker mo)]
    (.setImageIcon lm imi))))

(defn go-onboard [hm inst]
  (println :go-onboard)
(let [sel (DisplayUtilities/pickInstanceFromCollection 
                nil (cls-instances "FLEET") 0 "Select Boat")]
  (when sel
    (ssv inst "onboard-boat" (sv sel "label"))
    (println  (sv sel "label"))
    (rr/modify-instances [inst]))))

(defn update-mo [name lat lon crs spd]
  (when-let [mo (and name (OMT/getMapOb name))]
  (.setLatitude mo (double lat))
  (.setLongitude mo (double lon))
  (.setCourse mo (int crs))
  (.setSpeed mo (double spd))))

(defn run-es [hm inst]
  (if-let [esi (fainst (cls-instances "Run") "VR")]
  (rr/run-engine esi)))

(defn refresh-camera [hm inst]
  (rr/modify-instances [inst]))

(defn get-camera-panel []
  (if-let [omt (Util/getTab "ru.igis.omtab.OpenMapTab")]
  (if-let [cmp (Util/getComponentOfClass omt "ru.igis.omtab.ext.CameraPanel")]
    cmp)))

(defn restart-vr-plugin []
  (println "RESTART VR PLUGIN")
(clock/stop-clock)
(println "1. Loading Clojure Programs..")
(if-let [wps (ClojureTab/findAnnotated (cls-instances "WorkingPrograms") "VR")]
  (do
    (loop [i 1 pins (svs wps "cloPrograms")]
      (when (seq pins)
        (println (str " 1." i " " (sv (first pins) "title") " = " (ClojureTab/loadProgram (first pins)) ))
        (recur (inc i) (rest pins))))
    (println "2. Start Expert System..")
    (if-let [run (ClojureTab/findAnnotated (cls-instances "Run") "VR")]
      (do
        (ClojureTab/invoke "ru.rules" "run-engine" run)
        (println "3. Assert VR Control..")
        (if-let [vrc (ClojureTab/findAnnotated (cls-instances "VRControl") "VR")]
          (do
            (rr/assert-instances [vrc])
            (println "4. Start Clock..")
            (clock/start-clock))
          (println "  Annotated with \"VR\" instance of VRControl not found!")))))))

(defn start-camera-control [ins]
  (if-let [cmp (get-camera-panel)]
  (let [rosh (proxy [RosetteHandler] []
                      (rosetteDirection [dir]
                        (ssv ins "camera-heading" dir)
                        (rr/modify-instances [ins])
                        dir))
           spih (proxy [SpinnerHandler] []
                      (spinnerValue [ttt val]
                        (condp = ttt
                          "heading" (ssv ins "camera-heading" val)
                          "altitude" (ssv ins "camera-altitude" val)
                          "altitude power" (ssv ins "cam-alt-power" val)
                          "tilt" (ssv ins "camera-tilt" val)
                          "range" (ssv ins "camera-range" val))
                        (rr/modify-instances [ins])
                        val))
           addh (proxy [AddonListener] []
                        (actionPerformed [evt]
                          (condp = (.getActionCommand evt)
                            "start" (restart-vr-plugin)
                            "stop" (clock/stop-clock)
                            "action" (if-let [vrc (fainst (cls-instances "VRControl") "VR")]
                                               (go-onboard nil vrc)))))]
    (.setRosetteHandler cmp rosh)
    (.setSpinnerHandler cmp spih)
    (.setAddonListener cmp addh)
    (println "Camera Control started.."))
  (println "CameraPanel not found!")))

