#!/bin/sh

cd $(dirname $0)

clj -J-Xmx1024M -M run/vr.clj
