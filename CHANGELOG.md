# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## `APIML 2.17.2 / Zowe 2.17.0 (2024-07-15)`

* Feature:  Cloud gateway support for AT-TLS (#3564) ([084f9b4](https://github.com/zowe/api-layer/commit/084f9b4)), closes [#3564](https://github.com/zowe/api-layer/issues/3564)
* Feature:  Customizable request buffer size for WebSocket connections (#3609) ([4a7ac74](https://github.com/zowe/api-layer/commit/4a7ac74)), closes [#3609](https://github.com/zowe/api-layer/issues/3609)


* Bugfix:  Update attls icsf condition (#3635) ([3954f0c](https://github.com/zowe/api-layer/commit/3954f0c)), closes [#3635](https://github.com/zowe/api-layer/issues/3635)
* Bugfix:  Add missing PAT documetantion (#3618) ([224f897](https://github.com/zowe/api-layer/commit/224f897)), closes [#3618](https://github.com/zowe/api-layer/issues/3618)
* Bugfix:  Add requestConnectionTimeout as a zowe.yaml property (#3629) ([ef1e451](https://github.com/zowe/api-layer/commit/ef1e451)), closes [#3629](https://github.com/zowe/api-layer/issues/3629)
* Bugfix:  disable auto conversion for tagged files on z/os (#3619) ([75025bc](https://github.com/zowe/api-layer/commit/75025bc)), closes [#3619](https://github.com/zowe/api-layer/issues/3619)
* Bugfix:  Do not load keystore when ATTLS is set, to allow for ICSF keys. Keystore reading of ICSF keys i ([bd4e738](https://github.com/zowe/api-layer/commit/bd4e738)), closes [#3612](https://github.com/zowe/api-layer/issues/3612)
* Bugfix:  optionaly protect health endpoint (#3625) ([204e120](https://github.com/zowe/api-layer/commit/204e120)), closes [#3625](https://github.com/zowe/api-layer/issues/3625)
* Bugfix:  Provide the correct external URL in the ZUUL Gateway if AT-TLS is enabled (#3565) ([ef78150](https://github.com/zowe/api-layer/commit/ef78150)), closes [#3565](https://github.com/zowe/api-layer/issues/3565)
* Bugfix:  Specify protocol in the start.sh (#3593) ([9992aba](https://github.com/zowe/api-layer/commit/9992aba)), closes [#3593](https://github.com/zowe/api-layer/issues/3593)
* Bugfix:  UI titles and message consistency (#3502) ([bcf992d](https://github.com/zowe/api-layer/commit/bcf992d)), closes [#3502](https://github.com/zowe/api-layer/issues/3502)
* Bugfix:  WebSocket client default timeout (#3613) ([7b8e29b](https://github.com/zowe/api-layer/commit/7b8e29b)), closes [#3613](https://github.com/zowe/api-layer/issues/3613)


## `2.16.2 / Zowe 2.16.2 (2024-05-22)`

* Feature:  OIDC authentication flow v2 (#3527) ([4c81a7c](https://github.com/zowe/api-layer/commit/4c81a7c)), closes [#3527](https://github.com/zowe/api-layer/issues/3527)
* Feature:  Add message for when API Mediation Layer starts (#3523) ([b734662](https://github.com/zowe/api-layer/commit/b734662)), closes [#3523](https://github.com/zowe/api-layer/issues/3523)
* Feature:  Disable ssl on attls profile (#3521) ([f28aec8](https://github.com/zowe/api-layer/commit/f28aec8)), closes [#3521](https://github.com/zowe/api-layer/issues/3521)
* Feature:  Forward valid OIDC token to southbound service in case of distributed ID is not mapped (#3497) ([60777c1](https://github.com/zowe/api-layer/commit/60777c1)), closes [#3497](https://github.com/zowe/api-layer/issues/3497)
* Feature:  include OIDC JWKSet in the gateway JWKs (#3499) ([a588a8f](https://github.com/zowe/api-layer/commit/a588a8f)), closes [#3499](https://github.com/zowe/api-layer/issues/3499)
* Feature:  Move OIDC access token from cookie to special header (#3513) ([6248308](https://github.com/zowe/api-layer/commit/6248308)), closes [#3513](https://github.com/zowe/api-layer/issues/3513)
* Feature:  Allow further customization of websocket webclient (#3315) ([7e8d855](https://github.com/zowe/api-layer/commit/7e8d855)), closes [#3315](https://github.com/zowe/api-layer/issues/3315)
* Feature:  The log message `ZWEAM001I` is issued when API Mediation Layer starts (#3523) ([b734662](https://github.com/zowe/api-layer/commit/b734662)), closes [#3523](https://github.com/zowe/api-layer/issues/3523)
* Feature:  SSL is now disabled when profile `attls` is active to simplify AT-TLS configuration (#3521) ([f28aec8](https://github.com/zowe/api-layer/commit/f28aec8)), closes [#3521](https://github.com/zowe/api-layer/issues/3521)
* Feature:  Valid OIDC tokens are now forwarded to the downstream service when the distributed ID is not mapped (#3497) ([60777c1](https://github.com/zowe/api-layer/commit/60777c1)), closes [#3497](https://github.com/zowe/api-layer/issues/3497)
* Feature:  Included OIDC JWKSet in the gateway JWKs. JWKs retrieved from the Identity Provider allow clients and services to validate the OIDC access token locally (#3499) ([a588a8f](https://github.com/zowe/api-layer/commit/a588a8f)), closes [#3499](https://github.com/zowe/api-layer/issues/3499)
* Feature:  Moved OIDC access token from cookie to special header. If the user ID from the token cannot be mapped to a mainframe account, the access token is now sent via the request header OIDC-token (#3513) ([6248308](https://github.com/zowe/api-layer/commit/6248308)), closes [#3513](https://github.com/zowe/api-layer/issues/3513)


* Bugfix:  Prevent logging failure messages irrelevant to all problems. (#3542) ([714fa6a](https://github.com/zowe/api-layer/commit/714fa6a)), closes [#3542](https://github.com/zowe/api-layer/issues/3542)
* Bugfix:  Fix reading OIDC configuration from system environment (#3537) ([c1a1c9f](https://github.com/zowe/api-layer/commit/c1a1c9f)), closes [#3537](https://github.com/zowe/api-layer/issues/3537)
* Bugfix:  Read from env instead of properties (#3535) ([87352b1](https://github.com/zowe/api-layer/commit/87352b1)), closes [#3535](https://github.com/zowe/api-layer/issues/3535)
* Bugfix:  Fix SSL Context switching (backport of #3531) (#3532) ([cefeae3](https://github.com/zowe/api-layer/commit/cefeae3)), closes [#3531](https://github.com/zowe/api-layer/issues/3531) [#3532](https://github.com/zowe/api-layer/issues/3532)
* Bugfix:  Change scheme of service homepage when AT-TLS is enabled and fix bug in UI (#3346) ([c75280c](https://github.com/zowe/api-layer/commit/c75280c)), closes [#3346](https://github.com/zowe/api-layer/issues/3346)
* Bugfix:  Check for nullpointer exception when jwk key cannot be retrieved (#3503) ([7c00dba](https://github.com/zowe/api-layer/commit/7c00dba)), closes [#3503](https://github.com/zowe/api-layer/issues/3503)
* Bugfix:  Correct the API ML started message (#3524) ([faace92](https://github.com/zowe/api-layer/commit/faace92)), closes [#3524](https://github.com/zowe/api-layer/issues/3524)
* Bugfix:  Fix issue when PAT passed as authorization header with auth scheme zoweJwt (#3505) ([5585231](https://github.com/zowe/api-layer/commit/5585231)), closes [#3505](https://github.com/zowe/api-layer/issues/3505)
* Bugfix:  Fix header position (#3345) ([9a9f8e3](https://github.com/zowe/api-layer/commit/9a9f8e3)), closes [#3345](https://github.com/zowe/api-layer/issues/3345)
* Bugfix:  Log real status code (#3326) ([8a42c17](https://github.com/zowe/api-layer/commit/8a42c17)), closes [#3326](https://github.com/zowe/api-layer/issues/3326)
* Bugfix:  More general exception handling to detect TCP Stack restart (#3462) ([33daf4c](https://github.com/zowe/api-layer/commit/33daf4c)), closes [#3462](https://github.com/zowe/api-layer/issues/3462)
* Bugfix:  Fix Zowe logo and trademark info (#3338) ([0d895ac](https://github.com/zowe/api-layer/commit/0d895ac)), closes [#3338](https://github.com/zowe/api-layer/issues/3338)
* Bugfix:  Fix base path in API Catalog (#3297) ([309aac0](https://github.com/zowe/api-layer/commit/309aac0)), closes [#3297](https://github.com/zowe/api-layer/issues/3297)
* Bugfix:  Fix disabling EhCache (#3280) ([a3067b9](https://github.com/zowe/api-layer/commit/a3067b9)), closes [#3280](https://github.com/zowe/api-layer/issues/3280)
* Bugfix:  Allow key exchange port configuration (#3453) ([d82322e](https://github.com/zowe/api-layer/commit/d82322e)), closes [#3453](https://github.com/zowe/api-layer/issues/3453)
* Bugfix:  Changed the scheme of the service homepage when AT-TLS is enabled and fix a bug in UI (#3346) ([c75280c](https://github.com/zowe/api-layer/commit/c75280c)), closes [#3346](https://github.com/zowe/api-layer/issues/3346)
* Bugfix:  Checked for NullPointerException when the JWK key cannot be retrieved (#3503) ([7c00dba](https://github.com/zowe/api-layer/commit/7c00dba)), closes [#3503](https://github.com/zowe/api-layer/issues/3503)
* Bugfix:  Fixed an issue when PAT passed as the authorization header with the auth scheme zoweJwt (#3505) ([5585231](https://github.com/zowe/api-layer/commit/5585231)), closes [#3505](https://github.com/zowe/api-layer/issues/3505)
* Bugfix:  Fixed the header position in the API Catalog (#3345) ([9a9f8e3](https://github.com/zowe/api-layer/commit/9a9f8e3)), closes [#3345](https://github.com/zowe/api-layer/issues/3345)
* Bugfix:  Fixed the log message about unauthorized calls (#3326) ([8a42c17](https://github.com/zowe/api-layer/commit/8a42c17)), closes [#3326](https://github.com/zowe/api-layer/issues/3326)
* Bugfix:  Allow for more general exception handling to detect TCP Stack restart (#3462) ([33daf4c](https://github.com/zowe/api-layer/commit/33daf4c)), closes [#3462](https://github.com/zowe/api-layer/issues/3462)
* Bugfix:  Respect configuration enabling JWT Token Refresh Functionality #3468 (#3474) ([b4146be](https://github.com/zowe/api-layer/commit/b4146be)), closes [#3468](https://github.com/zowe/api-layer/issues/3468) [#3474](https://github.com/zowe/api-layer/issues/3474)
* Bugfix:  Fix the Zowe logo and trademark info in the API Catalog (#3338) ([0d895ac](https://github.com/zowe/api-layer/commit/0d895ac)), closes [#3338](https://github.com/zowe/api-layer/issues/3338)


## `2.15.0 / Zowe 2.15.0 (2024-02-21)`

* Feature:  Support timeout configuration values of websocket webclient in the API Gateway (#3315) ([7e8d855](https://github.com/zowe/api-layer/commit/7e8d855)), closes [#3315](https://github.com/zowe/api-layer/issues/3315)


* Bugfix:  Fix displaying base path in API Catalog (#3297) ([309aac0](https://github.com/zowe/api-layer/commit/309aac0)), closes [#3297](https://github.com/zowe/api-layer/issues/3297)
* Bugfix:  Fix disabling EhCache using `apiml.caching.enabled=false` (#3280) ([a3067b9](https://github.com/zowe/api-layer/commit/a3067b9)), closes [#3280](https://github.com/zowe/api-layer/issues/3280)

## `2.14.4 / Zowe 2.14.0 (2023-01-19)`

* Feature:  Introduce native identity mapper as a replacement for ZSS identity mapping of x509 and OIDC id ([24ba6d2](https://github.com/zowe/api-layer/commit/24ba6d2)), closes [#3252](https://github.com/zowe/api-layer/issues/3252)


* Bugfix:  Fix truststore for websockets in Spring Cloud Gateway (v2) (#3249) ([b4d6730](https://github.com/zowe/api-layer/commit/b4d6730)), closes [#3249](https://github.com/zowe/api-layer/issues/3249)
* Bugfix:  Fix keyring path update to properly support keyring paths by Spring Cloud Gateway (#3265) ([5593723d](https://github.com/zowe/api-layer/commit/5593723d)), closes [#3265](https://github.com/zowe/api-layer/issues/3265)
* Bugfix:  Fix conflict of XML processing between EhCache and onboarding process (#3266) ([8d30acb6](https://github.com/zowe/api-layer/commit/8d30acb6)), closes [#3266](https://github.com/zowe/api-layer/issues/3266)
* Bugfix:  Fix using keystore during creation of request without client certificate in Spring Cloud Gateway (#3273) ([57a201d1](https://github.com/zowe/api-layer/commit/57a201d1)), closes [#3273](https://github.com/zowe/api-layer/issues/3273)
* Bugfix:  Fix closing WebSocket to prevent a memory leak. (#3271) ([aa8b316f](https://github.com/zowe/api-layer/commit/aa8b316f)), closes [#3271](https://github.com/zowe/api-layer/issues/3271)
* Bugfix:  Fix disabling EhCache (#3276) ([3c189204](https://github.com/zowe/api-layer/commit/3c189204)), closes [#3276](https://github.com/zowe/api-layer/issues/3276)
* Bugfix:  Fix enabling CORS by default in AT-TLS mode used in the API Gateway (#3270) ([9d5e3a31](https://github.com/zowe/api-layer/commit/9d5e3a31)), closes [#3270](https://github.com/zowe/api-layer/issues/3270)

## `2.13.2 / Zowe 2.13.0 (2023-12-11)`

* Feature:  CORS enabled in default mode with AT-TLS profile (#3221) ([047ec144](https://github.com/zowe/api-layer/commit/047ec144)), closes [#3221](https://github.com/zowe/api-layer/issues/3221)
* Feature:  Add Zowe authentication scheme to Cloud Gateway (#3214) ([b71fbc1](https://github.com/zowe/api-layer/commit/b71fbc1)), closes [#3214](https://github.com/zowe/api-layer/issues/3214)
* Feature:  Add new `/zaas/zoweJwt` endpoint to provide Zowe JWT token for Spring cloud Gateway (#3199) ([7d42791d](https://github.com/zowe/api-layer/commit/7d42791d)), closes [#3199](https://github.com/zowe/api-layer/issues/3199)
* Feature:  Add new `/zaas/zosmf` endpoint to provide z/OSMF JWT/LTPA2 token for Spring cloud Gateway (#3153) ([cc96c81](https://github.com/zowe/api-layer/commit/cc96c81)), closes [#3153](https://github.com/zowe/api-layer/issues/3153)
* Feature:  Add new `/zaas/safIdt` endpoint to provide SAF IDT token for Spring cloud Gateway (#3220) ([c72adbc8](https://github.com/zowe/api-layer/commit/c72adbc8)), closes [#3220](https://github.com/zowe/api-layer/issues/3220)
* Feature:  Support z/OSMF scheme in Spring Cloud Gateway (#3190) ([c059322](https://github.com/zowe/api-layer/commit/c059322)), closes [#3190](https://github.com/zowe/api-layer/issues/3190)
* Feature:  Fixes for Azure JWKS reader (#3201) ([f24695ed](https://github.com/zowe/api-layer/commit/f24695ed)), closes [#3200](https://github.com/zowe/api-layer/issues/3200)
* Feature:  Supporting additional Discovery Service registration by Spring Cloud Gateway (#3181) ([c6cc561](https://github.com/zowe/api-layer/commit/c6cc561)), closes [#3181](https://github.com/zowe/api-layer/issues/3181)
* Feature:  Gateway additional registrations HA (#3127) ([a367380](https://github.com/zowe/api-layer/commit/a367380)), closes [#3127](https://github.com/zowe/api-layer/issues/3127)
* Feature:  Fetch JWK from OIDC providers (#3137) ([b23bb8f](https://github.com/zowe/api-layer/commit/b23bb8f)), closes [#3137](https://github.com/zowe/api-layer/issues/3137)


* Bugfix:  Fix signing outgoing call from Cloud Gateway just if necessary (#3203) ([12ca262](https://github.com/zowe/api-layer/commit/12ca262)), closes [#3203](https://github.com/zowe/api-layer/issues/3203)
* Bugfix:  AT-TLS support (#3186) ([8a26c44](https://github.com/zowe/api-layer/commit/8a26c44)), closes [#3186](https://github.com/zowe/api-layer/issues/3186)
* Bugfix:  read public key from keyring (#3212) ([a0a6937](https://github.com/zowe/api-layer/commit/a0a6937)), closes [#3212](https://github.com/zowe/api-layer/issues/3212)
* Bugfix:  Update bean definitions for noop cache mode (#3197) ([b73d812](https://github.com/zowe/api-layer/commit/b73d812)), closes [#3197](https://github.com/zowe/api-layer/issues/3197)
* Bugfix:  Change ehCache storage location (#3184) ([adfadff](https://github.com/zowe/api-layer/commit/adfadff)), closes [#3184](https://github.com/zowe/api-layer/issues/3184)
* Bugfix:  Fix qualifier for JWT clock (#3180) ([6bc1afae](https://github.com/zowe/api-layer/commit/6bc1afae)), closes [#3180](https://github.com/zowe/api-layer/pull/3180)
* Bugfix:  Set HTTP client timeouts  (#3174) ([f9d5d6f](https://github.com/zowe/api-layer/commit/f9d5d6f)), closes [#3174](https://github.com/zowe/api-layer/issues/3174)
* Bugfix:  Style updates for Catalog UI and Caching Fix for static file distribution in API Catalog (#3168) ([092c45b](https://github.com/zowe/api-layer/commit/092c45b)), closes [#3168](https://github.com/zowe/api-layer/issues/3168)
* Bugfix:  Gateway additional registration fixes (#3172) ([557b4f5](https://github.com/zowe/api-layer/commit/557b4f5)), closes [#3172](https://github.com/zowe/api-layer/issues/3172)
* Bugfix:  Reasonable defaults in the cloud-gateway-service application.yml (#3167) ([f10ad93](https://github.com/zowe/api-layer/commit/f10ad93)), closes [#3167](https://github.com/zowe/api-layer/issues/3167)
* Bugfix:  Qualifier for clock to avoid conflict in extension (#3166) ([d1aa345a](https://github.com/zowe/api-layer/commit/d1aa345a)), closes [#3166](https://github.com/zowe/api-layer/pull/3166)
* Bugfix:  Enhance error handling in the UI (#3158) ([ee39a4b](https://github.com/zowe/api-layer/commit/ee39a4b)), closes [#3158](https://github.com/zowe/api-layer/issues/3158)
* Bugfix:  Fix context oath (#3159) ([a0715938](https://github.com/zowe/api-layer/commit/a0715938)), closes [#3159](https://github.com/zowe/api-layer/issues/3159)
* Bugfix:  Fix of resource leak in loading of images in API Catalog (#3233) ([29424cd7](https://github.com/zowe/api-layer/commit/29424cd7)), closes [#3159](https://github.com/zowe/api-layer/issues/3233)

## `APIML 2.12.2 / Zowe 2.12.0 (2023-10-10)`

* Feature:  Added a Central API ML registry endpoint to the Cloud Gateway to access an aggregated view of all services from all domains. (#3076) ([ff8ee9b](https://github.com/zowe/api-layer/commit/ff8ee9b)), closes [#3076](https://github.com/zowe/api-layer/issues/3076)
* Feature:  It is now possible to forward the client certificate from Central Gateway to Domain Gateway in the request header (#3046) ([eda4750](https://github.com/zowe/api-layer/commit/eda4750)), closes [#3046](https://github.com/zowe/api-layer/issues/3046)
* Feature: Register Gateway to an additional Discovery service. Clients outside of the API ML cluster can now know about other gateways to facilitate routing to clusters and domains. (#3068) ([e988df3](https://github.com/zowe/api-layer/commit/e988df3)), closes [#3068](https://github.com/zowe/api-layer/issues/3068) [#3044](https://github.com/zowe/api-layer/issues/3044)
* Feature:  You can now verify service SSO support from API ML (#3054) ([3c1bb91](https://github.com/zowe/api-layer/commit/3c1bb91)), closes [#3054](https://github.com/zowe/api-layer/issues/3054)


* Bugfix:  Fixed normalization of `baseUrl` in ZAAS client (#3123) ([c8a23a8](https://github.com/zowe/api-layer/commit/c8a23a8)), closes [#3123](https://github.com/zowe/api-layer/issues/3123)
* Bugfix:  Added the JVM heap configuration to `zowe.yaml` (#3087) ([59ead89](https://github.com/zowe/api-layer/commit/59ead89)), closes [#3087](https://github.com/zowe/api-layer/issues/3087)
* Bugfix:  Fixed an error preventing the Catalog UI to load when a service does not have some required parameter. (#3050) ([30ec8df](https://github.com/zowe/api-layer/commit/30ec8df)), closes [#3050](https://github.com/zowe/api-layer/issues/3050)
* Bugfix:  Fix navigation issue in Catalog when using browser back button (#3145) ([eb7b02e](https://github.com/zowe/api-layer/commit/b2492fd9)), closes [#3135](https://github.com/zowe/api-layer/issues/2998)

## `APIML 2.11.0 / Zowe 2.11.0 (2023-08-30)`

* Feature:  Spring cloud gateway routing (#3031) ([a1dd492](https://github.com/zowe/api-layer/commit/a1dd492)), closes [#3031](https://github.com/zowe/api-layer/issues/3031)
* Feature:  Swagger validation for registered services. (#3039) ([b5ad040](https://github.com/zowe/api-layer/commit/b5ad040)), closes [#3039](https://github.com/zowe/api-layer/issues/3039)
  

* Bugfix:  Set default value of nonStrictVerifySslCertificatesOfServices to false (#3029) ([75b658c](https://github.com/zowe/api-layer/commit/75b658c)), closes [#3029](https://github.com/zowe/api-layer/issues/3029)
* Bugfix:  newlines and SSL error message in z/OSMF validation (#3024) ([0a0b27b](https://github.com/zowe/api-layer/commit/0a0b27b)), closes [#3024](https://github.com/zowe/api-layer/issues/3024)
* Bugfix:  z/OSMF logging improvements (#2998) ([eb7b02e](https://github.com/zowe/api-layer/commit/eb7b02e)), closes [#2998](https://github.com/zowe/api-layer/issues/2998)

## `APIML 2.10.6 / Zowe 2.10.0 (2023-07-28)`

* Feature: The API Catalog now allows pre-defined style customizations (#2965) ([b286cef](https://github.com/zowe/api-layer/commit/b286cef))


* Bugfix:  Provide client with information about expired password (#2969) ([c4dc217](https://github.com/zowe/api-layer/commit/c4dc217)), closes [#2969](https://github.com/zowe/api-layer/issues/2969)

## `APIML 2.9.1 / Zowe 2.9.0 (2023-06-12)`

* Feature: Personal access tokens are now accepted as Bearer authentication and in the apimlAuthenticationToken cookie (#2908) ([7c393a6](https://github.com/zowe/api-layer/commit/7c393a6)), closes [#2908](https://github.com/zowe/api-layer/issues/2908)
* Feature:  A OAuth2 access token is now accepted as an authentication source (#2922) ([3809622](https://github.com/zowe/api-layer/commit/3809622)), closes [#2835](https://github.com/zowe/api-layer/issues/2835)
* Feature: The maximum idle timeout for websocket connections (between the gateway and the registered service) is now configurable (#2914) ([020da87](https://github.com/zowe/api-layer/commit/020da87)), closes [#2914](https://github.com/zowe/api-layer/issues/2914)


* Bugfix:  Ignore client certificate in a request when x509 authentication is not enabled (#2930) ([406f588](https://github.com/zowe/api-layer/commit/406f588)), closes [#2930](https://github.com/zowe/api-layer/issues/2930)
* Bugfix:  Return the correct list of public keys when z/OSMF is not available (#2936) ([030a34f](https://github.com/zowe/api-layer/commit/030a34f)), closes [#2936](https://github.com/zowe/api-layer/issues/2936)

## `APIML 2.8.2 / Zowe 2.8.0 (2023-04-27)`

* Feature: A unique authentication cookie name has been added for multi-instance deployment (#2812) ([6654271](https://github.com/zowe/api-layer/commit/6654271)), closes [#2812](https://github.com/zowe/api-layer/issues/2812)


* Bugfix: Parsing OpenAPI v2 Swagger files by API Catalog (#2876) ([cc45774](https://github.com/zowe/api-layer/commit/cc45774)), closes [#2876](https://github.com/zowe/api-layer/pull/2876)
* Bugfix: Mitigate storing password in memory for V2 (#2858) ([b1596eb](https://github.com/zowe/api-layer/commit/b1596eb)), closes [#2858](https://github.com/zowe/api-layer/issues/2858)
* Bugfix: Mitigate storing password in memory for V1 (#2867) ([3356b7c](https://github.com/zowe/api-layer/commit/3356b7c)), closes [#2867](https://github.com/zowe/api-layer/issues/2867)
* Bugfix: Read response from http client to prevent exahusting connection pool (#2854) ([137be23](https://github.com/zowe/api-layer/commit/137be23)), closes [#2854](https://github.com/zowe/api-layer/issues/2854)
* Bugfix: Passticket generation and limit Eureka replication peers threads (#2845) ([42b491e](https://github.com/zowe/api-layer/commit/42b491e)), closes [#2845](https://github.com/zowe/api-layer/issues/2845)
* Bugfix: Refactor SSL configuration (#2832) ([33f4882](https://github.com/zowe/api-layer/commit/33f4882)), closes [#2832](https://github.com/zowe/api-layer/issues/2832)

## `APIML 2.7.0 / Zowe 2.7.0 (2023-03-10)`

* Feature:  Support of other keyring types (#2799) ([952bf2b](https://github.com/zowe/api-layer/commit/952bf2b)), closes [#2799](https://github.com/zowe/api-layer/issues/2799)
* Feature:  OIDC info is now available via webfinger (#2757) ([71e88ba](https://github.com/zowe/api-layer/commit/71e88ba)), closes [#2757](https://github.com/zowe/api-layer/issues/2757)
* Feature:  The API Catalog can now be configured to hide service information (#2743) ([2fbbc65](https://github.com/zowe/api-layer/commit/2fbbc65)), closes [#2743](https://github.com/zowe/api-layer/issues/2743)


* Bugfix:  Update keyring config (#2828) ([c1e1cc9](https://github.com/zowe/api-layer/commit/c1e1cc9)), closes [#2828](https://github.com/zowe/api-layer/issues/2828)
* Bugfix: Run gateway instances with own cache storage (#2807) ([4d08707](https://github.com/zowe/api-layer/commit/4d08707)), closes [#2807](https://github.com/zowe/api-layer/issues/2807)
* Bugfix:  Fix stack overflow during cleaning websocket client (#2815) ([376f818](https://github.com/zowe/api-layer/commit/376f818)), closes [#2815](https://github.com/zowe/api-layer/issues/2815)
* Bugfix:  Fix support of different type of keyrings in proper format (just two slashes) (#2687) ([dfb0168](https://github.com/zowe/api-layer/commit/dfb0168)), closes [#2687](https://github.com/zowe/api-layer/issues/2687)
* Bugfix:  Handle WebSocket connection failure (#2805) ([232bade](https://github.com/zowe/api-layer/commit/232bade)), closes [#2805](https://github.com/zowe/api-layer/issues/2805)
* Bugfix:  Periodically clean connection pool (#2797) ([7058290](https://github.com/zowe/api-layer/commit/7058290)), closes [#2797](https://github.com/zowe/api-layer/issues/2797)
* Bugfix:  Recognize profile settings (#2789) ([adf5ea5](https://github.com/zowe/api-layer/commit/adf5ea5)), closes [#2789](https://github.com/zowe/api-layer/issues/2789)
* Bugfix:  Eureka peer connections loop (#2775) ([85a27ea](https://github.com/zowe/api-layer/commit/85a27ea)), closes [#2775](https://github.com/zowe/api-layer/issues/2775)
* Bugfix:  Reduce number of WARN logs (#2780) ([df0243f](https://github.com/zowe/api-layer/commit/df0243f)), closes [#2780](https://github.com/zowe/api-layer/issues/2780)
* Bugfix:  Fix bug in Wizard static onboarding method (#2773) ([c8d7c66](https://github.com/zowe/api-layer/commit/c8d7c66)), closes [#2773](https://github.com/zowe/api-layer/issues/2773)
* Bugfix:  Improve handling of SSL errors (#2744) ([bb9792b](https://github.com/zowe/api-layer/commit/bb9792b)), closes [#2744](https://github.com/zowe/api-layer/issues/2744)

## `APIML 2.6.0 / Zowe 2.6.0 (2023-01-23)`

* Feature: Spring Cloud Gateway implementation - Support of remapping to Passticket (#2046) closes [#2046](https://github.com/zowe/api-layer/issues/2046)
* Feature: Spring Cloud Gateway implementation - Support of remapping to client certificate (#2044) closes [#2044](https://github.com/zowe/api-layer/issues/2044)


* Bugfix: Improve the information for failure of extension loading (#2721) ([8a0455](https://github.com/zowe/api-layer/commit/8a04550aa7eb545e504cc57acf4cf5e38824a7c2))
* Bugfix: Correctly process metadata for the Plain Java Enabler running on z/OS (#1927) ([b182323](https://github.com/zowe/api-layer/commit/b1823238f3dac7738ab13e41653913469b56ee36)), closes [#1927](https://github.com/zowe/api-layer/issues/1927)

## `APIML 2.5.0 / Zowe 2.5.0 (22-12-12)`

* Feature:  Retry failed request routed through the Cloud Gateway service (#2697) ([b2e86b3](https://github.com/zowe/api-layer/commit/b2e86b3)), closes [#2697](https://github.com/zowe/api-layer/issues/2697)
* Feature:  The Cloud Gateway service can now handle cross-origin requests similar to the functionality in the Gateway(#2701) ([f5ab7b8](https://github.com/zowe/api-layer/commit/f5ab7b8)), closes [#2701](https://github.com/zowe/api-layer/issues/2701)
* Feature:  It is now possible to issue SMF records #83 about PAT generation via Rauditx (#2691) ([97df0bc](https://github.com/zowe/api-layer/commit/97df0bc)), closes [#83](https://github.com/zowe/api-layer/issues/83) [#2691](https://github.com/zowe/api-layer/issues/2691)
* Feature: A circuit breaker is now included in the Cloud Gateway service request router (#2679) ([b704413](https://github.com/zowe/api-layer/commit/b704413)), closes [#2679](https://github.com/zowe/api-layer/issues/2679)
* Feature:  A custom authentication header has been added for southbound services (#2618) ([9272aa0](https://github.com/zowe/api-layer/commit/9272aa0)), closes [#2618](https://github.com/zowe/api-layer/issues/2618)
* Feature:  It is now possible to include a generated passticket in the custom request header (#2625) ([e52448c](https://github.com/zowe/api-layer/commit/e52448c)), closes [#2625](https://github.com/zowe/api-layer/issues/2625)


* Bugfix:  Upgrade swagger parser and exclude bugged version (#2702) ([6b966b2](https://github.com/zowe/api-layer/commit/6b966b2)), closes [#2702](https://github.com/zowe/api-layer/issues/2702)
* Bugfix:  Allow to set private key password separately (#2684) ([4e3d3c7](https://github.com/zowe/api-layer/commit/4e3d3c7)), closes [#2684](https://github.com/zowe/api-layer/issues/2684)
* Bugfix:  Set content type header when calling caching service (#2682) ([97cb29c](https://github.com/zowe/api-layer/commit/97cb29c)), closes [#2682](https://github.com/zowe/api-layer/issues/2682)
* Bugfix:  Register java time module to parse date in swagger (#2634) ([b3d53b2](https://github.com/zowe/api-layer/commit/b3d53b2)), closes [#2634](https://github.com/zowe/api-layer/issues/2634)
* Bugfix:  Allowing users to type keyring with either 4 or 2 slashes (#2626) ([db33c4c](https://github.com/zowe/api-layer/commit/db33c4c)), closes [#2626](https://github.com/zowe/api-layer/issues/2626)

## `APIML 2.4.10 / Zowe 2.4.0 (2022-10-06)`

* Feature:  Validate OIDC token (#2604) ([cdd4a43](https://github.com/zowe/api-layer/commit/cdd4a43)), closes [#2604](https://github.com/zowe/api-layer/issues/2604)
* Feature: Introduced service routing based on header to enables the Cloud Gateway to route to a southbound service by information in the request header. (#2600) ([6fafb60](https://github.com/zowe/api-layer/commit/6fafb60)), closes [#2600](https://github.com/zowe/api-layer/issues/2600)
* Feature:  Introduced a new cloud gateway service that  provides routing functionality for multi-sysplex environments. (#2576) ([7c618c0](https://github.com/zowe/api-layer/commit/7c618c0)), closes [#2576](https://github.com/zowe/api-layer/issues/2576)


* Bugfix:  Do not require clientAuth extension (#2595) ([e9e8092](https://github.com/zowe/api-layer/commit/e9e8092)), closes [#2595](https://github.com/zowe/api-layer/issues/2595)
* Bugfix:  snakeyml update, scheme validation fix (#2577) ([ae48669](https://github.com/zowe/api-layer/commit/ae48669)), closes [#2577](https://github.com/zowe/api-layer/issues/2577)
* Bugfix:  Add build info to the manifest.yaml (#2573) ([93298dd](https://github.com/zowe/api-layer/commit/93298dd)), closes [#2573](https://github.com/zowe/api-layer/issues/2573)
* Bugfix:  Fix bug in the swagger (#2571) ([36997c6](https://github.com/zowe/api-layer/commit/36997c6)), closes [#2571](https://github.com/zowe/api-layer/issues/2571)
* Bugfix:  Handle exceptions in extensions config reader (#2609) ([336d3b4](https://github.com/zowe/api-layer/commit/336d3b4)), closes [#2609](https://github.com/zowe/api-layer/issues/2609)
* Bugfix:  Make the SAF IDT properties configurable in Zowe (#2610) ([b28a9dd](https://github.com/zowe/api-layer/commit/b28a9dd)), closes [#2610](https://github.com/zowe/api-layer/issues/2610)

## `APIML 2.4.4 / Zowe 2.3.0, 2.3.1 (2022-09-15)`

* Feature: Introduction of a new cloud gateway service to provide routing functionality for multi-sysplex environments. (#2576) ([7c618c0](https://github.com/zowe/api-layer/commit/7c618c0)), closes [#2576](https://github.com/zowe/api-layer/issues/2576)
* Feature: Introduced a new Personal Access Token (PAT) API to evict non-relevant tokens and rules (#2554) ([f3aeafa](https://github.com/zowe/api-layer/commit/f3aeafa)), closes [#2554](https://github.com/zowe/api-layer/issues/2554)
* Feature: Added a Redis sentinel enabled field that allows Sentinel configuration to be added to a file and kept available even when Sentinel is not in use. (#2546) ([3779072](https://github.com/zowe/api-layer/commit/3779072)), closes [#2546](https://github.com/zowe/api-layer/issues/2546)
* Feature:  Added customized code snippets to API Catalog.  Customized snippets can now be defined as part of the service metadata to be displayed in the API Catalog UI (#2526) ([602392e](https://github.com/zowe/api-layer/commit/602392e)), closes [#2526](https://github.com/zowe/api-layer/issues/2526)
* Feature:  Code snippet configuration now enables direct integration of an endpoint into an application without requiring code to integrate the other application's REST APIs. (#2509) ([4d2298e](https://github.com/zowe/api-layer/commit/4d2298e)), closes [#2509](https://github.com/zowe/api-layer/issues/2509)
* Feature: A Personal Access Token (PAT) for SSO is now accepted. The PAT can now be validated and invalidated using a REST API on the Gateway (#2499) ([ad17c18](https://github.com/zowe/api-layer/commit/ad17c18)), closes [#2499](https://github.com/zowe/api-layer/issues/2499)


* Bugfix:  snakeyml update, scheme validation fix (#2577) ([ae48669](https://github.com/zowe/api-layer/commit/ae48669)), closes [#2577](https://github.com/zowe/api-layer/issues/2577)
* Bugfix:  Add build info to the manifest.yaml (#2573) ([93298dd](https://github.com/zowe/api-layer/commit/93298dd)), closes [#2573](https://github.com/zowe/api-layer/issues/2573)
* Bugfix:  Fix bug in the swagger (#2571) ([36997c6](https://github.com/zowe/api-layer/commit/36997c6)), closes [#2571](https://github.com/zowe/api-layer/issues/2571)
* Bugfix:  AdditionalProperties must be outside of properties attribute (#2567) ([fea515a](https://github.com/zowe/api-layer/commit/fea515a)), closes [#2567](https://github.com/zowe/api-layer/issues/2567)
* Bugfix:  Enable hsts (#2565) ([4cffe97](https://github.com/zowe/api-layer/commit/4cffe97)), closes [#2565](https://github.com/zowe/api-layer/issues/2565)
* Bugfix:  Fix code snippets bug  (#2564) ([23bed56](https://github.com/zowe/api-layer/commit/23bed56)), closes [#2564](https://github.com/zowe/api-layer/issues/2564)
* Bugfix:  Enable redis storage mode in tests (#2522) ([11bf491](https://github.com/zowe/api-layer/commit/11bf491)), closes [#2522](https://github.com/zowe/api-layer/issues/2522)
* Bugfix:  Gradle publish after update  (#2528) ([1baa6f7](https://github.com/zowe/api-layer/commit/1baa6f7)), closes [#2528](https://github.com/zowe/api-layer/issues/2528)
* Bugfix:  Remove multiple tokens from cookies (#2514) ([d5bc187](https://github.com/zowe/api-layer/commit/d5bc187)), closes [#2514](https://github.com/zowe/api-layer/issues/2514)
* Bugfix:  Retrieve swagger api docs with or without certificate configuration enabled (#2500) ([16ca734](https://github.com/zowe/api-layer/commit/16ca734)), closes [#2500](https://github.com/zowe/api-layer/issues/2500)

## `APIML 2.2.4 / Zowe 2.2.0 (2022-07-14)`

* Feature:  Revoke a Persoanal Access Token by admin (#2476) ([e4d42a9](https://github.com/zowe/api-layer/commit/e4d42a9)), closes [#2476](https://github.com/zowe/api-layer/issues/2476)
* Feature:  Caching Service can store invalidated token rules (#2460) ([055aac9](https://github.com/zowe/api-layer/commit/055aac9)), closes [#2460](https://github.com/zowe/api-layer/issues/2460)
* Feature:  Exchange client certificate for SAF IDT (#2455) ([303087c](https://github.com/zowe/api-layer/commit/303087c)), closes [#2455](https://github.com/zowe/api-layer/issues/2455) [#2384](https://github.com/zowe/api-layer/issues/2384)
* Feature:  Fix SAF IDT scheme and service (#2224) ([7772401](https://github.com/zowe/api-layer/commit/7772401)), closes [#2224](https://github.com/zowe/api-layer/issues/2224)
* Feature:  Generate Personal Access Token (#2452) ([0e39aa7](https://github.com/zowe/api-layer/commit/0e39aa7)), closes [#2452](https://github.com/zowe/api-layer/issues/2452)
* Feature:  Limit the scope of a Personal Access Token (#2456) ([cc0aba4](https://github.com/zowe/api-layer/commit/cc0aba4)), closes [#2456](https://github.com/zowe/api-layer/issues/2456)
* Feature:  Revoke a Personal Access Token (#2422) ([c7f79d5](https://github.com/zowe/api-layer/commit/c7f79d5)), closes [#2422](https://github.com/zowe/api-layer/issues/2422)
* Feature:  Validate ServiceId with Endpoint (#2413) ([9f3825f](https://github.com/zowe/api-layer/commit/9f3825f)), closes [#2413](https://github.com/zowe/api-layer/issues/2413)


* Bugfix:  Immediately expire a passticket command to generate a passticket for each call (#2496) ([8adca78](https://github.com/zowe/api-layer/commit/8adca78)), closes [#2496](https://github.com/zowe/api-layer/issues/2496)
* Bugfix:  Optimize image builds (#2445) ([e220cbd](https://github.com/zowe/api-layer/commit/e220cbd)), closes [#2445](https://github.com/zowe/api-layer/issues/2445)
* Bugfix:  Extend Tomcat to be able to recover after TCP/IP stack is restarted, so that the service does not require restart. (#2421) ([a851b8f](https://github.com/zowe/api-layer/commit/a851b8f)), closes [#2421](https://github.com/zowe/api-layer/issues/2421)

## `APIML 2.1.0 / Zowe 2.1.0 (2022-05-31)`

* Feature:  Add trivial schema files for lib components. Update manifests to remove build metadata (#2379) ([6eba58f](https://github.com/zowe/api-layer/commit/6eba58f)), closes [#2379](https://github.com/zowe/api-layer/issues/2379)
* Feature:  Extend API operation filter in the Swagger UI (#2397) ([cffd6cf](https://github.com/zowe/api-layer/commit/cffd6cf)), closes [#2397](https://github.com/zowe/api-layer/issues/2397)
* Feature:  Generate basic code snippets (#2387) ([79c67d0](https://github.com/zowe/api-layer/commit/79c67d0)), closes [#2387](https://github.com/zowe/api-layer/issues/2387)
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


* Bugfix:  Add log masking class for sensitive logs (#2003) ([994b483](https://github.com/zowe/api-layer/commit/994b483)), closes [#2003](https://github.com/zowe/api-layer/issues/2003)
* Bugfix:  API Catalog swagger link (#2344) ([be07fda](https://github.com/zowe/api-layer/commit/be07fda)), closes [#2344](https://github.com/zowe/api-layer/issues/2344)
* Bugfix:  use same key and record lengths as jcl (#2341) ([d8644f2](https://github.com/zowe/api-layer/commit/d8644f2)), closes [#2341](https://github.com/zowe/api-layer/issues/2341)
* Bugfix:  Add server side logging for swagger handling code (#2328) ([7b0455d](https://github.com/zowe/api-layer/commit/7b0455d)), closes [#2328](https://github.com/zowe/api-layer/issues/2328)
* Bugfix:  Preserve request cookies (#2293) ([71c6649](https://github.com/zowe/api-layer/commit/71c6649)), closes [#2293](https://github.com/zowe/api-layer/issues/2293) [#2269](https://github.com/zowe/api-layer/issues/2269)
* Bugfix:  ZaaS client compatibility with Zowe v2 (#2227) ([abdf995](https://github.com/zowe/api-layer/commit/abdf995)), closes [#2227](https://github.com/zowe/api-layer/issues/2227)
* Bugfix:  Add BearerContent filter to enable bearer auth (#2197) ([1d41704](https://github.com/zowe/api-layer/commit/1d41704)), closes [#2197](https://github.com/zowe/api-layer/issues/2197)
* Bugfix:  Configure southbound timeout with APIML_GATEWAY_TIMEOUT_MILLIS (#2154) ([6af5d6f](https://github.com/zowe/api-layer/commit/6af5d6f)), closes [#2154](https://github.com/zowe/api-layer/issues/2154)
* Bugfix:  Improve error handling for API diff endpoint (#2178) ([1581e39](https://github.com/zowe/api-layer/commit/1581e39)), closes [#2178](https://github.com/zowe/api-layer/issues/2178)
* Bugfix:  Versioning in image publishing workflow (#2159) ([db52527](https://github.com/zowe/api-layer/commit/db52527)), closes [#2159](https://github.com/zowe/api-layer/issues/2159)
* Bugfix:  Add x509 auth info to gw api doc (#2142) ([0205470](https://github.com/zowe/api-layer/commit/0205470)), closes [#2142](https://github.com/zowe/api-layer/issues/2142)
* Bugfix:  Properly remove services when instances are removed from Discovery Service (#2128) ([c675b91](https://github.com/zowe/api-layer/commit/c675b91)), closes [#2128](https://github.com/zowe/api-layer/issues/2128)
* Bugfix:  Use ribbon LB for Web sockets (#2147) ([4751dbc](https://github.com/zowe/api-layer/commit/4751dbc)), closes [#2147](https://github.com/zowe/api-layer/issues/2147)
* Bugfix:  Add missing fields in error response (#2118) ([3b9745c](https://github.com/zowe/api-layer/commit/3b9745c)), closes [#2118](https://github.com/zowe/api-layer/issues/2118)
* Bugfix:  Do not require keyAlias for SSL configuration (#2110) ([03bee79](https://github.com/zowe/api-layer/commit/03bee79)), closes [#2110](https://github.com/zowe/api-layer/issues/2110)

## `APIML 2.0.9 / Zowe 2.0.0 (2022-04-20)`

* Feature:  Add missing tooltips to all onboarding options (#2194) ([5446fd5](https://github.com/zowe/api-layer/commit/5446fd5)), closes [#2194](https://github.com/zowe/api-layer/issues/2194)
* Feature:  Discovery service can be configured to modify the service ID at registration time (#2229) ([63f6fde](https://github.com/zowe/api-layer/commit/63f6fde)), closes [#2229](https://github.com/zowe/api-layer/issues/2229)
* Feature:  There is now the option to specify base packages for the extensions loader(#2081) ([9a4be5a](https://github.com/zowe/api-layer/commit/9a4be5a)), closes [#2081](https://github.com/zowe/api-layer/issues/2081)
* Feature:  There is a new design of the logout panel in the Catalog dashboard (#2102) ([1382f24](https://github.com/zowe/api-layer/commit/1382f24)), closes [#2102](https://github.com/zowe/api-layer/issues/2102)
* Feature:  There is now the option to change your password via the Catalog UI (#2035) ([139a231](https://github.com/zowe/api-layer/commit/139a231)), closes [#2035](https://github.com/zowe/api-layer/issues/2035)
* Feature:  Migrated the onboarding wizard to the Material UI library. This replaces the Mineral UI framework with the Material UI usage in the wizard (#2004) ([2c595d5](https://github.com/zowe/api-layer/commit/2c595d5)), closes [#2004](https://github.com/zowe/api-layer/issues/2004)
* Feature:  Migrated the dashboard to the Material UI library (#1959) ([0da7f15](https://github.com/zowe/api-layer/commit/0da7f15)), closes [#1959](https://github.com/zowe/api-layer/issues/1959)
* Feature:  Migrated the detail page to the Material UI library (#1949) ([95da488](https://github.com/zowe/api-layer/commit/95da488)), closes [#1949](https://github.com/zowe/api-layer/issues/1949)
* Feature:  Migrated Error components to the Material UI library (#1957) ([c60371d](https://github.com/zowe/api-layer/commit/c60371d)), closes [#1957](https://github.com/zowe/api-layer/issues/1957)
* Feature:  Migrated the header and footer to the Material UI library (#1943) ([537fa34](https://github.com/zowe/api-layer/commit/537fa34)), closes [#1943](https://github.com/zowe/api-layer/issues/1943)
* Feature:  Migrated the Login panel to the Material UI library (#1900) ([81ab2ed](https://github.com/zowe/api-layer/commit/81ab2ed)), closes [#1900](https://github.com/zowe/api-layer/issues/1900) 
* Feature:  Made various improvements to the onboarding wizard (#1772) ([20dd70b](https://github.com/zowe/api-layer/commit/20dd70b)), closes [#1772](https://github.com/zowe/api-layer/issues/1772)


* Bugfix:  Caching service logging (#2222) ([5ff64d9](https://github.com/zowe/api-layer/commit/5ff64d9)), closes [#2222](https://github.com/zowe/api-layer/issues/2222)
* Bugfix:  Upgrade Spring to version without vulnerability (#2252) ([f77fa1b](https://github.com/zowe/api-layer/commit/f77fa1b)), closes [#2252](https://github.com/zowe/api-layer/issues/2252)
* Bugfix:  Use unique artifacts for gha (#2192) ([f8def8c](https://github.com/zowe/api-layer/commit/f8def8c)), closes [#2192](https://github.com/zowe/api-layer/issues/2192)
* Bugfix:  Add x509 auth info to gw api doc (#2142) ([072ad23](https://github.com/zowe/api-layer/commit/072ad23)), closes [#2142](https://github.com/zowe/api-layer/issues/2142)
* Bugfix:  Authorization provider set empty as default (#2107) ([aa77926](https://github.com/zowe/api-layer/commit/aa77926)), closes [#2107](https://github.com/zowe/api-layer/issues/2107)
* Bugfix:  Change url for v2 and mark plugin as v2 in case very old apiml v1 copies don't support same url  ([6f4257a](https://github.com/zowe/api-layer/commit/6f4257a)), closes [#2022](https://github.com/zowe/api-layer/issues/2022)
* Bugfix:  Update data model for infinispan storage in Caching service (#2156) ([38a1348](https://github.com/zowe/api-layer/commit/38a1348)), closes [#2156](https://github.com/zowe/api-layer/issues/2156)
