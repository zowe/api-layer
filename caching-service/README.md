# Caching Service

To support the High Availability of all components within Zowe, components either need to be stateless, or offload the state to a location accessible by all instances of the service, including those which just started. At the current time, some services are not, and cannot be stateless. For these services, we introduce the Caching service. 

The Caching service aims to provide an API which offers the possibility to store, retrieve and delete data associated with keys. The service will be used only by internal Zowe applications and will not be exposed to the internet. The Caching service needs to support a hot-reload scenario in which a client service requests all available service data. 

The initial implementation of the service will depend on VSAM to store the key/value pairs, as VSAM is a native z/OS solution to storing key/value pairs.  Eventually, there will be other implementations for solutions such as MQs. As such, this needs to be taken into account for the initial design document. 

## Architecture

Internal architecture needs to take into consideration, namely the fact that there will be multiple storage solutions. The API, on the other hand, remains the same throughout various storage implementations. 

![Diagram](cachingServiceStructure.png "Architecture of the service")

## How to use

The Caching Service is built on top of the spring enabler, which means that it is dynamically registered to the API Mediation Layer. It appears in the API Catalog under the tile "Zowe Applications".

There are REST APIs available to create, delete, and update key-value pairs in the cache, as well as APIs to read a specific key-value pair or all key-value pairs in the cache.  

## Storage

There are multiple storage solutions supported by the Caching Service with the option to 
add custom implementation. [Additional Storage Support](#additional-storage-support) explains
what needs to be done to implement custom solution. 

### In Memory

This storage is useful for testing and integration verification. Don't use it in production. 
The key/value pairs are stored only in the memory of one instance of the service and therefore 
won't persist. 

### VSAM

TO BE DONE

### Additional Storage Support

To add a new implementation it is necessary to provide the library with the implementation
of the Storage.class and properly configure the Spring with the used implementation. 

    @ConditionalOnProperty(
        value = "caching.storage",
        havingValue = "custom"
    )
    @Bean
    public Storage custom() {
        return new CustomStorage();
    }

The example above shows the Configuration within the library that will use different storage than the default InMemory one. 

It is possible to provide the custom implementation via the -Dloader.path property provided on startup of the Caching service. 

## How do you run for local development

The Caching Service is a Spring Boot application. You can either add it as a run configuration and run it together with other services, or the npm command to run API ML also runs the mock. 

Command to run full set of api-layer: `npm run api-layer`. If you are looking for the Continuous Integration set up run: `npm run api-layer-ci`

In local usage, the Caching Service will run at `https://localhost:10016`. The API path is `/cachingservice/api/v1/cache/${path-params-as-needed}`.
For example, `https://localhost:10016/cachingservice/api/v1/cache/my-key` retrieves the cache entry using the key 'my-key'.

## Configuration properties

The Caching Service uses the standard `application.yml` structure for configuration.

`apiml.service.routes` only specifies one API route as there is no need for web socket or UI routes.
`caching.storage` this property is reserved for the setup of the proper storage within the Caching Service. 
