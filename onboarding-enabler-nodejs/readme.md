## API Mediation layer enabler for Javascript

Onboarding Javascript enabler of service into [API Mediation Layer](https://github.com/zowe/api-layer) (part of [Zowe](https://zowe.org)) using [eureka-js-client](https://www.npmjs.com/package/eureka-js-client).

### Quickstart

1) Clone api mediation layer
2) Build it! `./gradlew build`
3) Run it! `npm run api-layer`

4) Clone this sample
5) Install dependencies `npm install`
6) Run it! `npm start`


### How to use

In your NodeJS service `index.js`, add the following code to register your service with Eureka:

```js
const apiLayerService = require("eureka-js-app");
tlsOptions = apiLayerService.tlsOptions;
const httpsServer = https.createServer(tlsOptions, app);
httpsServer.listen(args.port, function () {
    console.log(`${args.serviceId} service listening on port ${args.port}`);
    apiLayerService.connectToEureka();
});

```

Then add the service configuration in a yaml file named `service-configuration.yml`. 

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

description: Hello World REST API Service implemented in Express and Node.js
baseUrl: https://localhost:10020/hwexpress
homePageRelativeUrl: https://localhost:10020/
statusPageRelativeUrl: https://localhost:10020/info
healthCheckRelativeUrl: https://localhost:10020/status
discoveryServiceUrls:
  - https://localhost:10011/eureka
routes:
  - gatewayUrl: api/v1
    serviceRelativeUrl: /api/v1
apiInfo:
  - apiId: org.zowe.hwexpress
    gatewayUrl: "api/v1"
    swaggerUrl: https://localhost:10020/swagger.json
catalogUiTile:
  id: cademoapps
  title: Sample API Mediation Layer Applications
  description: Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem
  version: 1.0.0
instance:
  app: hwexpress
  vipAddress: hwexpress
  instanceId: localhost:hwexpress:10020
  homePageUrl: https://localhost:10020/
  hostName: 'localhost'
  ipAddr: '127.0.0.1'
  secureVipAddress: hwexpress
  port:
    $: 10020
    '@enabled': false
  securePort:
    $: 10020
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
    apiml.apiInfo.0.swaggerUrl: https://localhost:10020/swagger.json
    apiml.service.title: 'Zowe Sample Node Service'
    apiml.service.description: 'The Proxy Server is an HTTP HTTPS, and Websocket server built upon NodeJS and ExpressJS.'

ssl:
  certificate: ssl/localhost.keystore.cer
  keystore: ssl/localhost.keystore.key
  caFile: ssl/localhost.pem
  keyPassword: password

```

## How to test 

1. From the root project, run `npm link`. It will generate the npm package `eureka-js-app` and publish into your local `nodemodules` directory (you will see the location once the command is executed).
2. Clone the https://github.com/taban03/helloworld-expressjs repo
3. Modify the `package.json` present in the helloworld-expressjs project by setting the correct location of the `eureka-js-app` npm package.
```json
 "eureka-js-app": "file:../../../../../../usr/local/lib/node_modules/eureka-js-app",
```
4. Run `npm install` and `node index.js` to run the sample service.