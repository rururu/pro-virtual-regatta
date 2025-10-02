#!/bin/sh

cd $(dirname $0)

clj -J-Xmx500M -M run/protege_gui.clj
