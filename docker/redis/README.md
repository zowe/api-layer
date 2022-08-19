# run-redis.sh

This script will create Redis and APIML configuration files and run Redis and the Gateway, Discovery, Mock z/OS, and Caching Services via Docker Compose.

The script uses template files in `config/` and `compose/` to create a Docker Compose file that references the required APIML and Redis configuration.
The template files are turned into files with valid syntax in the `redis-containers` directory, along with other files needed (such as keystore files).

The APIML services are configured by environment variables loaded by Docker Compose via the file generated from [apiml.env.template](./compose/apiml.env.template).

Redis instances are configured by the files generated from the templates in `config/`.

Argument|Information|Behaviour if not used
---|---|---
-l|Indicates a linux host OS is being used, changing some Docker and Redis configuration.|Non-linux host OS configuration is used.
-t|Sets Redis configuration to use TLS.|Redis is ran without TLS enable.d
-s|Sets Redis and Docker Compose to use a Redis Sentinel (in addition to Redis Replica) setup.|No Sentinels are used, only a Redis Replica setup. 
-a|Sets the tag to pull for APIML jib container images.|`latest` tag is used.
-W|The script will not run the Docker Containers, it will only create the files within `redis-containers`. Useful for debugging the script.|The containers are run.

For example: 
* `run-redis.sh -ts` would run a Redis Sentinel setup for a non-linux host OS, with Redis TLS enabled, using the lastest published APIML jib containers.
* `run-redis.sh -lW` would prepare a Redis Replica setup for a linux host OS with Redis TLS disabled, without actually running the docker containers.
* `run-redis.sh -t -a 1234` would run a Redis Replica setup for a non-linux host OS with Redis TLS enabled, using APIML jib containers published with the tag `1234`.

## Notes

The Caching Service, via the Lettuce library, attempts to connect to the entire Redis topology.

The master Redis instance reports the IP address of the Redis Replica. If the networking and certificates are not set up to understand/allow the connection
to this reported IP address, the Caching Service connection to the replica will fail, even if the Redis Master and Replica can communicate.

Similarly, when Redis Sentinel is used, the Sentinel reports the host name of the master instance, which then reports the Replica IP Address.
Again, this can cause issues if the DNS of the master reported by Sentinel is not known by the Caching Service's network, or is not allowed by the 
configured TLS certificates.

## Known Issues

Due to the master instance reporting the replica's IP address
