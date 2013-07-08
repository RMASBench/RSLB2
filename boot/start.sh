#!/bin/bash

# Cleanup processes when exiting
trap "kill 0" SIGINT SIGTERM EXIT

RSL_SIM_PATH=$(cd ../../roborescue; pwd)
. functions.sh

processArgs $*

if [ -z "$KERNEL_VIEWER" ]; then
	startKernel --nomenu --autorun --nogui
else
	startKernel --nomenu --autorun
fi
startSims --nogui

if [ -z "$NO_RSLB2" ]; then
    startRslb2 &
    PIDS="$PIDS $!"
fi

waitUntilFinished $PIDS

if [ -n "$PLOT" ]; then
    results/plot.sh "results/$ALGORITHM-$$.dat"
fi

# Cleanup
kill $PIDS 2>/dev/null
