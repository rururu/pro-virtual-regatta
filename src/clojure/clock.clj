(ns clock
(:use
  rete.core)
(:import
  java.util.Timer
  java.util.TimerTask))
(def TIMER nil)
(defn stop-timer-task []
  (when (some? TIMER)
  (.cancel TIMER)
  (def TIMER nil))
(if (nil? TIMER)
  (println "Clock stopped.")))

(defn start-timer-task [fun]
  (if (some? TIMER)
  (stop-timer-task))
(def TIMER (Timer.))
(.schedule 
  TIMER 
  (proxy [TimerTask] [] (run [] (fun)))
  (long 0) 
  (long 1000)))

(defn current-time []
  (System/currentTimeMillis))

(defn current-sec []
  (int (/ (current-time) 1000)))

(defn start-clock []
  (letfn [(step-clock []
              (if-let [clk (seq (facts-with-slot-value 'Clock 'sec not= 0))]
                (modify-fact (ffirst clk) {'sec  (current-sec)})
                (assert-frame ['Clock 'sec (current-sec)]))
              (fire))]
  (when (nil? TIMER)
    (start-timer-task step-clock)
    (println "Clock started..."))))

(defn stop-clock []
  (stop-timer-task))

(defn restart-clock [fun]
  (stop-clock)
(start-clock fun))

