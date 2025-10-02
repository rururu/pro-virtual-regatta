(MYBOAT-Info 0
(VRControl status "RUN"
	instance ?ins
	myboat-interval ?mbi)
(Clock sec ?s (= (mod ?s ?mbi) 0))
=>
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
(retract ?b1)
(nmea.server/update-mo ?n ?lat ?lon ?crs ?spd))

(Start-Server 0
?vrc (VRControl status "START"
	instance ?ins)
(Clock sec ?s (> ?s 5))
=>
(println "Start NMEA server..")
(nmea.server/run-server)
(nmea.server/modify-status ?ins "CONNVR"))

(Connect-to-VR 0
?vrc (VRControl status "CONNVR"
	instance ?ins)
(Clock sec ?s)
=>
(if (= (mod ?s 20) 0)
  (println "Waiting for VR data.."))
(when (not= @nmea.server/MYBOAT [nil nil])
  (nmea.server/modify-status ?ins "INIT")))

(Initialisation 0
?vrc (VRControl status "INIT"
	instance ?ins)
=>
(println "Initilisation..")
(protege.core/clear-cls "FLEET")
(protege.core/ssv ?ins "onboard-boat" "")
(nmea.server/modify-status ?ins "RUN"))

(FLEET-Info 0
(VRControl status "RUN"
	myboat-name ?myb
	fleet-interval ?fli)
(Clock sec ?s (= (mod ?s ?fli) 0))
=>
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
(retract ?mb)
(nmea.server/create-or-update-mo ?name ?lat ?lon ?crs ?spd)
(asser BOAT name  ?name
	lat ?lat
	lon ?lon 
	crs ?crs
	spd ?spd
	time ?time))

(Go-Onboard -1
?vrc (VRControl status "GOONB"
	instance ?ins
	onboard-boat ?onb)
=>
(nmea.server/go-onboard ?onb ?ins ?ins))

