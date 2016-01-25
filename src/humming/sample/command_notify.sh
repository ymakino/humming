#!/bin/sh

EOJ="$1"
EPC="$2"

DATAFILE="/tmp/${EOJ}_${EPC}notify"

if [ -f "$DATAFILE" ]; then
  cat "$DATAFILE"
  rm "$DATAFILE"
fi