#!/bin/sh

cd $(dirname $0)

clj -J-Dswing.defaultlaf=com.formdev.flatlaf.FlatLightLaf -J-Xmx500M -M run/protege_gui.clj
