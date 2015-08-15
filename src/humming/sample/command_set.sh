#!/bin/sh

EOJ="$1"
EPC="$2"
DATA="$3"

DATAFILE="/tmp/${EOJ}_${EPC}"

/bin/echo -n "$DATA" > "$DATAFILE"
