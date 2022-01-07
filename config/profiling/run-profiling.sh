#!/bin/bash

# Take low, medium, high as inputs to set # threads - check val and set threads

load="high"
threads_flag=
threads=50
dataset="mock_csv.csv"
dirName="output-${load}"
host=
port=

while getopts "l:d:h:p:t:o:" flag; do
    case $flag in
        l) load=$OPTARG ;;
        d) dataset=$OPTARG ;;
        h) host=$OPTARG ;;
        p) port=$OPTARG ;;
        t) threads_flag=$OPTARG ;;
        o) dirName=./$OPTARG ;; # use current directory to reduce change of accidentally deleted dir
    esac
done

if [ -z "$host" ]
then
    echo "Host parameter must be set with -h flag"
    exit 1
fi

if [ -z "$port" ]
then
    echo "Port parameter must be set with -p flag"
    exit 1
fi

rm -rf $dirName # jmeter fails if output directory already exists

jmeter -Jhost=$host -Jport=$port -Jthreads=$threads -Jdataset=$dataset -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l $dirName/result -e -o $dirName/test-results -j $dirName/result.log
