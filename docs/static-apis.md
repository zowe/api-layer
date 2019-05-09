# Adding Services to API Gateway without Code Changes

The basic functionality is provided at this moment. It includes 

Discovery service can register existing REST APIs without requiring them to be EurekaClients.

The definitions of such services are provided by YAML files in a directories that can be set using
the `apiml.discovery.staticApiDefinitionsDirectories` property. For example:

    -Dapiml.discovery.staticApiDefinitionsDirectories=config/local/api-defs;user/api-layer/config

The `config/local/api-defs` is the directory when the services are started on localhost.    

The Discovery Service loads these definitions during the startup.

Only routing is supported. Defining metadata for the API catalog will be supported in future.

[config/local/api-defs/staticclient.yml](/config/local/api-defs/staticclient.yml) contains a static definition of the "discoverable-client" sample service with service ID "staticclient". An integration test checks that the response of one endpoint is same.

An example for z/OSMF is provided in [config/local/api-defs/zosmf-sample.yml](/config/local/api-defs/zosmf-sample.yml)

## Reloading static API definitions

Static API definitions are loaded during the startup of Discovery Service.

They can be refreshed later by issuing a POST request to `/discovery/api/v1/staticApi`. For example:

    http -j -a eureka:password POST http://localhost:10011/discovery/api/v1/staticApi

With HTTPS enabled you need to do:

    http --cert=keystore/localhost/localhost.pem --verify=keystore/local_ca/localca.cer -j POST https://localhost:10011/discovery/api/v1/staticApi


## Diagnostics

List all registered applications in Discovery Service:

    http -j -a eureka:password GET http://localhost:10011/eureka/apps/
    http --cert=keystore/localhost/localhost.pem --verify=keystore/local_ca/localca.cer -j GET https://localhost:10011/eureka/apps/

List all routes in the API gateway:

    http --verify=keystore/local_ca/localca.cer GET https://localhost:10010/application/routes

List static API instances:

    http -j -a eureka:password GET http://localhost:10011/discovery/api/v1/staticApi
    http --cert=keystore/localhost/localhost.pem --verify=keystore/local_ca/localca.cer -j GET https://localhost:10011/discovery/api/v1/staticApi

Reload static API definitions:
    
    http -j -a eureka:password POST http://localhost:10011/discovery/api/v1/staticApi
    http --cert=keystore/localhost/localhost.pem --verify=keystore/local_ca/localca.cer -j POST https://localhost:10011/discovery/api/v1/staticApi

Notes:
 * `http` is a CLI REST API client (https://httpie.org/)
 *  `-j` requests response in JSON format
 *  `--verify=keystore/local_ca/localca.cer` trusts the CA that has signed the HTTPS certificate of the localhost system
 *  `--cert=keystore/localhost/localhost.pem` to provide client certificate that authorizes you to access the Discovery Service API


## Future steps

1. Complete the basic functionality (configuration on z/OS, documentation)
2. Display services without REST API documentation in API catalog
3. Define the API documentation (external link - e.g. to existing DocOps documentation)
4. Define the API documentation (OpenAPI/Swagger) 

   * That is served by the API service (in the current location assumed by API catalog)
   * That is served by the API service (in the location specified in YAML)
   * That is provided externally in without a service (as Swagger)

5. Active health checks (done by our Discovery Service instead of the service sending status to Eureka)
6. Refresh automatically when a definition is updated or added
