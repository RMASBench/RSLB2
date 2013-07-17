PIDS=
DIR=$(pwd)
BASEDIR=$(cd ..; pwd)
UUID=$(uuidgen)

# Print the usage statement
function printUsage {
    echo "Usage: $0 [options]"
    echo "Options"
    echo "======="
    echo "-a    --algorithm <algorithm>   Set the algorithm to run. Default is \"MaxSum\""
    echo "-m    --map       <mapdir>      Set the map directory. Default is \"paris\""
    echo '-n    --no-rslb2                Do not run RSLB2 (useful if you want to run it externaly with a debugger)'
    echo "-l    --log       <logdir>      Set the log directory. Default is \"logs/$PID\""
    echo "-c    --config    <configdir>   Set the config directory. Default is \"config\""
    echo "-p    --plot                    Plot the run results."
    echo "-t    --team      <teamname>    Set the team name. Default is \"\""
    echo "      --think-time <millis>     Set the max. agent think time in millis. Default is 1000."
    echo "      --seed      <num>         Set the random seed to num. Default is 1.";
    echo "-s    --scenario  <scenario>    Set the scenario to run. Default is \"example\" (.xml appended automatically)."
    echo "      --start-time <step>       Set the experiment start time."
    echo "-v    --viewer                  Enable the viewer."
    echo "-vv   --kernel-viewer           Enable the kernel viewer."
}

# Process arguments
function processArgs {
    LOGDIR="logs/$UUID"
    MAP="paris"
    TEAM=""
    CONFIGDIR="$DIR/config"
    SCENARIO="example"
    THINK_TIME=1000
    PORT=$((RANDOM%5000+7000))
    SEED=1

    while [[ ! -z "$1" ]]; do
        case "$1" in
            -a | --algorithm)
                ALGORITHM="$2"
                shift 2
                ;;
            -m | --map)
                MAP="$2"
                shift 2
                ;;
            -n | --no-rslb2)
                NO_RSLB2=true
                shift 1
                ;;
            -l | --log)
                LOGDIR="$2"
                shift 2
                ;;
            -p | --plot)
                PLOT=true
                shift 1
                ;;
            --seed)
                SEED="$2"
                shift 2
                ;;
            -t | --team)
                TEAM="$2"
                shift 2
                ;;
            --think-time)
                THINK_TIME="$2"
                shift 2
                ;;
            -c | --config)
                CONFIGDIR="$2"
                shift 2
                ;;
            -h | --help)
                printUsage
                exit 1
                ;;
            -s | --scenario)
                SCENARIO="$2"
                shift 2
                ;;
            --start-time)
                START_TIME="$2"
                shift 2
                ;;
            -v | --viewer)
                VIEWER=true
                shift 1
                ;;
            -vv | --kernel-viewer)
                KERNEL_VIEWER=true
                shift 1
                ;;

            *)
                echo "Unrecognised option: $1"
                printUsage
                exit 1
                ;;
        esac
    done

    # Allow for map to simply be the name
    [ -d $MAP ] || MAP="$RSL_SIM_PATH/maps/gml/$MAP"
    if [ ! -d $MAP ] ; then
        echo "$MAP is not a directory"
        printUsage
        exit 1
    fi

    # Ensure that the scenario file exists
    if [ ! -f "$SCENARIO" ]; then
        MAPNAME=$(basename $MAP)
        SCENARIO="../scenarios/$MAPNAME/$SCENARIO.xml"
        if [ ! -f "$SCENARIO" ]; then
            echo "$SCENARIO does not exist."
            echo "Specify a valid scenario."
            printUsage
            exit 1
        fi
    fi
    # Convert the scenario to relative path (because the simulator automatically
    # appends the path in $MAP to the scenario file)
    SCENARIO_PATH=$(absPath $SCENARIO)
    MAPDIR_PATH=$(absPath $MAP)
    SCENARIO=$(relPath $MAPDIR_PATH $SCENARIO_PATH)

    # Make and expand logdir
    if ! mkdir -p $LOGDIR; then
        echo "Unable to create log directory \"$LOGDIR\"."
        printUsage
        exit 1
    fi
    LOGDIR=$(cd $LOGDIR; pwd)

    # Check that the user has specified some algorithm to run
    if [ -z "$ALGORITHM" ]; then
        echo "You must specify which algorithm to run."
        printUsage
        exit 1
    fi
    
    # Check that there exists a configuration file for the requested algorithm
    if [ ! -f "$CONFIGDIR/$ALGORITHM.cfg" ]; then
        echo "Unable to read file \"$DIR/config/$ALGORITHM.cfg\"."
        echo "No configuration file found for algorithm \"$ALGORITHM\". Please create one."
        printUsage
        exit 1
    fi

    # ... and a set of configuration files for the simulator
    SCONFIGDIR="$CONFIGDIR/roborescue"
    if [ ! -f "$SCONFIGDIR/kernel.cfg" ]; then
        echo "$SCONFIGDIR must contain the configuration files for the Robocup Rescue simulator."
        printUsage
        exit 1
    fi

    JVM_OPTS=""
}

# Start the kernel
function startKernel {
    # Extract the number of steps from the configuration
    STEPS=$(configFetchSetting $CONFIGDIR/$ALGORITHM.cfg "experiment.end_time")

    echo "Using config $SCONFIGDIR/kernel.cfg"
    KERNEL_OPTIONS="-c $SCONFIGDIR/kernel.cfg"
    KERNEL_OPTIONS="$KERNEL_OPTIONS --kernel.agents.think-time=$THINK_TIME --kernel.timesteps=$STEPS --kernel.port=$PORT"
    KERNEL_OPTIONS="$KERNEL_OPTIONS --gis.map.dir=$MAP --gis.map.scenario=$SCENARIO"
    KERNEL_OPTIONS="$KERNEL_OPTIONS --kernel.logname=/dev/null $*"
    KERNEL_OPTIONS="$KERNEL_OPTIONS --random.seed=$SEED $*"
    if [ -z "$KERNEL_VIEWER" ]; then
        JVM_OPTS="-Djava.awt.headless=true"
    else
        JVM_OPTS="-Djava.awt.headless=false"
    fi
    JVM_OPTS="$JVM_OPTS -Xmx2G"
    makeClasspath $RSL_SIM_PATH/jars $RSL_SIM_PATH/lib
    PROGRAM="-cp $CP kernel.StartKernel $KERNEL_OPTIONS"
    OUTFILE="$LOGDIR/kernel.log"
    launch

    # Wait for the kernel to start
    waitFor $LOGDIR/kernel.log "Listening for connections" $!
}

# Start the viewer and simulators
function startSims {
    JVM_OPTS="-Xmx256m -Djava.awt.headless=true"

    makeClasspath $RSL_SIM_PATH/lib
    # Simulators
    PROGRAM="-cp $CP:$RSL_SIM_PATH/jars/rescuecore2.jar:$RSL_SIM_PATH/jars/standard.jar:$RSL_SIM_PATH/jars/misc.jar rescuecore2.LaunchComponents misc.MiscSimulator -c $SCONFIGDIR/misc.cfg --kernel.port=$PORT $*"
    OUTFILE="$LOGDIR/misc.log"
    launch
    PID_MISC=$!

    PROGRAM="-cp $CP:$RSL_SIM_PATH/jars/rescuecore2.jar:$RSL_SIM_PATH/jars/standard.jar:$RSL_SIM_PATH/jars/traffic3.jar rescuecore2.LaunchComponents traffic3.simulator.TrafficSimulator -c $SCONFIGDIR/traffic3.cfg --kernel.port=$PORT $*"
    OUTFILE="$LOGDIR/traffic.log"
    launch
    PID_TRAFFIC=$!

    PROGRAM="-cp $CP:$RSL_SIM_PATH/jars/rescuecore2.jar:$RSL_SIM_PATH/jars/standard.jar:$RSL_SIM_PATH/jars/resq-fire.jar:$RSL_SIM_PATH/oldsims/firesimulator/lib/commons-logging-1.1.1.jar rescuecore2.LaunchComponents firesimulator.FireSimulatorWrapper -c $SCONFIGDIR/resq-fire.cfg --kernel.port=$PORT $*"
    OUTFILE="$LOGDIR/fire.log"
    launch
    PID_FIRE=$!

    # Wait for all simulators to start
    waitFor $LOGDIR/misc.log "connected" $PID_MISC
    waitFor $LOGDIR/traffic.log "connected" $PID_TRAFFIC
    waitFor $LOGDIR/fire.log "connected" $PID_FIRE

    # Viewer
    if [ ! -z "$VIEWER" ]; then
        echo "Launching viewer..."
        TEAM_NAME_ARG="RSLBench Team"
        if [ ! -z "$TEAM" ]; then
            TEAM_NAME_ARG="\"--viewer.team-name=$TEAM\"";
        fi
        JVM_OPTS="-Xmx256m -Djava.awt.headless=false"
        PROGRAM="-cp $CP:$RSL_SIM_PATH/jars/rescuecore2.jar:$RSL_SIM_PATH/jars/standard.jar:$RSL_SIM_PATH/jars/sample.jar rescuecore2.LaunchComponents sample.SampleViewer -c $SCONFIGDIR/viewer.cfg $TEAM_NAME_ARG --kernel.port=$PORT"
        OUTFILE="$LOGDIR/viewer.log"
        launch

        waitFor $LOGDIR/viewer.log "connected" $!
    fi
}

function startRslb2 {
    JVM_OPTS="-Xmx2G -Dlog4j.configurationFile=file://$BASEDIR/supplement/log4j2.xml -Djava.awt.headless=true"
    OPTS="-c $SCONFIGDIR/kernel.cfg -c $CONFIGDIR/$ALGORITHM.cfg --results.path=results/ --run=$UUID --kernel.port=$PORT"
    OPTS="$OPTS --random.seed=$SEED"
    if [ ! -z "$START_TIME" ]; then
        OPTS="$OPTS --experiment.start_time=$START_TIME"
    fi
    OPTS="$OPTS --gis.map.dir=$MAP --gis.map.scenario=$SCENARIO"
    PROGRAM="-jar $BASEDIR/dist/RSLB2.jar $OPTS"
    OUTFILE=""
    launch
}


#################################################################################
# Java-related helper functions
#################################################################################

# Launches a java program
function launch {
    if [ -z "$OUTFILE" ]; then
        #echo "Launching $JVM_OPTS -Dlog4.log.dir=$LOGDIR $PROGRAM"
        java $JVM_OPTS -Dlog4j.log.dir=$LOGDIR $PROGRAM &
    else
        #echo "Launching $JVM_OPTS -Dlog4.log.dir=$LOGDIR $PROGRAM 2>&1 >$OUTFILE"
        java $JVM_OPTS -Dlog4j.debug -Dlog4j.log.dir=$LOGDIR $PROGRAM 2>&1 >$OUTFILE &
    fi
    PIDS="$PIDS $!"
}

# Make a classpath argument by looking in a directory of jar files.
# Positional parameters are the directories to look in
function makeClasspath {
    RESULT="$BASEDIR/supplement:$RSL_SIM_PATH/supplement"
    while [[ ! -z "$1" ]]; do
        for NEXT in $1/*.jar; do
            RESULT="$RESULT:$NEXT"
        done
        shift
    done
    CP=${RESULT#:}
}


#################################################################################
# Utilities to wait until a program is ready / has finished
#################################################################################

# Wait for a regular expression to appear in a file.
# $1 is the log to check
# $2 is the regex to wait for
# $3 is the optional output frequency. Messages will be output every n sleeps. Default 1.
# $4 is the optional sleep time. Defaults to 1 second.
function waitFor {
    SLEEP_TIME=1
    FREQUENCY=1
    if [ ! -z "$3" ]; then
        FREQUENCY=$3
    fi
    if [ ! -z "$4" ]; then
        SLEEP_TIME=$4
    fi
    F=$FREQUENCY
    echo "Waiting for '$1' to exist..."
    while [[ ! -e $1 ]]; do
        if (( --F == 0 )); then
            echo "Still waiting for '$1' to exist..."
            F=$FREQUENCY
        fi
        sleep $SLEEP_TIME
    done
    echo "Waiting for '$2'..."
    while [ -z "`grep \"$2\" \"$1\"`" ]; do
        if (( --F == 0 )); then
            echo "Still waiting for '$2'..."
            F=$FREQUENCY
        fi
        # Check if the pid still exists, otherwise something went
        # wrong and we should fail
        if [ ! -z "$3" ]; then
            if ! kill -0 "$3" 2>/dev/null; then
                echo "Waited process died. Aborting execution." 1>&2
                exit 1
            fi
        fi
        sleep $SLEEP_TIME
    done
}

function waitUntilFinished {
    SLEEP_TIME=1

    while true; do
        for PID in $*; do
            if ! kill -0 "$PID" 2>/dev/null; then
                PID_FINISHED="$PID"
                return 1
            fi
        done
        if grep -q 'Kernel has shut down' $LOGDIR/kernel.log; then
            return 0
        fi
        sleep $SLEEP_TIME
    done
}


#################################################################################
# Utilities to manipulate paths
#################################################################################

# Outputs the absolute path of the input route (that must exist)
function absPath {
    path=$(dirname $1)
    echo $(cd $path && pwd)/$(basename $1)
}

# Outputs the relative path between two absolute routes
function relPath {
    # both $1 and $2 are absolute paths beginning with /
    # returns relative path to $2/$target from $1/$source
    source=$1
    target=$2

    common_part=$source # for now
    result="" # for now

    while [[ "${target#$common_part}" == "${target}" ]]; do
        # no match, means that candidate common part is not correct
        # go up one level (reduce common part)
        common_part="$(dirname $common_part)"
        # and record that we went back, with correct / handling
        if [[ -z $result ]]; then
            result=".."
        else
            result="../$result"
        fi
    done

    if [[ $common_part == "/" ]]; then
        # special case for root (no common path)
        result="$result/"
    fi

    # since we now have identified the common part,
    # compute the non-common part
    forward_part="${target#$common_part}"

    # and now stick all parts together
    if [[ -n $result ]] && [[ -n $forward_part ]]; then
        result="$result$forward_part"
    elif [[ -n $forward_part ]]; then
        # extra slash removal
        result="${forward_part:1}"
    fi

    echo $result
}

#################################################################################
# Utilities to parse the configuration file
#################################################################################

function configResolveIncludes {
    P=$(dirname $1)
    while egrep -q '^[^#]*!include[[:space:]]+' $1; do
        #echo "Include found!"
        line=$(nl -ba $1  | egrep -m1 '^[^#]*!include[[:space:]]+')
        #echo "Line: $line"
        numline=$(echo $line | awk '{print $1}')
        file=$P/$(echo $line | awk '{print $3}')
        #echo "Parsing included file $file..."
        totallines=$(wc -l $1 | awk '{print $1}')
        headline=$(($numline - 1))
        tailline=$(($totallines - $numline))

        # Head + input file + Tail
        if [ $headline -gt 0 ]; then
            head -n$headline $1 > $1.tmp
        fi
        cat $file >> $1.tmp
        echo "" >> $1.tmp
        tail -n$tailline $1 >> $1.tmp
        mv $1.tmp $1
    done
}

function configFetchSetting {
    # We need to resolve the configuration includes, so use a temporary file for that
    cp $1 $1.parsed
    configResolveIncludes $1.parsed

    # Now fetch (the latest value) for the given setting
    VALUE=$(egrep '[^#]*'$2':' $1.parsed | tail -n1 | cut -d ':' -f2)
    echo $VALUE

    # Remove the temporary file when done
    rm $1.parsed
}
