# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## `1.28.23 (2023-09-27)`

* Bugfix:  Fix processing of Open API docs by API Catalog (JavaTimeModule) which was the cause of missing tiles. (#3040) ([09f5095](https://github.com/zowe/api-layer/commit/09f5095)), closes [#3040](https://github.com/zowe/api-layer/issues/3040)

## `1.28.22 (2023-06-21)`

* Bugfix:  Mitigate storing passwords in the memory (v1) (#2862) ([60293f9](https://github.com/zowe/api-layer/commit/60293f9)), closes [#2862](https://github.com/zowe/api-layer/issues/2862)
* Bugfix:  Align button in swagger UI (#2861) ([7c01a5b](https://github.com/zowe/api-layer/commit/7c01a5b)), closes [#2861](https://github.com/zowe/api-layer/issues/2861)

## `1.28.20 (2023-04-04)`

* Bugfix:  Prevent null pointer exception in Swagger UI when buffer is missing. (#2857) ([1f04b97](https://github.com/zowe/api-layer/commit/1f04b97)), closes [#2857](https://github.com/zowe/api-layer/issues/2857)

## `1.28.16 (2022-11-28)`

* Bugfix: Use the APIML_SECURITY_X509_ENABLED flag to properly disable client certificate authentication for SSO (#2645) ([5f32c09](https://github.com/zowe/api-layer/commit/5f32c09)), closes [#2645](https://github.com/zowe/api-layer/issues/2645)
* Bugfix: Enable Strict Transport Security HTTP on the Gateway service as the default (#2552) ([decf6fe](https://github.com/zowe/api-layer/commit/decf6fe)), closes [#2575](https://github.com/zowe/api-layer/issues/2552)

## `1.28.0 (2022-05-11)`

* Feature:  Enhance SAF IDT authentication scheme to support client certificates and APPL keyword (#2223) ([e3f54d2](https://github.com/zowe/api-layer/commit/e3f54d2)), closes [#2223](https://github.com/zowe/api-layer/issues/2223)
* Feature:  New endpoint to retrieve default API doc for service (#2327) ([502ba3c](https://github.com/zowe/api-layer/commit/502ba3c)), closes [#2327](https://github.com/zowe/api-layer/issues/2327)
* Feature:  Enhanced Discovery service health check (#2312) ([2f167ff](https://github.com/zowe/api-layer/commit/2f167ff)), closes [#2312](https://github.com/zowe/api-layer/issues/2312)
* Feature:  Support for TLSv1.3 (#2314) ([e96135a](https://github.com/zowe/api-layer/commit/e96135a)), closes [#2314](https://github.com/zowe/api-layer/issues/2314) [#2269](https://github.com/zowe/api-layer/issues/2269)
* Feature:  Enhance x509 authentication scheme to support client certificates (#2285) ([a053b00](https://github.com/zowe/api-layer/commit/a053b00)), closes [#2285](https://github.com/zowe/api-layer/issues/2285)
* Feature:  Enhance zowejwt authentication scheme to support client certificates (#2292) ([c602080](https://github.com/zowe/api-layer/commit/c602080)), closes [#2292](https://github.com/zowe/api-layer/issues/2292)
* Feature:  Enhance z/OSMF authentication scheme to support client certificates (#2207) ([5750072](https://github.com/zowe/api-layer/commit/5750072)), closes [#2207](https://github.com/zowe/api-layer/issues/2207)
* Feature:  Add support to change password via zOSMF (#2095) ([51e8bd3](https://github.com/zowe/api-layer/commit/51e8bd3)), closes [#2095](https://github.com/zowe/api-layer/issues/2095)
* Feature:  Enable Discovery Service and Gateway Service native library extensions (#1987) ([fd03db5](https://github.com/zowe/api-layer/commit/fd03db5)), closes [#1987](https://github.com/zowe/api-layer/issues/1987)
* Feature:  Add methods for ZaaS client to support password change (#1991) ([7597bd7](https://github.com/zowe/api-layer/commit/7597bd7)), closes [#1991](https://github.com/zowe/api-layer/issues/1991)
* Feature:  API ML sample extension (#1947) ([a085cf3](https://github.com/zowe/api-layer/commit/a085cf3)), closes [#1947](https://github.com/zowe/api-layer/issues/1947)

* Bugfix:  Add server side logging for swagger handling code (#2328) ([7b0455d](https://github.com/zowe/api-layer/commit/7b0455d)), closes [#2328](https://github.com/zowe/api-layer/issues/2328)
* Bugfix:  Preserve request cookies (#2293) ([71c6649](https://github.com/zowe/api-layer/commit/71c6649)), closes [#2293](https://github.com/zowe/api-layer/issues/2293) [#2269](https://github.com/zowe/api-layer/issues/2269)
* Bugfix:  ZaaS client compatibility with Zowe v2 (#2227) ([abdf995](https://github.com/zowe/api-layer/commit/abdf995)), closes [#2227](https://github.com/zowe/api-layer/issues/2227)
* Bugfix:  Add BearerContent filter to enable bearer auth (#2197) ([1d41704](https://github.com/zowe/api-layer/commit/1d41704)), closes [#2197](https://github.com/zowe/api-layer/issues/2197)
* Bugfix:  Configure southbound timeout with APIML_GATEWAY_TIMEOUT_MILLIS (#2154) ([6af5d6f](https://github.com/zowe/api-layer/commit/6af5d6f)), closes [#2154](https://github.com/zowe/api-layer/issues/2154)
* Bugfix:  Improve error handling for API diff endpoint (#2178) ([1581e39](https://github.com/zowe/api-layer/commit/1581e39)), closes [#2178](https://github.com/zowe/api-layer/issues/2178)
* Bugfix:  Update data model for infinispan storage in Caching service (#2156) ([38a1348](https://github.com/zowe/api-layer/commit/38a1348)), closes [#2156](https://github.com/zowe/api-layer/issues/2156)
* Bugfix:  Versioning in image publishing workflow (#2159) ([db52527](https://github.com/zowe/api-layer/commit/db52527)), closes [#2159](https://github.com/zowe/api-layer/issues/2159)
* Bugfix:  Add x509 auth info to gw api doc (#2142) ([0205470](https://github.com/zowe/api-layer/commit/0205470)), closes [#2142](https://github.com/zowe/api-layer/issues/2142)
* Bugfix:  Properly remove services when instances are removed from Discovery Service (#2128) ([c675b91](https://github.com/zowe/api-layer/commit/c675b91)), closes [#2128](https://github.com/zowe/api-layer/issues/2128)
* Bugfix:  Use ribbon LB for Web sockets (#2147) ([4751dbc](https://github.com/zowe/api-layer/commit/4751dbc)), closes [#2147](https://github.com/zowe/api-layer/issues/2147)
* Bugfix:  Add missing fields in error response (#2118) ([3b9745c](https://github.com/zowe/api-layer/commit/3b9745c)), closes [#2118](https://github.com/zowe/api-layer/issues/2118)
* Bugfix:  Do not require keyAlias for SSL configuration (#2110) ([03bee79](https://github.com/zowe/api-layer/commit/03bee79)), closes [#2110](https://github.com/zowe/api-layer/issues/2110)
* Bugfix:  Add log masking class for sensitive logs (#2003) ([994b483](https://github.com/zowe/api-layer/commit/994b483)), closes [#2003](https://github.com/zowe/api-layer/issues/2003)

## `1.27.0 (2022-01-14)`

* Feature:  Enable Discovery Service class path extensions and Gateway native library extensions (#1987) ([fd03db5](https://github.com/zowe/api-layer/commit/fd03db5)), closes [#1987](https://github.com/zowe/api-layer/issues/1987)
* Feature:  Add Zaas methods for password change support (#1991) ([7597bd7](https://github.com/zowe/api-layer/commit/7597bd7)), closes [#1991](https://github.com/zowe/api-layer/issues/1991)
* Feature:  Create API ML sample extension. This extension contains a sample controller (#1947) ([a085cf3](https://github.com/zowe/api-layer/commit/a085cf3)), closes [#1947](https://github.com/zowe/api-layer/issues/1947)
* Feature:  remove jwtsecret from usage (#1976) ([62e9d1d](https://github.com/zowe/api-layer/commit/62e9d1d)), closes [#1976](https://github.com/zowe/api-layer/issues/1976)


* Bugfix:  Build conformant images properly (#2009) ([5f07073](https://github.com/zowe/api-layer/commit/5f07073)), closes [#2009](https://github.com/zowe/api-layer/issues/2009)
* Bugfix:  Add log masking class for sensitive logs (#2003) ([994b483](https://github.com/zowe/api-layer/commit/994b483)), closes [#2003](https://github.com/zowe/api-layer/issues/2003)

## `1.26.0 (apiml: 1.26.16 2021-13-12)`

* Feature: Enable hystrix metrics stream for core APIML services (#1899) ([0734f4d8](https://github.com/zowe/api-layer/commit/0734f4d8)), closes [#1858](https://github.com/zowe/api-layer/issues/1858)

* Bugfix: The correct key from the keystore by alias is now chosen, rather than the first key certificate pair returned from the keyring (#1939) ([6ea7a62](https://github.com/zowe/api-layer/commit/6ea7a62)), closes [#1939](https://github.com/zowe/api-layer/issues/1939)
* Bugfix:  The Metrics service connection is now released when the stream is changed, thereby stopping leaky connections that fill connection pools and memory (#1931) ([5dcf55e](https://github.com/zowe/api-layer/commit/5dcf55e)), closes [#1931](https://github.com/zowe/api-layer/issues/1931)
* Bugfix:  Removes duplicated hystrix streams in Metrics Service dashboard (#1924) ([4dfd4e1](https://github.com/zowe/api-layer/commit/4dfd4e1)), closes [#1924](https://github.com/zowe/api-layer/issues/1924)
* Bugfix:  Fixes transformation of swagger server URLs in API Catalog to be the location of the current browser location (#1934) ([1b8844c](https://github.com/zowe/api-layer/commit/1b8844c)), closes [#1934](https://github.com/zowe/api-layer/issues/1934)
* Bugfix:  Adds proper icons to Metrics Service (#1912) ([517105f](https://github.com/zowe/api-layer/commit/517105f)), closes [#1912](https://github.com/zowe/api-layer/issues/1912)
* Bugfix:  Disables hystrix timeout (#1906) ([7fb1301](https://github.com/zowe/api-layer/commit/7fb1301)), closes [#1906](https://github.com/zowe/api-layer/issues/1906)
* Bugfix:  Fixes the GW start script (#1898) ([a4363ad](https://github.com/zowe/api-layer/commit/a4363ad)), closes [#1898](https://github.com/zowe/api-layer/issues/1898)
* Bugfix:  Removes hardcoded values from the Metrics Service UI, thereby facilitating PoC work (#1902) ([bd6f8d2](https://github.com/zowe/api-layer/commit/bd6f8d2)), closes [#1902](https://github.com/zowe/api-layer/issues/1902)
* Bugfix:  The Metrics Service now verifies certificates enabling the service to register to the Discovery Service properly (#1868) ([8fcf46c](https://github.com/zowe/api-layer/commit/8fcf46c)), closes [#1868](https://github.com/zowe/api-layer/issues/1868)
* Bugfix:  The API Catalog now uses the URL from the browser window to access swagger, thereby preventing failure when using a kubernetes environment (#1841) ([37cbfbc](https://github.com/zowe/api-layer/commit/37cbfbc)), closes [#1841](https://github.com/zowe/api-layer/issues/1841)
* Bugfix:  Displays the correct error message when the wrong jwtConfigurationMode is used (#1830) ([990426d](https://github.com/zowe/api-layer/commit/990426d)), closes [#1830](https://github.com/zowe/api-layer/issues/1830)
* Bugfix:  Handles the JWT token expiration correctly (#1836) ([90a887d](https://github.com/zowe/api-layer/commit/90a887d)), closes [#1836](https://github.com/zowe/api-layer/issues/1836)
* Bugfix:  SSE no longer adds a trailing slash to an endpoint provided after the Gateway route (#1839) ([5f7ba56](https://github.com/zowe/api-layer/commit/5f7ba56)), closes [#1839](https://github.com/zowe/api-layer/issues/1839)

## `1.25.0 (apiml: 1.25.4 2021-10-25)`

* Feature:  Add controller for public key provisioning. This feature makes it possible to retrieve public keys to verify JWT tokens. (#1824) ([5acb9e9](https://github.com/zowe/api-layer/commit/5acb9e9)), closes [#1824](https://github.com/zowe/api-layer/issues/1824)
* Feature:  Per service configuration to direct the API Gateway to add headers. This feature enables APIML to add or override headers in responses.  (#1812) ([25bbdbe](https://github.com/zowe/api-layer/commit/25bbdbe)), closes [#1812](https://github.com/zowe/api-layer/issues/1812)
* Feature:  Per service configuration to ignore certain headers on the API Gateway. This feature makes it possible to select which headers are stripped from requests that go through the Gateway to a particular service. (#1806) ([b258732](https://github.com/zowe/api-layer/commit/b258732)), closes [#1806](https://github.com/zowe/api-layer/issues/1806)
* Feature:  Certificate authentication for static API refresh endpoint. This feature makes it possible to authenticate via certificates for the static refresh endpoint in the API Catalog. (#1782) ([d4a91b0](https://github.com/zowe/api-layer/commit/d4a91b0)), closes [#1782](https://github.com/zowe/api-layer/issues/1782)
* Feature:  Server Sent Events Handler for accessing Turbine SSE events through API Gateway. This feature enables the Gateway to route server-sent events. (#1723) ([9bea501](https://github.com/zowe/api-layer/commit/9bea501)), closes [#1723](https://github.com/zowe/api-layer/issues/1723)
* Feature:  Service onboarding Wizard improvements (#1772) ([20dd70b](https://github.com/zowe/api-layer/commit/20dd70b)), closes [#1772](https://github.com/zowe/api-layer/issues/1772)
* Feature:  Controllers are now provided which makes it  possible to delete the static definition file. (#1759) ([e4c22dc](https://github.com/zowe/api-layer/commit/e4c22dc)), closes [#1759](https://github.com/zowe/api-layer/issues/1759)
* Feature:  Provide compression for specific paths only. It is now possible to delegate compression of a response to the API ML while limiting the paths that are compressed. (#1755) ([cc612e5](https://github.com/zowe/api-layer/commit/cc612e5)), closes [#1755](https://github.com/zowe/api-layer/issues/1755)
* Feature:  Static API enpoints protected by SAF check. Static API definition files are now only available to authorized users. (#1764) ([e2d95df](https://github.com/zowe/api-layer/commit/e2d95df)), closes [#1764](https://github.com/zowe/api-layer/issues/1764)
* Feature:  Static Definition creation endpoints in API Catalog. Controllers are now provided which make it possible to either create or override the static definition file. (#1735) ([2976db5](https://github.com/zowe/api-layer/commit/2976db5)), closes [#1735](https://github.com/zowe/api-layer/issues/1735)
* Feature:  Service onboarding Wizard Automatic registration of static definition. Static definitions now can be automatically onboarded. (#1751) ([3228249](https://github.com/zowe/api-layer/commit/3228249)), closes [#1751](https://github.com/zowe/api-layer/issues/1751)
* Feature:  Service onboarding Wizard can now produce NodeJS & Micronaut enablers configurations. Configuration files have been added for the NodeJS and Micronaut onboarding methods. (#1733) ([1e077e8](https://github.com/zowe/api-layer/commit/1e077e8)), closes [#1733](https://github.com/zowe/api-layer/issues/1733)
* Feature:  Service onboarding Wizard UX tweaks have been introduced to improve Wizard usablity. (#1752) ([47c5414](https://github.com/zowe/api-layer/commit/47c5414)), closes [#1752](https://github.com/zowe/api-layer/issues/1752)


* Bugfix: Add handling in case of PassTicketException (#1810) ([f962361](https://github.com/zowe/api-layer/commit/f962361)), closes [#1810](https://github.com/zowe/api-layer/issues/1810)
* Bugfix: Add https://${ZOWE_EXTERNAL_HOST}:${ZWE_EXTERNAL_PORT} to ZAF API Catalog pluign definition (#1829) ([6527a32](https://github.com/zowe/api-layer/commit/6527a32)), closes [#1829](https://github.com/zowe/api-layer/issues/1829)
* Bugfix: api-catalog app failed to load in desktop if Gateway service is registered as NodePort (#1827) ([ec45915](https://github.com/zowe/api-layer/commit/ec45915)), closes [#1827](https://github.com/zowe/api-layer/issues/1827)
* Bugfix: Add ZAF pluginDefinition.json to API Catalog package (#1822) ([4745548](https://github.com/zowe/api-layer/commit/4745548)), closes [#1822](https://github.com/zowe/api-layer/issues/1822)
* Bugfix: Define appfwPlugins instead of desktopIframePlugins for API Catalog ZAF plugin definition (#1814) ([371cc0b](https://github.com/zowe/api-layer/commit/371cc0b)), closes [#1814](https://github.com/zowe/api-layer/issues/1814)
* Bugfix: Periodically update the Gateway url during Gateway Lookup (#1817) ([7016ea5](https://github.com/zowe/api-layer/commit/7016ea5)), closes [#1817](https://github.com/zowe/api-layer/issues/1817)
* Bugfix:  PJE enabler sample and password validation fixes (#1819) ([45a4001](https://github.com/zowe/api-layer/commit/45a4001)), closes [#1819](https://github.com/zowe/api-layer/issues/1819)
* Bugfix: Display multiple APIs with multiple different versions in API catalog (#1800) ([6400aa3](https://github.com/zowe/api-layer/commit/6400aa3)), closes [#1800](https://github.com/zowe/api-layer/issues/1800)
* Bugfix: Handle API Catalog errors during parsing apiversions and wrong URLs in service definition (#1788) ([7a0346f](https://github.com/zowe/api-layer/commit/7a0346f)), closes [#1788](https://github.com/zowe/api-layer/issues/1788)
* Bugfix: Support for ZOSMF APAR PH34201 (#1795) ([5503e4b](https://github.com/zowe/api-layer/commit/5503e4b)), closes [#1795](https://github.com/zowe/api-layer/issues/1795)
* Bugfix: Fix flaky unit test (#1771) ([58d9656](https://github.com/zowe/api-layer/commit/58d9656)), closes [#1771](https://github.com/zowe/api-layer/issues/1771)
* Bugfix: API Catalog static definition file generation (#1761) ([b6790cb](https://github.com/zowe/api-layer/commit/b6790cb)), closes [#1761](https://github.com/zowe/api-layer/issues/1761)
* Bugfix: ZAAS client to not send a certificate during authentication requests (#1763) ([691036a](https://github.com/zowe/api-layer/commit/691036a)), closes [#1763](https://github.com/zowe/api-layer/issues/1763)

## `1.24.0 (apiml: 1.24.4 2021-09-01)`

* Feature:  SAF resource access controller that allows the authorized user to query the SAF resource access levels (#1734) ([680f4b9](https://github.com/zowe/api-layer/commit/680f4b9)), closes [#1734](https://github.com/zowe/api-layer/issues/1734)
* Feature:  Compress routed response with GZIP based on the routed service's preference (#1728) ([85b5948](https://github.com/zowe/api-layer/commit/85b5948)), closes [#1728](https://github.com/zowe/api-layer/issues/1728)
* Feature:  Introduce REST provider for SAF IDT tokens (#1714) ([047c54e](https://github.com/zowe/api-layer/commit/047c54e)), closes [#1714](https://github.com/zowe/api-layer/issues/1714)
* Feature:  Refresh endpoint for prolonging a valid JWT token (#1719) ([f918916](https://github.com/zowe/api-layer/commit/f918916)), closes [#1719](https://github.com/zowe/api-layer/issues/1719)
* Feature:  Introduce error codes for SAF authentication (#1692) ([89ac5a2](https://github.com/zowe/api-layer/commit/89ac5a2)), closes [#1692](https://github.com/zowe/api-layer/issues/1692)
* Feature:  Support SAF IDT as the authentication scheme. The application now properly recognizes the scheme and fills the X-SAF-Token header with the token produced by the `SafAuthenticationService` (#1688) ([26a84c0](https://github.com/zowe/api-layer/commit/26a84c0)), closes [#1688](https://github.com/zowe/api-layer/issues/1688)
* Feature:  Conditional full debug logging for API Mediation Layer (#1662) ([81bc46f](https://github.com/zowe/api-layer/commit/81bc46f)), closes [#1662](https://github.com/zowe/api-layer/issues/1662)
* Feature:  AT-TLS aware API Mediation Layer. API ML now provides the same functionality to clients when TLS is handled by the z/OS Communication Server. (#1621) ([be26a9a](https://github.com/zowe/api-layer/commit/be26a9a)), closes [#1621](https://github.com/zowe/api-layer/issues/1621)
* Feature:  Distributed authentication based load balancing (#1602) ([4e7d993](https://github.com/zowe/api-layer/commit/4e7d993)), closes [#1602](https://github.com/zowe/api-layer/issues/1602)
* Feature:  Add support for stomp v1.2 websocket protocol (#1697) ([d1057e0](https://github.com/zowe/api-layer/commit/d1057e0)), closes [#1697](https://github.com/zowe/api-layer/issues/1697)


* Bugfix:  Configurable timeout to verify the zOSMF availability (#1738) ([82fa3bf](https://github.com/zowe/api-layer/commit/82fa3bf)), closes [#1738](https://github.com/zowe/api-layer/issues/1738)
* Bugfix:  Exclude tls_rsa ciphers for Websocket client (#1737) ([df013bf](https://github.com/zowe/api-layer/commit/df013bf)), closes [#1737](https://github.com/zowe/api-layer/issues/1737)
* Bugfix:  Hide log messages with no value (#1744) ([50cff16](https://github.com/zowe/api-layer/commit/50cff16)), closes [#1744](https://github.com/zowe/api-layer/issues/1744)
* Bugfix:  PerServiceGzipFilter checks for correct URI pattern (#1732) ([b7ae93f](https://github.com/zowe/api-layer/commit/b7ae93f)), closes [#1732](https://github.com/zowe/api-layer/issues/1732)
* Bugfix:  Add agentkeepalive dependency to Catalog frontend because of failing build (#1698) ([7f6db61](https://github.com/zowe/api-layer/commit/7f6db61)), closes [#1698](https://github.com/zowe/api-layer/issues/1698)
* Bugfix:  Correctly handle websocket connections for new path pattern (#1701) ([2db9a7e](https://github.com/zowe/api-layer/commit/2db9a7e)), closes [#1701](https://github.com/zowe/api-layer/issues/1701)
* Bugfix:  Prevent API ML run with unsecure connection in AT-TLS mode (#1689) ([2139218](https://github.com/zowe/api-layer/commit/2139218)), closes [#1689](https://github.com/zowe/api-layer/issues/1689)
* Bugfix:  Accommodating keystore parameters with spaces (#1676) ([a784d26](https://github.com/zowe/api-layer/commit/a784d26)), closes [#1676](https://github.com/zowe/api-layer/issues/1676)
* Bugfix:  Discovery health and info endpoints: authentication and enablement (#1612) ([b52d076](https://github.com/zowe/api-layer/commit/b52d076)), closes [#1612](https://github.com/zowe/api-layer/issues/1612)
* Bugfix:  Serve swagger url over http in Gateway HTTP mode (#1646) ([3b35a10](https://github.com/zowe/api-layer/commit/3b35a10)), closes [#1646](https://github.com/zowe/api-layer/issues/1646)
* Bugfix:  Load balancer remote cache fixes (#1636) ([129b33c](https://github.com/zowe/api-layer/commit/129b33c)), closes [#1636](https://github.com/zowe/api-layer/issues/1636)
* Bugfix:  QueryTest typo in class name (#1634) ([a040847](https://github.com/zowe/api-layer/commit/a040847)), closes [#1634](https://github.com/zowe/api-layer/issues/1634)
* Bugfix:  Reuse jetty client (#1658) ([11d575a](https://github.com/zowe/api-layer/commit/11d575a)), closes [#1658](https://github.com/zowe/api-layer/issues/1658)
* Bugfix:  Publish spring onboarding enabler in format consistent with other projects (#1608) ([dac35b1](https://github.com/zowe/api-layer/commit/dac35b1)), closes [#1608](https://github.com/zowe/api-layer/issues/1608)

## `1.23.0 (2021-07-26)`

* Feature:  Authentication based server-side load balancing. A service can now configure itself with the Authentication based load balancing scheme whereby a user is directed to the same instance of a service for a given period of time (#1576) ([4ad382e](https://github.com/zowe/api-layer/commit/4ad382e)), closes [#1576](https://github.com/zowe/api-layer/issues/1576).
* Feature:  Catalog: authenticate with client certificate for /apidoc/** endpoints (#1568) ([79dedfd](https://github.com/zowe/api-layer/commit/79dedfd)), closes [#1568](https://github.com/zowe/api-layer/issues/1568)
* Feature:  Gateway: authenticate with client certificate for /gateway/services/** endpoint (#1568) ([d0d7b0af92de20fe606076e90f48604018cdf099](https://github.com/zowe/api-layer/commit/d0d7b0af92de20fe606076e90f48604018cdf099)), closes [#1568](https://github.com/zowe/api-layer/issues/1568)
* Feature:  Make cookie samesite configurable. The configuration parameter `apiml.auth.cookieProperties.cookieSameSite` makes it possible for users to configure the SameSite attribute of the `apimlAuthenticationToken` cookie (#1545) ([135904c](https://github.com/zowe/api-layer/commit/135904c)), closes [#1545](https://github.com/zowe/api-layer/issues/1545)
* Feature:  Services can now configure their desired load balancer behavior (#1536) ([db0c070](https://github.com/zowe/api-layer/commit/db0c070)), closes [#1536](https://github.com/zowe/api-layer/issues/1536)


* Bugfix:  Fix login error handling when auth service is not available (#1579) ([1221573](https://github.com/zowe/api-layer/commit/1221573)), closes [#1579](https://github.com/zowe/api-layer/issues/1579)
* Bugfix:  Handle multiple discovery service instances in static refresh (#1582) ([d7237ce](https://github.com/zowe/api-layer/commit/d7237ce)), closes [#1582](https://github.com/zowe/api-layer/issues/1582)
* Bugfix:  Improve redis configuration for users (#1589) ([b1f7088](https://github.com/zowe/api-layer/commit/b1f7088)), closes [#1589](https://github.com/zowe/api-layer/issues/1589)
* Bugfix:  Properly display API Catalog status on Gateway homepage (#1581) ([b8dd9cd](https://github.com/zowe/api-layer/commit/b8dd9cd)), closes [#1581](https://github.com/zowe/api-layer/issues/1581)
* Bugfix:  Change order of authentication filters on Login endpoint (#1526) ([3b93e9b](https://github.com/zowe/api-layer/commit/3b93e9b)), closes [#1526](https://github.com/zowe/api-layer/issues/1526)
* Bugfix:  Wrong use of certificates in ZAAS client (#1514) ([964c4fa](https://github.com/zowe/api-layer/commit/964c4fa)), closes [#1514](https://github.com/zowe/api-layer/issues/1514)

## `1.22.0 (2021-06-14)`

* Feature: Deterministic routing based on the provided headers is now available. Clients can now specify which instance of a service the user should be routed to. This enables reusability of underlying resources such as LPARs associated with a specific service instance (#1496) ([ed91f25](https://github.com/zowe/api-layer/commit/ed91f25)), closes [#1496](https://github.com/zowe/api-layer/issues/1496).
* Feature: Basic authentication via Websocket is now fully supported (#1482) ([112da99](https://github.com/zowe/api-layer/commit/112da99)), closes [#1482](https://github.com/zowe/api-layer/issues/1482).
* Feature: Passwords can be changed via SAF. An endpoint is exposed allowing users to change passwords using this API ML endpoint (#1471) ([3f3c2af](https://github.com/zowe/api-layer/commit/3f3c2af)), closes [#1471](https://github.com/zowe/api-layer/issues/1471).
* Feature: A self-service application is now available that can run in the infrastructure of the user to verify whether certificates are properly created and configured (#1441) ([e694c0f](https://github.com/zowe/api-layer/commit/e694c0f)), closes [#1441](https://github.com/zowe/api-layer/issues/1441)


* Bugfix: Use the apiml.service.id in the API Catalog as used in other services. (#1475) ([7bc8f99](https://github.com/zowe/api-layer/commit/7bc8f99)), closes [#1475](https://github.com/zowe/api-layer/issues/1475)
* Bugfix: Change the registration to use the correct hostname in `instanceId` (#1473) ([1d6caa8](https://github.com/zowe/api-layer/commit/1d6caa8)), closes [#1473](https://github.com/zowe/api-layer/issues/1473)
* Bugfix: The HTTP client is not closed when generating a passticket. The ZAAS client can now reuse connections and provide correct login with passtickets (#1470) ([ed9f929](https://github.com/zowe/api-layer/commit/ed9f929)), closes [#1470](https://github.com/zowe/api-layer/issues/1470).
* Bugfix: Configurable jwt alias at startup via environment variable (#1442) ([0e3df7a](https://github.com/zowe/api-layer/commit/0e3df7a)), closes [#1442](https://github.com/zowe/api-layer/issues/1442)
* Bugfix: Use the actual hostname instead of the one provided by Spring Cloud (#1434) ([6b8c38a](https://github.com/zowe/api-layer/commit/6b8c38a)), closes [#1434](https://github.com/zowe/api-layer/issues/1434)
* Bugfix: Distinguish lib and fat jars (#1398) ([f771a40](https://github.com/zowe/api-layer/commit/f771a40)), closes [#1398](https://github.com/zowe/api-layer/issues/1398)
* Bugfix: Accept list of Discovery services in the Catalog. If the Catalog fails to contact to the Discovery service, the Catalog tries to contact another service from the list (#1376) ([42ae70d](https://github.com/zowe/api-layer/commit/42ae70d)), closes [#1376](https://github.com/zowe/api-layer/issues/1376)

## `1.21.1 (2021-04-26)`

* Feature: Configuration of the API ML run is now permitted where the hostname in the certificate is not verified in a strict manner. The certificate Common Name or Subject Alternate Name (SAN) are NOT checked. This facilitates deployment to Marist when certificates are valid but do not contain a valid hostname. This is for development purposes only and should not be used for production. (#1334) ([2da761a](https://github.com/zowe/api-layer/commit/2da761a)), closes [#1334](https://github.com/zowe/api-layer/issues/1355) 
* Feature: Caching service: The alphanumeric constraint for keys stored in the service has been removed. (#1317) ([237420](https://github.com/zowe/api-layer/commit/23742017fb37815dc40b5e7c8645acfac5a92ccb))
* Feature: An endpoint has been added to delete all keys for a specific service (#1253) ([0c3e01](https://github.com/zowe/api-layer/commit/0c3e01900ea646bd959472bae3bd9c1fbd7d3e31)), closes [1253](https://github.com/zowe/api-layer/issues/1253)

* Bugfix: Stop leaking X-Certificate headers (#1328) ([b2737a](https://github.com/zowe/api-layer/commit/b2737a921bb543f7b6865739b8a618cca72691e3))
* Bugfix: Remove the wait from start.sh to reduce address spaces (#1335) ([2ba780](https://github.com/zowe/api-layer/commit/2ba7803902d7796518cf1c9a5806b9c81b7360bb))
* Bugfix: Make the version endpoint available at the URL: /application/version (#1312) ([0ac95a4](https://github.com/zowe/api-layer/commit/0ac95a41333e3b13dd7dedfd147a7c24d5d3088f))
* Bugfix: Load the JWT secret properly when concurrently loaded and requested (#1255) ([1644a8c](https://github.com/zowe/api-layer/commit/1644a8c)), closes [#1255](https://github.com/zowe/api-layer/issues/1255) 
* Bugfix: Swagger v2 yaml parsed and rendered (#1269) ([a1f2cc0](https://github.com/zowe/api-layer/commit/a1f2cc0c3580e6d36a878e0fff23b943857b38e4)), closes [1229](https://github.com/zowe/api-layer/issues/1229)


## `1.20.0 (2021-03-11)`
* Bugfix (authentication): Support specific z/OSMF version. This fix allows the user to force the authentication token that is used. (#1241) ([2da761a](https://github.com/zowe/api-layer/commit/2da761a)), closes [#1241](https://github.com/zowe/api-layer/issues/1241)
* Bugfix (authentication): Ignore wrong or non-existing SAF classes when SAF is not used (#1216) ([c5ea311](https://github.com/zowe/api-layer/commit/c5ea311)), closes [#1216](https://github.com/zowe/api-layer/issues/1216)
* Bugfix (enabler): Add unregistration method to the the Node.js enabler. (#1214) ([1ecd5c7](https://github.com/zowe/api-layer/commit/1ecd5c7)), closes [#1214](https://github.com/zowe/api-layer/issues/1214)
* Feature: x509 authentication scheme. This feature supports authentication with a client certificate in southbound services whereby users can decide which part of the certificate to use. (#1208) ([94dbf37](https://github.com/zowe/api-layer/commit/94dbf37)), closes [#1208](https://github.com/zowe/api-layer/issues/1208)
* Feature: Add NodeJS sample service and enabler. This feature makes it possible for a service based on NodeJS to register with API ML in a similar way as with other onboarding enablers. (#1140) ([c86a289](https://github.com/zowe/api-layer/commit/c86a289)), closes [#1140](https://github.com/zowe/api-layer/issues/1140)
* Feature: Allow Zowe to run without a jwtsecret if the jwtsecret is not required. (#1203) ([7dc6dad](https://github.com/zowe/api-layer/commit/7dc6dad)), closes [#1203](https://github.com/zowe/api-layer/issues/1203)
* Bugfix: Enable /api/v1/gateway path format for the `/auth/logout`, `/auth/login`, `/auth/query`, and `/auth/passticket` endpoints (#1126) ([13ac9a5](https://github.com/zowe/api-layer/commit/13ac9a5)), closes [#1126](https://github.com/zowe/api-layer/issues/1126)
* Feature: Introduce token validation providers. This feature provides a future mechanism of token validation whereby custom endpoint can be provided that requires authentication. (#1142) ([80cc790](https://github.com/zowe/api-layer/commit/80cc790)), closes [#1142](https://github.com/zowe/api-layer/issues/1142)
* Feature: Reject eviction strategy has been added to VSAM. If storage is full, and `ZWE_CACHING_EVICTION_STRATEGY` is set to `reject` this feature prevents the Caching Service from removing entries, but returns a status code 507 with the message, “Insufficient storage space limit”. (#1112) ([70c2d71](https://github.com/zowe/api-layer/commit/70c2d71)), closes [#1112](https://github.com/zowe/api-layer/issues/1112) ([80cc790](https://github.com/zowe/api-layer/commit/80cc790)), closes [#1142](https://github.com/zowe/api-layer/issues/1142)
* Feature: Base information (SSO, API ID) about a service is now displayed in the API Catalog. (#1116) ([4b61377](https://github.com/zowe/api-layer/commit/4b61377)), closes [#1116](https://github.com/zowe/api-layer/issues/1116) [#1116](https://github.com/zowe/api-layer/issues/1116)
* Bugfix: Accept `swagger/openapi` in yaml format (#1202) ([0c412b0](https://github.com/zowe/api-layer/commit/0c412b0)), closes [#1202](https://github.com/zowe/api-layer/issues/1202)
* Feature (Caching Service): Production logging for the Caching Service. This feature limits messages sent to Spool to the bare minimum, thereby improving the information returned to the caller. (#1185) ([7adffb1](https://github.com/zowe/api-layer/commit/7adffb1)), closes [#1185](https://github.com/zowe/api-layer/issues/1185)

## `1.19.0`

- Feature: The connection limit of the Gateway has been configured to support multiple long-running requests by service. [#843](https://github.com/zowe/api-layer/issues/843)
- Feature: The size of API Mediation Layer has been reduced to fit within 150MB. [#909](https://github.com/zowe/api-layer/issues/909)
- Feature: You can now remove or configure the Catalog from appearing on the Gateway homepage [#727](https://github.com/zowe/api-layer/issues/727)
- Bugfix: API ID is not sent to Eureka in metadata by the Java enabler [#991](https://github.com/zowe/api-layer/issues/991)
- Feature: Connection limits have been enhanced to improve latency times when making requests through the API ML. This feature also enables concurrent requests. [#987](https://github.com/zowe/api-layer/issues/987)
- Feature: The connection limit log messages hae been enhanced. New messages indicate when too many connections occur. [#987](https://github.com/zowe/api-layer/issues/987)
- Bugfix: Fixed tcp connections that are stuck open. [#1009](https://github.com/zowe/api-layer/issues/1009)
- Feature: The `/api/v1/gateway/services/{serviceId}` endpoint has been added which provides information about a service in API ML for API clients. You can now view information to choose the applicable available API service without having a trusted service certificate. Proper SAF authorization is required. [#873](https://github.com/zowe/api-layer/issues/873)
- Feature: The size limitation in the InMemory cache for proper handling is now supported when size limitations are reached.  [#998](https://github.com/zowe/api-layer/issues/998)
- Feature: The 'Remove Oldest' eviction mechanism for Caching Service has been implemented to limit the volume of data in the cache.[#998](https://github.com/zowe/api-layer/issues/998)
- Feature: Configure CORS origins per service has been configured so that onboarded services can request to delegate CORS behavior for a route to the API Mediation Layer. [#997](https://github.com/zowe/api-layer/issues/997)
- Feature: The 'Reject eviction' strategy to Caching service has been implemented to limit the volume of data in the cache.[#998](https://github.com/zowe/api-layer/issues/998)
- Feature: Debug logging to x509 Client certificate authentication classes has been added. This feature enables users to determine the cause of system problems during client certificate authentication setup.

## `1.18.0`

- Feature: Configure more detailed logging outside of Spool. [#709](https://github.com/zowe/api-layer/issues/709)
- Feature: HA: Componentize the start script per API ML service. Individual API Mediation Layer components can be launched and restarted individually. [#862](https://github.com/zowe/api-layer/issues/862)
- Feature: HA: It is now possible to distinguish between internal and external traffic through port separation, whereby each port uses a unique certificate; one presenting an internal certificate, and the other an external certificate. [#910](https://github.com/zowe/api-layer/issues/910)
- Feature: API version is now automatically set to the version tab selected in the API Catalog so users can easily grab the Base Path. [#943](https://github.com/zowe/api-layer/issues/943)
- Feature: A new enhancement to the API Catalog versioning has been introduced with the addition of the API differences tab. This feature enables users to compare versions of two APIs. [#923](https://github.com/zowe/api-layer/issues/923)
- Bugfix: ZaasJwtService enhancement on JWT parsing and error handling. [#897](https://github.com/zowe/api-layer/issues/897)
- Bugfix: Upgrade dependencies for the Enablers [#933](https://github.com/zowe/api-layer/issues/933)

## `1.17.0`

- Feature: Multiple versions of one API are now presented in the Catalog if configured to do so. Users can now switch between different versions within the Catalog to see differences in API documentation between versions. [#844](https://github.com/zowe/api-layer/issues/844)
- Feature: Setting `APIML_DEBUG_MODE_ENABLED` in `instance.env` is properly passed on to the all API ML services. [#901](https://github.com/zowe/api-layer/issues/901)
- Bugfix: Improved returned information while logging out via logout on Gateway. [#831](https://github.com/zowe/api-layer/issues/831) 
- Bugfix: Updated API paths for the API ML in the API Catalog to use the service id in front. [#853](https://github.com/zowe/api-layer/issues/853) 

## `1.16.0`

- Feature: ZAAS Client can now use HTTP so that the Application Transparent Transport Layer Security (AT-TLS) can be used for communication to ZAAS. [#813](https://github.com/zowe/api-layer/issues/813)
- Feature: Implemented the logout functionality in ZAAS Client. [#808](https://github.com/zowe/api-layer/issues/808)
- Feature: Added a more helpful and actionable description to message ZWEAM511E, which occurs when API ML does not trust the certificate provided by the service. [#818](https://github.com/zowe/api-layer/issues/818)
- Bugfix: Changed the default expiration time value for JWT token to 8h for consistency with the z/OSMF default. [#615](https://github.com/zowe/api-layer/issues/615)
- Bugfix: Reduced excessive and unhelpful log messages. [#672](https://github.com/zowe/api-layer/issues/672)
- Bugfix: Added the Base Path field in the API Catalog if one is available, which can override the Swagger Base Path. This causes the proper Base Path to be displayed in the event that the api doc is not populated properly. [#810](https://github.com/zowe/api-layer/issues/810)
- Bugfix: Removed overwriting of the Swagger Base Path, which resulted in malformed API routes when the base URL is shared among multiple services. [#852](https://github.com/zowe/api-layer/issues/852)
- Bugfix: API ML was previously not reporting SSL certificate errors when servers were unable to communicate. Now, if a SSLException occurs, SSL certificate errors are reported.  [#698](https://github.com/zowe/api-layer/issues/698)
- Bugfix: Fixed language in log messages for consistency. [#830](https://github.com/zowe/api-layer/issues/830)

## `1.15.0`

- Feature: The API Path Pattern now supports serviceId as the first element. This improves the consistency of the URL when processing through the Gateway or outside of the Gateway. [#688](https://github.com/zowe/api-layer/issues/688)
- Feature: The SAF Provider can now be used as a possible authentication provider. This removes the API ML dependency on z/OSMF for authentication enabling SAF to obtain the JWT. [#472](https://github.com/zowe/api-layer/issues/472)
- Feature: The Swagger URL is now provided for z/OSMF. This URL provides full documentation containing the Try It Out functionality if the zOSMF version supports the Swagger endpoint. Alternatively, the URL provides the info endpoint to directly enable access to Zowe endpoints.  [#665](https://github.com/zowe/api-layer/issues/665)
- Feature: The default configuration of API ML now supports character encoding. [#777](https://github.com/zowe/api-layer/issues/777)
- Bugfix: SSL validation when Eureka is running in HTTP mode has been fixed. When the scheme is HTTP, SSL configuration is not verified since it not used. [#792](https://github.com/zowe/api-layer/issues/792)
- Bugfix: A problem in error handling has been fixed when no api-doc is available. Now a specific return code and message is generated when a problem occurs when obtaining or transforming the api-doc. [#571](https://github.com/zowe/api-layer/issues/571)

## `1.14.0`

- Bugfix: Prevent crashing of API ML when null routes are set. [#767](https://github.com/zowe/api-layer/pull/767)
- Feature: Add support to the X-Forwarded-* Headers. [#769](https://github.com/zowe/api-layer/pull/769)
- Feature: Improve the configuration validator for the enablers to improve message specificity when one or more parameters required for setup are missing. [#760](https://github.com/zowe/api-layer/pull/760)

## `1.13.0`

- Feature: Add CORS Headers Support [#384](https://github.com/zowe/api-layer/issues/384)
- Feature: Option to set connection timeout for service [#683](https://github.com/zowe/api-layer/issues/683)
- Bugfix: SAF Keyrings support for ZAAS Client [#656](https://github.com/zowe/api-layer/issues/656)
- Feature: Spring Boot enabler configuration validation [#570](https://github.com/zowe/api-layer/issues/570)

## `1.12.0`

- Feature: Provide Zowe Authentication and Authorization Service (ZAAS) client [#425](https://github.com/zowe/api-layer/issues/425)
- Feature: Refresh the static client definitions from the API Catalog UI [#57](https://github.com/zowe/api-layer/issues/57)
- Feature: Switch to sso-auth instead of apiml-auth [#55](https://github.com/zowe/api-layer/issues/55)
- Bugfix: Added logout endpoint api doc [#632](https://github.com/zowe/api-layer/issues/632)
- Bugfix: Make jjwt only a test dependency [#563](https://github.com/zowe/api-layer/issues/563)
- Bugfix: Fix order of fetching JWT from request [#661](https://github.com/zowe/api-layer/pull/661) 
- Feature: Implement request retrying for service instances [#401](https://github.com/zowe/api-layer/issues/401)

## `1.11.0`

- Feature: Allow users of the API Catalog to test public and private endpoints directly from the UI [#258](https://github.com/zowe/api-layer/issues/258)
- Feature: Provide Endpoints to get public keys for JWT verification (z/OSMF and Zowe) [#566](https://github.com/zowe/api-layer/pull/566) 
- Bugfix: Log errors found while processing static definitions [#579](https://github.com/zowe/api-layer/issues/579)
- Bugfix: Use uppercase parameters for PassTicket service [#592](https://github.com/zowe/api-layer/pull/592)
