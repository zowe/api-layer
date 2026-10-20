## Running the API Mediation Layer on Local Machine

The API Mediation Layer can run on a z/OS system or on your local machine (Mac/Linux/Windows).

The default settings for running on local machine are stored in the [/config/local/](../config/local) directory.

`npm` is used to launch services. For Java command line, check `package.json` in root folder.

To start each server individually with default settings for running on your local machine, run the following statements.

### Build first!

```shell
./gradlew build
```

### Gateway Service

```shell
npm run gateway-service-thin
```

#### HTTPS

By default, the Gateway runs in encrypted mode (HTTPS) for northbound traffic using signed certificates in Java keystore.

To override this and run the Gateway without TLS encryption, add the following two arguments to java command:
    
    -Denvironment.scheme=http -Denvironment.sslEnabled=false

The source code contains sample certificates that are used by configuration for testing on your local machine. 
Do not use these certificates in other environments. More details can be found at [/keystore/README.md](/keystore/README.md).

#### Redirect HTTP requests on the HTTPS port

To redirect plaintext HTTP received on API ML's existing HTTPS listeners, enable:

```yaml
apiml:
  security:
    httpRedirect:
      enabled: true
```

This feature is disabled by default. It runs inside embedded Tomcat and does not
open another port. Valid HTTP/1.0 and HTTP/1.1 endpoint requests receive a
`308 Permanent Redirect`, which preserves the method and body when a client
follows the redirect. Existing percent escapes in paths and queries are preserved.
Characters explicitly permitted by Tomcat's `relaxedPathChars` or `relaxedQueryChars`
are percent-encoded in the redirect so clients can follow a valid HTTPS URI.
Plaintext requests do not reach authentication or routing.

The main listener uses `apiml.service.externalUrl` as its destination. Configure
it as an HTTPS origin (scheme, hostname and optional port, without a path, query,
fragment or user information). Invalid origins fail early with a configuration
error naming the property. If unset or empty, the destination uses
`apiml.service.hostname` and the actual bound port, including when `server.port=0`.
The Gateway and modulith supply a default external URL; set it to an empty string
to use the bound port in tests with random ports. Additional connectors use
`apiml.service.hostname` and their own port, so the modulith's Discovery listener
continues to redirect to its Discovery port. Incoming `Host` and forwarded
headers cannot change the destination.

The initial plaintext redirect does not contain HSTS. HTTPS application responses,
including authentication failures, use `CustomHstsServerHttpHeadersWriter` to
preserve any existing HSTS header, including policies from backend services.
Otherwise, the default is
`Strict-Transport-Security: max-age=31536000; includeSubDomains`. Set
`apiml.security.hsts.maxAge` (seconds, zero or greater) and
`apiml.security.hsts.includeSubDomains` to customize this fallback policy in the
reactive Gateway, modulith and API Catalog. The shared response filter also
supports AT-TLS: transport verification supplies SSL information before WebFlux,
even without a client certificate. An enabled AT-TLS flag alone is insufficient.
HSTS applies to the entire hostname, and optionally its subdomains, so backend
policies should be coordinated. Browsers preserve explicit ports other than
port 80 when upgrading a URL.

The redirect handler bounds the request line using `server.max-http-request-header-size`
and the connector's connection timeout (60 seconds if no positive timeout is configured).
Malformed targets, CONNECT requests and cleartext HTTP/2 are rejected; `OPTIONS *`
redirects to the HTTPS origin's root. TLS negotiation, including client certificate
authentication, remains handled by Tomcat.

After sending the redirect, API ML half-closes the connection and discards incoming
data without routing it. Draining uses a fixed deadline of five seconds or the
connector's connection timeout, whichever is shorter. This avoids resets caused
by immediately closing connections with uploads in progress while bounding resource
use. Clients that keep sending beyond the deadline can still receive a reset.

This feature applies to native Java TLS. When inbound AT-TLS is enabled, z/OS can
reject plaintext before it reaches API ML, so the redirect connector is not installed.
Plain HTTP mode is also unchanged. Clients should still use HTTPS directly:
redirecting cannot encrypt data already sent in the first HTTP request.

Run the redirect integration tests with:

```shell
./gradlew :apiml:test --tests 'org.zowe.apiml.acceptance.HttpsRedirectTest' -x :api-catalog-ui:build
```

The tests start API ML locally on ports 40985 and 10011 and a test backend on an
available port. They cover the HTTP redirect, following it through Gateway routing
with the original method, body and encoded URL, and HSTS on the HTTPS response.

### Discovery Service

**Note:** If you want to run the Discovery Service using IntelliJ Run Dashboard, you need to add `https` in the `Active profiles` field in `Edit Configuration`. 

```shell
npm run discovery-service-thin
```

### API Catalog

```shell
npm run api-catalog-service-thin
```

### Caching API

```shell
npm run caching-service
```

### Mock Services

```shell
npm run mock-services
```

### Sample Application - Discoverable Client

```shell
npm run discoverable-client
```

### Sample Application - onboarding-enabler-spring-sample-app

```shell
npm run onboarding-enabler-spring-sample-app
```

### Sample Application - onboarding-enabler-nodejs-sample-app

To run onboarding-enabler-nodejs-sample-app, follow the steps below:

1. Run `npm run onboarding-enabler-nodejs-sample-app` from the root project directory.

2. 2. Navigate to [https://localhost:10011]([https://localhost:10011]) and check if the service `HWEXPRESS` is registered to the Discovery Service. You should be able to reach the following endpoints using HTTPS:
   
       * [https://localhost:10039/swagger.json](https://localhost:10039/swagger.json) which contains the API documentation.
       * [https://localhost:10039/api/v1/status](https://localhost:10039/api/v1/status) for the health check endpoint containing the status of the application.
       * [https://localhost:10039/api/v1/info](https://localhost:10039/api/v1/info) for the service information such as service ID and Node.js version.
       * [https://localhost:10039/api/v1/hello](https://localhost:10039/api/v1/hello) for the greeting endpoint.
       * [https://localhost:10010/hwexpress/api/v1/hello](https://localhost:10010/hwexpress/api/v1/hello) for the greeting endpoint, routed through API Gateway.
       
       Go to the [API Catalog](https://localhost:10010/apicatalog/ui/v1) and check if the API documentation of the service is retrieved.

### Default Discovery Timing Settings 

Default timings can require up to 3 minutes for service startup, discovery, and shutdown to be registered.
To change settings so services register/de-register quicker, then append the following to the existing arguments:

```
-Deureka.instance.leaseExpirationDurationInSeconds=6 
-Deureka.instance.leaseRenewalIntervalInSeconds=5 
-Deureka.client.registryFetchIntervalSeconds=5 
-Deureka.client.initialInstanceInfoReplicationIntervalSeconds=5
```

These settings are for development purposes only, it may result in false positive or unexpected behaviour.

### Default API Catalog Refresh/Cache update Timing Settings (UI only)

To increase the service cache frequency rate which updates the Catalog UI with discovered service statuses:

```
-Dapiml.service-registry.serviceFetchDelayInMillis=10000 
-Dapiml.service-registry.cacheRefreshUpdateThresholdInMillis=10000 
-Dapiml.service-registry.cacheRefreshInitialDelayInMillis=10000 
-Dapiml.service-registry.cacheRefreshRetryDelayInMillis=10000
```

These settings are for development purposes only, it may result in false positive or unexpected behaviour.

### Debugging

To turn on debugging messages for Zowe and informational messages for third-party dependencies:

    --spring.profiles.active=dev

To turn on SSL/TLS protocol debugging:

    -Djavax.net.debug=ssl:handshake

### IntelliJ Idea setup

If your editor of choice happens to be Idea and you want to use its 'Run Dashboard' refer to [Intellij Idea Setup](ide-setup.md#intellij-idea-setup).

### Running multiple instances of Eureka locally

 To run multiple instances of Eureka from your local environment follow these steps:

 1.  Open the _hosts_ file located inside `C:\Windows\System32\drivers\etc` directory as Administrator and add one or more virtual ip – hostname entries to allow discovery peers on your laptop.

     *Example:*
         ```
         127.0.0.2       localhost1
         127.0.0.3       localhost2
         ```

     * On Mac, this can be done by editing `/etc/hosts` file as the root:

             `sudo nano /etc/hosts`

         Add:
            ```
             127.0.0.2 localhost2
             127.0.0.3 localhost3
            ```
       
         Activate:
         
             `dscacheutil -flushcache; sudo killall -HUP mDNSResponder`

         Add virtual local network adapter on Mac:

             ```
             sudo ifconfig lo0 alias 127.0.0.2
             sudo ifconfig lo0 alias 127.0.0.3
             ```
         
         It can be removed issuing: `ifconfig lo0 -alias 127.0.0.2`

 2.  Modify the [discovery-service.yml](../config/local/discovery-service.yml) and change the values of _discoveryLocations_, _hostname_, _ipAddress_ and _port_ as in the example below:

     *Example:*

     ```yaml
     environment:
         cacheRefreshDelayInMillis: 10000
         discoveryLocations: http://eureka:password@discoveryInstance1:10022/eureka,http://eureka:password@discoveryInstance2:10033/eureka
         dsIpAddress: 0.0.0.0
         eurekaPassword: password
         eurekaUserName: eureka
         hostname: discoveryInstance1
         initialCacheRefreshDelayInMillis: 10000
         ipAddress: 127.0.0.2
         port: 10022
         preferIpAddress: false
         registryServiceInitialDelayInSeconds: 10
         registryServiceRetryDelayInSeconds: 5
         truststore: keystore/local/localhost_truststore.jks
         truststorePassword: trustword
         truststoreType: JKS
         updateThresholdInMillis: 10000

     spring:
         output:
             ansi:
                 enabled: always
     ``` 

 3.  Run the _DiscoveryServiceApplication_ and once it's up, modify the *.yml* file again changing _hostname_, _ipAddress_ and _port_ with the values chosen for the other peer.

     * Without IntelliJ IDEA, you can run `npm run api-layer-multi` to run all the services from JARs. This task starts two Discovery Services with other API Mediation Layer services using configuration file in [config/local-multi](/config/local-multi/).

 4. Run _DiscoveryServiceApplication_ again.

 5.  Go to 127.0.0.**x**:**yyyyy** check the multiple instances registered with each other. If this was successful, the **registered-replicas** and **available-replicas** fields will contain the URL(s) of the other instance(s) of Eureka that you've created in the previous steps.

 **Note:** This configuration will work when the _preferIpAddress_ parameter is set to _false_. If you set it to _true_, you need to modify the value of _discoveryLocations:_ to use IP address instead of hostname, otherwise Eureka won't be able to find the services registered and as consequence the **available-replicas** will be empty.
  
   *Example:*
        ```
        discoveryLocations: http://eureka:password@127.0.0.2:10022/eureka,http://eureka:password@127.0.0.3:10033/eureka
        ```
   instead of 
       ```
       discoveryLocations: http://eureka:password@discoveryInstance1:10022/eureka,http://eureka:password@discoveryInstance2:10033/eureka
       ```

### Ports Used

   This is a list of default ports used by the project for development:

|        SERVICE         |  PORT |
|:----------------------:|:-----:|
|  Gateway               | 10010 |
|  Discovery Service     | 10011 |
|  Discoverable client   | 10012 |
|  Mock Services         | 10013 |
|  API Catalog           | 10014 |
|  EnablerJavaSampleApp  | 10016 |
