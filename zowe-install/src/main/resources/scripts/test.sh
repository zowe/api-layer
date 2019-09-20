#!/bin/sh

PARAMS="$@"

# The default log level is WARN
# Select one of the levels: ERROR | WARN | INFO | DEBUG | TRACE
LOG_LEVEL=WARN

function usage {
    echo "Set the log level for API Mediation Layer"
    echo "usage: api-mediation-start-catalog.sh -level <level>"
    echo ""
    echo "  <level> level to be setup:"
    echo "     - ERROR - setups APIML error level"
    echo "     - WARN - setups APIML want level"
    echo "     - INFO - setups APIML info level"
    echo "     - DEBUG - setups APIML debug level"
    echo "     - TRACE - setups APIML trace level"
    echo ""
    echo "  Called with: ${PARAMS}"
}

while [ "$1" != "" ]; do
    case $1 in
        -l | --level )      shift
                                LOG_LEVEL=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     echo "Unexpected parameter: $1"
                                usage
                                exit 1
    esac
    shift
done

case $LOG_LEVEL in
    ERROR | WARN | INFO | DEBUG | TRACE )
        LEVEL=$LOG_LEVEL
        ;;
    *)
        # Wrong input, default value (WARN) is set
        LEVEL=WARN
        ;;
esac


echo Hello, $LEVEL
