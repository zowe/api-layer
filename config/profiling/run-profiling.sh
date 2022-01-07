#!/bin/bash

# Take host and port as inputs
# Take low, medium, high as inputs to set # threads
# Allow overriding dataset and threads for more info
# Allow overriding directory name

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

rm -rf $dirName # jmeter fails if output directory already exists

jmeter -Jhost=$host -Jport=$port -Jthreads=$threads -Jdataset=$dataset -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l $dirName/result -e -o $dirName/test-results -j $dirName/result.log
