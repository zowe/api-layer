## jmeter cli mode

jmeter -Jusername=USER -Jpassword=validPassword -Jhost=localhost -Jport=10010 -Jthreads=10 -Jdataset=mock_csv_small.csv -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l output/result -e -o output/test-results -j output/result.log

## jmeter using taurus

* not working at the moment

bzt taurus/taurus_config.yml

### Actual performance test

1) Caching service with InMemmory impl.

2) 1 Caching service with Vsam

3) 2 Caching services with Vsam

#### Thread levels: 

Low load: 5 threads / 1000 records

jmeter -Jusername=USER -Jpassword=validPassword -Jhost=localhost -Jport=10010 -Jthreads=5 -Jdataset=mock_csv.csv -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l output-l/result -e -o output-l/test-results -j output-l/result.log

Medium load: 15 threads

jmeter -Jusername=USER -Jpassword=validPassword -Jhost=localhost -Jport=10010 -Jthreads=15 -Jdataset=mock_csv.csv -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l output-m/result -e -o output-m/test-results -j output-m/result.log

High load: 50 threads

jmeter -Jusername=USER -Jpassword=validPassword -Jhost=localhost -Jport=10010 -Jthreads=50 -Jdataset=mock_csv.csv -Jjmeter.reportgenerator.overall_granularity=1000 -n -t caching-profiling-parametrized.jmx -l output-h/result -e -o output-h/test-results -j output-h/result.log


