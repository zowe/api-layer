## Onboarding Node.js enabler for Zowe API Mediation Layer

This is the onboarding Node.js enabler for [Zowe API Mediation Layer](https://github.com/zowe/api-layer) (part of [Zowe](https://zowe.org)) that allows to register a NodeJS based service to the API Mediation Layer Discovery Service. It uses [eureka-js-client](https://www.npmjs.com/package/eureka-js-client).

### How to use

1. Install the onboarding Node.js enabler package as a dependency of your service:

    `npm i @zowe/apiml-onboarding-enabler-nodejs --dev-save`

2. Inside your Node.js service `index.js`, add the following code block to register your service with Eureka:

    ```js
    const apiLayerService = require("@zowe/apiml-onboarding-enabler-nodejs");
    tlsOptions = apiLayerService.getTlsOptions();
    const httpsServer = https.createServer(tlsOptions, app);
    httpsServer.listen(args.port, function () {
        apiLayerService.connectToEureka();
    });
    
    ```
   To make sure that your application will automatically unregister from Eureka once shut down, you can use the `unregisterFromEureka()` function, like shown in the example below.
   **Example:**
   
    ```js
    process.on('SIGINT', signal => {
        apiLayerService.unregisterFromEureka();
        httpsServer.close(() => {
            process.exit(0);
        });
    });   
        
    ```

3. Create a yaml file named `service-configuration.yml`, add the configuration properties and place the yaml file inside a `/config` directory at the same level of your `index.js`. 
Below is an example of the configuration.
 
    **Example:**
    
    ```yaml
    serviceId: hwexpress
    title: Hello World express REST API
    eureka:
      ssl: true
      host: localhost
      ipAddress: 127.0.0.1
      port: 10011
      servicePath: '/eureka/apps/'
      maxRetries: 30
      requestRetryDelay: 1000
      registryFetchInterval: 5
    
    
    description: Hello World REST API Service implemented in Express and Node.js
    baseUrl: https://localhost:10039/hwexpress
    homePageRelativeUrl: https://localhost:10039/
    statusPageRelativeUrl: https://localhost:10039/info
    healthCheckRelativeUrl: https://localhost:10039/status
    discoveryServiceUrls:
      - https://localhost:10011/eureka
    routes:
      - gatewayUrl: /api/v1
        serviceRelativeUrl: /api/v1
    apiInfo:
      - apiId: org.zowe.hwexpress
        gatewayUrl: "api/v1"
        swaggerUrl: https://localhost:10039/swagger.json
    catalogUiTile:
      id: cademoapps
      title: Sample API Mediation Layer Applications
      description: Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem
      version: 1.0.0
    instance:
      app: hwexpress
      vipAddress: hwexpress
      instanceId: localhost:hwexpress:10039
      homePageUrl: https://localhost:10039/
      hostName: 'localhost'
      ipAddr: '127.0.0.1'
      secureVipAddress: hwexpress
      port:
        $: 10039
        '@enabled': false
      securePort:
        $: 10039
        '@enabled': "true"
    
      dataCenterInfo:
        '@class': com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo
        name: MyOwn
      metadata:
        apiml.catalog.tile.id: 'samplenodeservice'
        apiml.catalog.tile.title: 'Zowe Sample Node Service'
        apiml.catalog.tile.description: 'NodeJS Sample service running'
        apiml.catalog.tile.version: '1.0.0'
        apiml.routes.api_v1.gatewayUrl: "api/v1"
        apiml.routes.api_v1.serviceUrl: "/api/v1"
        apiml.apiInfo.0.apiId: org.zowe.hwexpress
        apiml.apiInfo.0.gatewayUrl: "api/v1"
        apiml.apiInfo.0.swaggerUrl: https://localhost:10039/swagger.json
        apiml.service.title: 'Zowe Sample Node Service'
        apiml.service.description: 'The Proxy Server is an HTTP HTTPS, and Websocket server built upon NodeJS and ExpressJS.'
    
    ssl:
      p12File: ssl/localhost.keystore.p12
      keyPassword: password
    ```
    A certificate and private key (PEM format) configuration is also supported:
    ```yaml
    ssl:
      certificate: ssl/localhost.keystore.cer
      keystore: ssl/localhost.keystore.key
      caFile: ssl/localhost.pem
      keyPassword: password
    ```
Alternatively, you can also pass the config as a json to the client:
  ```js
      import { EurekaClient as Eureka } from '@zowe/apiml-onboarding-enabler-nodejs'
      const client = new Eureka({
      eureka: {
        ssl: true,
        host: localhost,
        port: 10011,
        servicePath: '/eureka/apps/',
        maxRetries: 2,
        registryFetchInterval: 30000,
        fetchRegistry: false,
        heartbeatInterval: 60000
      },
      instance: {
        app: hwexpress,
        instanceId: localhost:hwexpress:10039,
        hostName: 'localhost',
        ipAddr: '127.0.0.1',
        homePageUrl: https://localhost:10039/,
        secureVipAddress: hwexpress,
        port: {
          $: 10039,
          '@enabled': false
        },
        securePort: {
          $: 10039,
          '@enabled': true
        },
        dataCenterInfo: {
          '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
          name: 'MyOwn'
        },
        metadata: {
          'apiml.routes.ui-v1.gatewayUrl': 'ui',
          'apiml.routes.ui-v1.serviceUrl': '/',
          'apiml.routes.ws-v1.gatewayUrl': 'ws/ui',
          'apiml.routes.ws-v1.serviceUrl': '/'
        }
      },
      requestMiddleware: (requestOpts, done) => {
        done(Object.assign(requestOpts, tlsOptions));
      }
    }); 
  ```

4. Start your Node.js service and verify that it registers to the Zowe API Mediation Layer.
