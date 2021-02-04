# Hello World API Service in Express

This is an example about how an API service implemented using [Node.js](https://nodejs.org/en/) and [Express](https://expressjs.com/) can be registered to the API Mediation Layer using the [apiml-onboarding-enabler-nodejs](https://www.npmjs.com/package/apiml-onboarding-enabler-nodejs) npm package. 


 [index.js](src/index.js) starts the API service implemented in Express and registers it to the Discovery service using the Node.js onboarding enabler.

 This example contains the full HTTPS validation of both Discovery Service and the Hello World service.

 The certicate, private key for the service, and the local CA certificate are loaded from `keystore/localhost/localhost.keystore.p12`.
 
 ## How to run

 You can start the service using:

    cd helloworld-expressjs
    npm install
    node index.js

If the APIML is already running then you should see following messages:

    hwexpress service listening on port 10020
    registered with eureka:  hwexpress/localhost:hwexpress:10020

The you can access it via gateway it by:

    http --verify=../keystore/local_ca/localca.cer GET https://localhost:10010/api/v1/hwexpress/hello

## Registration to the Discovery Service

The registration is performed by calling the NodeJS enabler library method `apiLayerService.connectToEureka()` in the `index.js` file.

