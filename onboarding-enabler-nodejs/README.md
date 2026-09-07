## Onboarding Node.js enabler for Zowe API Mediation Layer

This is the onboarding Node.js enabler for [Zowe API Mediation Layer](https://github.com/zowe/api-layer) (part of [Zowe](https://zowe.org)) that allows to register a NodeJS based service to the API Mediation Layer Discovery Service.

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

### Discovery retries and request deadlines

The circuit breaker is enabled by default. Initial registration, heartbeats, and
registry fetches (including the initial fetch and `waitForRegistry` polling) share
one breaker. Each loop schedules its next attempt after the previous operation
completes, so slow requests do not overlap within that loop. Startup waits for
successful registration and, when enabled, the initial registry fetch; an outage
keeps startup pending while the breaker retries rather than abandoning startup.

Configure these properties under `eureka` in the client configuration:

```yaml
eureka:
  requestTimeout: 10000
  circuitBreaker:
    enabled: true
    maxFailures: 5
    backoffTimeout: 1000
    cooldownTime: 60000
    backoffMax: 300000
```

All timeout and interval values shown here are **milliseconds**.

- `requestTimeout` defaults to `10000` and must be a positive, finite number.
  Each HTTPS attempt has an absolute deadline covering connection establishment,
  TLS negotiation, and receipt of the response body. A hung connection or body
  completes with an `ETIMEDOUT` error and its request is destroyed. Errors,
  aborted responses, and late transport events cannot complete an attempt twice.
  This deadline starts at the transport stage, after cluster resolution and
  request middleware; custom resolvers and middleware must invoke their callbacks.
- In CLOSED state, consecutive failures use exponential retry delays starting at
  `backoffTimeout`, capped at `backoffMax`. An explicit zero `backoffTimeout`
  is preserved. After `maxFailures`, OPEN suspends managed requests for
  `cooldownTime`. Failed HALF_OPEN probes double subsequent OPEN cooldowns up to
  `backoffMax`; successful probes close and reset the breaker. Only one HALF_OPEN
  probe is allowed across the managed loops. A successful operation resets the
  consecutive failure count.
- With the breaker enabled, an operation does not also run the legacy transport
  retry loop: `maxRetries` and `requestRetryDelay` do not multiply its attempts.
  Transport errors and HTTP 5xx responses cause the next attempt to rotate to the
  next configured Discovery endpoint. Direct calls to `register`, `renew`, or
  `fetchRegistry` remain low-level operations; use `start()` for managed scheduling.
- A heartbeat HTTP 404 triggers re-registration and waits for that POST to finish.
  The composite operation counts once: the 404 alone is not a breaker failure,
  but a failed re-registration is. Registration HTTP 400 is a failure, not success.
- `waitForRegistry: true` polls at two-second intervals after successful empty
  registry fetches until the instance VIP is present. Failed fetches follow the
  same breaker policy as other managed operations.

Set `eureka.circuitBreaker.enabled: false` to retain legacy interval scheduling
and bounded transport retries. A transport error or HTTP 5xx permits at most
`maxRetries` additional attempts, delayed by `requestRetryDelay`, then twice that
value, then three times that value, and so on. The request deadline still applies
to each attempt; resolver failures terminate without transport retries.

`stop()` cancels scheduled work, retry timers, and in-flight transport deadlines,
and resets the breaker. Late callbacks from the stopped lifecycle cannot emit
registration/startup events, re-register on a stale 404, or restart scheduling,
even if `start()` has subsequently begun a new lifecycle. Pending startup callbacks
are discarded on stop. When registration is enabled, stop still makes the explicit
deregistration request and invokes its supplied callback with that result.

### Validation

Run `npm test` for unit tests and lint checks. The legacy Gulp/Istanbul coverage
summary may report `100% (0/0)` for ES modules; that is not meaningful coverage.
Use a V8-aware coverage runner when measuring the ES module sources.
`npm run integration` additionally requires a running Discovery Service and its TLS
fixtures. The current integration entry point uses a directory import (`../src`)
that Node.js ESM rejects with `ERR_UNSUPPORTED_DIR_IMPORT`; this must be resolved
before that integration suite can exercise Discovery.
