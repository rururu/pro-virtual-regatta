(ns kml.genvr
(:require
  [clojure.java.io :as io]
  [rete.core :as rete]
  [ru.rules :as rr])
(:import
  ru.igis.omtab.OMT
  ru.igis.omtab.Util))
(def SAIL-URL (let [rpt "resources/public/img/sailing.png"
        file (io/file rpt)
        apt (.getAbsolutePath  file)]
  (str "file:" apt)))
(def TEMP-KML (str "<kml xmlns=\"http://www.opengis.net/kml/2.2\"
  xmlns:gx=\"http://www.google.com/kml/ext/2.2\">
  <Document>
	<Camera>
	<longitude>$lon</longitude>
	<latitude>$lat</latitude>
	<range>$rng</range>
	<tilt>$tlt</tilt>
	<altitude>$alt</altitude>
	<heading>$hdg</heading>
	<altitudeMode>absolute</altitudeMode>
	</Camera>
	<Style id=\"1\">
	  <IconStyle>
	    <Icon>
	     <href>" SAIL-URL "</href>
	    </Icon>
	  </IconStyle>
	</Style>
	<Placemark>
	  <styleUrl>#1</styleUrl>
	  <name>$name</name>
	  <description>On board of boat in Race</description>
	  <Point>
		<coordinates>$lon,$lat,0</coordinates>
	  </Point>
	</Placemark>
  </Document>
</kml>"))
(def BOAT-KML "<Placemark>
	  <styleUrl>#1</styleUrl>
	  <name>$name</name>
	  <description>On board of boat in Race</description>
	  <Point>
		<coordinates>$lon,$lat,0</coordinates>
	  </Point>
	</Placemark>")
(def FLEET-PFX-KML (str "<kml xmlns=\"http://www.opengis.net/kml/2.2\"
	xmlns:gx=\"http://www.google.com/kml/ext/2.2\">
	<Document>
	  <Style id=\"1\">
	    <IconStyle>
		<Icon>
		   <href>" SAIL-URL "</href>
		</Icon>
	    </IconStyle>
	  </Style>"))
(def FLEET-SFX-KML "</Document>
</kml>")
(def CAMERA (volatile! {
  :heading 0
  :altitude 10
  :alt-power 0
  :tilt 80
  :range 100
  :onboard ""}))
(defn name-correction [name]
  (let [name (.replace name "<" "less")
         name (.replace name ">" "more")
         name (.replace name "&" "and")]
         name))

(defn substitute-in-kml [kml name lat lon hdg]
  (let [alt (* (@CAMERA :altitude) 
                   (int (Math/pow 10 (@CAMERA :alt-power))))
        name (name-correction name)
        kml (.replace kml "$name" (str name))
        kml (.replace kml "$lat" (str lat))
        kml (.replace kml "$lon" (str lon))
        kml (.replace kml "$hdg" (str hdg))
        kml (.replace kml "$alt" (str alt))
        kml (.replace kml "$tlt" (str (@CAMERA :tilt)))
        kml (.replace kml "$rng" (str (@CAMERA :range)))]
        kml))

(defn create-onboard-kml []
  (if-let [onb (rr/do-for-fact 'VRControl [] #(or (rete/slot-value 'onboard-boat %)
                                                                              (rete/slot-value 'myboat-name %)))]
  (if-let [mo (OMT/getMapOb onb)]
    (let [lat (.getLatitude mo)
             lon (.getLongitude mo)
             crs (.getCourse mo)
             dis (/ (@CAMERA :range) 1852)
             [lat2 lon2] (Util/relPos lat lon (double crs) dis)
             name (name-correction onb)
             hdg (+ crs (@CAMERA :heading))
             hdg (if (> hdg 360) (- hdg 360) hdg)
             kml (substitute-in-kml TEMP-KML name lat2 lon2 hdg)]
      kml))))

(defn create-fleet-kml []
  (let [fleet (fn [f]
                  (let [name (rete/slot-value 'name f)
                          mo (OMT/getMapOb name)]
                    (if mo
                      (let [lat (.getLatitude mo)
                              lon (.getLongitude mo)
                              name (name-correction name)
                              kml (.replace BOAT-KML "$name" name)
                              kml (.replace kml "$lat" (str lat))
                              kml (.replace kml "$lon" (str lon))]
                         kml)
                      "")))
        onb (rr/do-for-fact 'VRControl [] #(rete/slot-value 'onboard-boat %))
        fbb (rr/collect-for-all-facts 'BOAT [['name not= onb]] fleet)
        kml (apply str fbb)
        kml (str FLEET-PFX-KML kml FLEET-SFX-KML)]
  ;;(println :FLEET-KML kml)
  kml))

