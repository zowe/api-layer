#!/bin/bash

load_flag=
threads_flag=
threads=
dataset="mock_csv.csv"
dir_flag=
dir="output"
host=
port=
force=0

while getopts "HMLt:h:p:d:o:f" flag; do
    case $flag in
        H) load_flag="high" ;;
        M) load_flag="medium" ;;
        L) load_flag="low" ;;
        t) threads_flag=$OPTARG ;;
        h) host=$OPTARG ;;
        p) port=$OPTARG ;;
        d) dataset=$OPTARG ;;
        o) dir_flag=./$OPTARG ;; # use current directory to reduce chance of accidentally deleted dir
        f) force=1 ;;
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
else
    echo "Threads value must be set with a valid load flag (-H, -M, -L) or valid value in -t flag"
    exit 1
fi

dir="$dir-threads-$threads"

if [ -n "$dir_flag" ]
then
    dir=$dir_flag
fi

 # jmeter fails if output directory already exists so check for it and delete if appropriate
if [ -d $dir ]
then
    if [ $force -eq 1 ]
    then
        echo "Force deleting existing output directory '$dir'"
        rm -rf $dir
    else
        read -p "Output directory '$dir' already exists, do you wish to delete this directory? (yn) " yn
        case $yn in
            [Yy]*) rm -rf $dir ;;
            *) echo "Directory already exists and won't be overwritten, exiting"; exit 1 ;;
        esac
    fi
fi

jmeter -D javax.net.ssl.keyStore=../../keystore/client_cert/client-certs.p12 -D javax.net.ssl.keyStorePassword=password -Jhost=$host -Jport=$port -Jthreads=$threads -Jdataset=$dataset -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l $dir/result -e -o $dir/test-results -j $dir/result.log
