# Hello World API Service in Express

This is an example how an API service implemented using [Node.js](https://nodejs.org/en/) and [Express](https://expressjs.com/) can be registered to the API Mediation Layer. 

There are following files:
 
 - [index.js](index.js) - starts the API service implemented in Express and registers it to the Discovery service

 - [apiLayerService.js](apiLayerService.js) - a module that does the registration to the Discovery Service using [eureka-js-client](https://www.npmjs.com/package/eureka-js-client)

 This example contains the full HTTPS validation of both Discovery Service and the Hello World service.

 The certicate, private key for the service, and the local CA certificate are loaded from `keystore/localhost/localhost.keystore.p12`.

 You can start the service using:

    cd helloworld-expressjs
    node index.js

If the APIML is already running then you should see following messages:

    hwexpress service listening on port 10020
    registered with eureka:  hwexpress/localhost:hwexpress:10020

The you can access it via gateway it by:

    http --verify=../keystore/local_ca/localca.cer GET https://localhost:10010/api/v1/hwexpress/hello

## APIML Service Metadata

The function `registerServiceToDiscoveryService()` in `index.js` does the registration to the Discovery Service by calling `registerService(options)` from the `apiLayerService.js` module.

The `options` argument contains the options that are used to define the application to the Discovery Service and additional metadata for the API Catalog.

The fields are following:

- `tlsOptions` the options that are used for the [request](https://github.com/request/request#tlsssl-protocol) library to setup HTTPS connection (both server and client validation is done). The `pfx` with PKCS12 keystore and `passphrase` with the keystore passwords are recommended
- `discoveryServiceUrl` - the URL of the APIML Discovery Service - for the APIML running on your workstatition it is by default: `https://localhost:10011/eureka/apps/`
- `hostName` - the hostname of the service that is accesible by the API Gateway
- `ipAddr` - the IP address of the service that is accessible by the API Gateway
- `port` - the port on which the service is listening
- `serviceId`, `title`, `description`, 
`homePageUrl`, `statusPageUrl`, `healthCheckUrl`, `routes` - see [Configuration Parameters](https://zowe.github.io/docs-site/latest/guides/api-mediation-onboard-an-existing-rest-api-service-without-code-changes.html#configuration-parameters)
