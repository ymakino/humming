#!/bin/sh

EOJ="$1"
EPC="$2"

DATAFILE="/tmp/${EOJ}_${EPC}"

if [ -f "$DATAFILE" ]; then
  cat "$DATAFILE"
elif [ $EPC = "80" ]; then
  echo 30
elif [ $EPC = "81" ]; then
  echo 40
elif [ $EPC = "E0" ]; then
  echo 0123
else
  echo 00
fi

