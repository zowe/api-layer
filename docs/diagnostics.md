# Diagnostics

The running services needs to be diagnosed in case of problems. The Spring Boot Actuator provides a lot of useful REST API endpoints to know more about the application state. The issue is that thay should be opened to public. The idea is protect them with SAF resource on z/OS using ESM microservice. But ESM microservice is not used by API Mediation Layer code.

The default configuration exposes just few endpoints that are safe. But for local development a profile `diag` is turned on to enable all of them. 

See `config/local/gateway-service.yml` file and look for:

    spring.profiles.include: diag


Actuator endpoints can be accessed on `/application/` URL for each service. 
For example, the gateway on localhost has this URL:

    https://localhost:10010/application/


## Build Information

It can be useful to know what code is used. This is available at `/application/info` endpoint for each service. E.g:

    https://localhost:10010/application/info


It is also printed to the log as the very first messsage:

    [DS] 16:32:04.022 [main] INFO org.zowe.apiml.product.service.BuildInfo - Service discovery-service version xyz #n/a on 2018-08-23T14:28:33.223Z by plape03mac850 commit 6fd7c53
    [GS] 16:32:04.098 [main] INFO org.zowe.apiml.product.service.BuildInfo - Service gateway-service version xyz #n/a on 2018-08-23T14:28:33.231Z by plape03mac850 commit 6fd7c53
    [DC] 16:32:04.195 [main] INFO org.zowe.apiml.product.service.BuildInfo - Service discoverable-client version xyz #n/a on 2018-08-23T14:28:33.217Z by plape03mac850 commit 6fd7c53
    [AC] 16:32:04.317 [main] INFO org.zowe.apiml.product.service.BuildInfo - Service api-catalog-services version xyz #n/a on 2018-08-23T14:28:33.201Z by plape03mac850 commit 6fd7c53


## Version Information

It is also possible to know the version of API ML and Zowe (if API ML used as part of Zowe), using `/version` endpoint in API Gateway service. E.g.:

    https://localhost:10010/version
    
Example of response:

    {                                                         
        "apimlVersion": "1.3.3-SNAPSHOT build #2 (8d984be)",
        "zoweVersion": "1.8.0 build #802"                     
    }                                                         
