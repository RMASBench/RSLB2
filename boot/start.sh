#!/bin/bash

# Cleanup processes when exiting
trap "kill 0" SIGINT SIGTERM EXIT

RSL_SIM_PATH=$(cd ../../roborescue; pwd)
. functions.sh

processArgs $*

cd $RSL_SIM_PATH/boot
startKernel --nomenu --autorun --nogui
startSims --nogui
cd -

if [ -z "$NO_RSLB2" ]; then
    startRslb2 &
    PIDS="$PIDS $!"
fi

waitUntilFinished $PIDS

# Cleanup
kill $PIDS 2>/dev/null
