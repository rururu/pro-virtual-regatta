(MYBOAT-Info 0
(VRControl status "RUN"
	instance ?ins
	myboat-interval ?mbi)
(Clock sec ?s (= (mod ?s ?mbi) 0))
=>
;(println 6)
(nmea.server/assert-my-boat @nmea.server/MYBOAT ?s))

(Update-Boat-MapOb 0
?b1 (BOAT name ?n time ?t1)
?b2 (BOAT name ?n
	lat ?lat
	lon ?lon 
	crs ?crs
	spd ?spd
	time ?t2 (> ?t2 ?t1))
=>
;(println 8)
(retract ?b1)
(nmea.server/update-mo ?n ?lat ?lon ?crs ?spd))

(Start 0
?vrc (VRControl status "START"
	instance ?ins)
(Clock sec ?s)
=>
;(println 1)
(if (= (mod ?s 20) 0)
  (println "Waiting for VR data.."))
(when (not= @nmea.server/MYBOAT [nil nil])
  (nmea.server/modify-status ?ins "INIT")))

(Initialisation 0
?vrc (VRControl status "INIT"
	myboat-name ?mbn
	instance ?ins)
=>
;(println 5)
(println "Initilisation..")
(protege.core/clear-cls "FLEET")
(protege.core/ssv ?ins "onboard-boat" ?mbn)
(ru.igis.omtab.OMT/setTimerRunning true)
(nmea.server/set-mo-image ?mbn (nmea.server/IMG-URL "ONB"))
(nmea.server/modify-status ?ins "RUN"))

(Camera-control 0
(VRControl camera-heading ?hdg
	camera-altitude ?alt
	cam-alt-power ?apw
	camera-tilt ?tilt
	camera-range ?rng
	onboard-boat ?onb)
(Clock)
=>
(let [oob  (@kml.genvr/CAMERA :onboard)]
  (when (not= ?onb oob)
    (if (not (empty? oob))
      (nmea.server/set-mo-image oob (nmea.server/IMG-URL "NOB")))
    (nmea.server/set-mo-image ?onb (nmea.server/IMG-URL "ONB"))))
(vswap! kml.genvr/CAMERA
  assoc
  :heading ?hdg
  :altitude ?alt
  :alt-power ?apw
  :tilt ?tilt
  :range ?rng
  :onboard ?onb))

(FLEET-Info 0
(VRControl status "RUN"
	myboat-name ?myb
	fleet-interval ?fli)
(Clock sec ?s (= (mod ?s ?fli) 0))
=>
;(println 3)
(let [flt @nmea.server/FLEET]
  (println "Fleet" (count flt))
  (nmea.server/remove-fleet-boats flt ?myb)
  (nmea.server/assert-fleet-boats flt ?s)))

(Create-Boat-for-MYBOAT 0
(VRControl status "RUN"
	myboat-name ?name)
?mb (MYBOAT lat ?lat 
	lon ?lon
	crs ?crs
	spd ?spd
	date ?date
	time ?time)
=>
;(println 2)
(retract ?mb)
(nmea.server/create-or-update-mo ?name ?lat ?lon ?crs ?spd)
(asser BOAT name  ?name
	lat ?lat
	lon ?lon 
	crs ?crs
	spd ?spd
	time ?time))

