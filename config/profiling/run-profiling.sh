#!/bin/bash

# Take host and port as inputs
# Take low, medium, high as inputs to set # threads
# Allow overriding dataset and threads for more info
# Allow overriding directory name

load="high"
threads=50
dirName="output-${load}"
host=
port=

rm -rf $dirName # jmeter fails if output directory already exists

jmeter -Jhost=$host -Jport=$port -Jthreads=$threads -Jdataset=mock_csv.csv -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l $dirName/result -e -o $dirName/test-results -j $dirName/result.log
