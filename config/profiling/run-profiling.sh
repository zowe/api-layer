#!/bin/bash

load_flag=
threads_flag=
threads=
dataset="mock_csv.csv"
dir_flag=
dirName="output"
host=
port=

while getopts "d:h:p:t:o:HML" flag; do
    case $flag in
        H) load_flag="high" ;;
        M) load_flag="medium" ;;
        L) load_flag="low" ;;
        d) dataset=$OPTARG ;;
        h) host=$OPTARG ;;
        p) port=$OPTARG ;;
        t) threads_flag=$OPTARG ;;
        o) dir_flag=./$OPTARG ;; # use current directory to reduce change of accidentally deleted dir
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

if [ "$load_flag" == "high" ]
then
    threads=50
elif [ "$load_flag" == "medium" ]
then
    threads=15
elif [ "$load_flag" == "low" ]
then
    threads=5
elif [ -n "$threads_flag" ]
then
    threads=$threads_flag
fi


if [ -z "$threads" ]
then
    echo "Threads value must be set with a valid load flag (-H, -M, -L) or valid value in -t flag"
    exit 1
fi

dirName="$dirName-threads-$threads"

if [ -n "$dir_flag" ]
then
    dirName=$dir_flag
fi

# TODO check if exists and confirm delete
rm -rf $dirName # jmeter fails if output directory already exists

jmeter -Jhost=$host -Jport=$port -Jthreads=$threads -Jdataset=$dataset -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l $dirName/result -e -o $dirName/test-results -j $dirName/result.log
