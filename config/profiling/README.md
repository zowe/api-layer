# Performance testing the Caching Service

The `run-profiling.sh` script can be used to run performance tests against the Caching Service.

This script uses Apache JMeter to run performance tests.

## Quick start

1. Ensure the jmeter cli is installed on your machine with `jmeter -v`.
2. Start the API Mediation Layer, including the Caching Service, with appropriate settings on the target machine.
3. Run `./run-profiling.sh -h ${APIML Gateway host} -p ${APIML Gateway port} -t ${number of threads to run}`.
4. See the results in `output-threads-${number of threads}`.

## Performance tests

Three load levels of performance tests should be used against each implemented data persistent technology in the Caching Service.
Each load level should be run once against an APIML with 1 instance of the Caching Service and once against an APIML with 2 instances of the Caching Service.

To run each load level:
1. `./run-profiling.sh -h ${host} -p ${port} -H`
2. `./run-profiling.sh -h ${host} -p ${port} -M`
3. `./run-profiling.sh -h ${host} -p ${port} -L`

## Script arguments

Required arguments:
* `-h ${host}` - host of the APIML to run performance tests against
* `-p ${port}` - port of the APIML to run performance tests against

One of the below is required:
* `-H` - run tests with a "high" load - 50 threads
* `-M` - run tests with a "medium" load - 15 threads
* `-L` - run tests with a "low" load - 5 threads
* `-t ${number of threads}` - run the provided number of threads

The following arguments are optional:
* `-d ${dataset file}` - set the CSV file containing records for use in the Caching Service requests - defaults to `mock_csv.cs`
* `-o ${output directory` - set the output directory - defaults to `./output-threads-${number of threads used}`
* `-f` - sets the script to run if possible without any user confirmations - defaults to false 
