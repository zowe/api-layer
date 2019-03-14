# 3. Discovery of externally accessible URL of API Gateway

Date: 2018

## Status

Accepted

## Context

Services that are participating in the API ML need to know the URL of the API Gateway when they need to access other services via the gateway.

## Decision

Such services are already configured with the URL of the Discovery Service (Eureka) so it makes sense to keep information about gateway URL in Eureka.

Eureka has information about every instance of the API gateway however the `hostname` of those instances is the hostname of the instance but not the DVIPA hostname
that should be used to access the gateway with high-availability. The same is true for `ipAddress`. Other fields that are listed in https://github.com/Netflix/eureka/wiki/eureka-REST-operations were considered (namely `vipAddress` but that one is used by Spring Cloud for routing and it needs to be the service ID).
One option was to add new metadata. Second option was to use the `homePageUrl` and put the DVIPA address (hostname when hostnames are preferred or IP address) there. Second option was to use the `homePageUrl` that was already used by the API Catalog to find the URL of the API Gateway.

So the decision is to use the `homePageUrl` and put the DVIPA address (hostname when hostnames are preferred or IP address) there.

## Consequences

The configuration scripts and templates need to set `apiml.gateway.hostname` configuration property to the DVIPA address.

The services that would like to get homepage URL of the specific instance of the gateway will have to use the `hostname` or `ipAddress` fields for the particular instance of the API gateway. This should not be necessary for external applications but might be used for diagnostics purposes (e.g. access Actuator endpoints for a specific instance).
