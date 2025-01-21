# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## `APIML 3.1.1 / Zowe 3.1.0 (2025-02-03)`

* Feature:  Override external URL for additional registration (#3935) ([d5dd912](https://github.com/zowe/api-layer/commit/d5dd912)), closes [#3935](https://github.com/zowe/api-layer/issues/3935)
* Feature:  Support OIDC token to authenticate in API Catalog (#3925) ([a4ead1d](https://github.com/zowe/api-layer/commit/a4ead1d)), closes [#3925](https://github.com/zowe/api-layer/issues/3925)
* Feature:  Allows to obtain certificates from multiple sources (#3914) ([2e028cb](https://github.com/zowe/api-layer/commit/2e028cb)), closes [#3914](https://github.com/zowe/api-layer/issues/3914)
* Feature:  Rate limit per service (#3903) ([cad63cb](https://github.com/zowe/api-layer/commit/cad63cb)), closes [#3903](https://github.com/zowe/api-layer/issues/3903)
* Feature:  Add validate oidc token call to zaas client (#3897) ([3f0ac10](https://github.com/zowe/api-layer/commit/3f0ac10)), closes [#3897](https://github.com/zowe/api-layer/issues/3897)
* Feature:  Limit API usage (#3868) ([bdbd3cb](https://github.com/zowe/api-layer/commit/bdbd3cb)), closes [#3868](https://github.com/zowe/api-layer/issues/3868)
* Feature:  Java sample to authenticate with client certificate (#3862) ([992deb3](https://github.com/zowe/api-layer/commit/992deb3)), closes [#3862](https://github.com/zowe/api-layer/issues/3862)
* Feature:  Support client AT-TLS setting (#3828) ([75cf96b](https://github.com/zowe/api-layer/commit/75cf96b)), closes [#3828](https://github.com/zowe/api-layer/issues/3828)


* Bugfix:  Make "native" the default SAF authorization provider (#3937) ([f4aafe6](https://github.com/zowe/api-layer/commit/f4aafe6)), closes [#3937](https://github.com/zowe/api-layer/issues/3937)
* Bugfix:  z/OSMF static definition conversion (#3938) ([d998b5a](https://github.com/zowe/api-layer/commit/d998b5a)), closes [#3938](https://github.com/zowe/api-layer/issues/3938)
* Bugfix:  Do not leak 'exampleSetFlag' in api doc (v3.x.x) (#3933) ([ee31cd9](https://github.com/zowe/api-layer/commit/ee31cd9)), closes [#3933](https://github.com/zowe/api-layer/issues/3933)
* Bugfix:  Improve error handling in case of failure when retrieving API doc (#3932) ([3fb0d59](https://github.com/zowe/api-layer/commit/3fb0d59)), closes [#3932](https://github.com/zowe/api-layer/issues/3932)
* Bugfix:  Remove the word 'central' from the log messages (#3929) ([1ce5918](https://github.com/zowe/api-layer/commit/1ce5918)), closes [#3929](https://github.com/zowe/api-layer/issues/3929)
* Bugfix:  Fix services endpoint to show correct list of onboarded services (#3919) ([3d20320](https://github.com/zowe/api-layer/commit/3d20320)), closes [#3919](https://github.com/zowe/api-layer/issues/3919)
* Bugfix:  Auto conversion during z/OSMF static definition creation (#3930) ([1106cb9](https://github.com/zowe/api-layer/commit/1106cb9)), closes [#3930](https://github.com/zowe/api-layer/issues/3930)
* Bugfix:  Improve untrusted certificate message when certificate is not forwarded (#3927) ([25ae2ed](https://github.com/zowe/api-layer/commit/25ae2ed)), closes [#3321](https://github.com/zowe/api-layer/issues/3321)
* Bugfix:  Correct apiBasePath & server URL for primary and additional Gateways (#3922) ([aa50350](https://github.com/zowe/api-layer/commit/aa50350)), closes [#3922](https://github.com/zowe/api-layer/issues/3922)
* Bugfix:  Enable infinispan debug logs messages with caching service in debug mode (#3925) ([6c6306a](https://github.com/zowe/api-layer/commit/6c6306a)), closes [#3905](https://github.com/zowe/api-layer/issues/3905)
* Bugfix:  Specify content type when validating OIDC (#3902) ([ae65470](https://github.com/zowe/api-layer/commit/ae65470)), closes [#3902](https://github.com/zowe/api-layer/issues/3902)
* Bugfix:  Fix handling unavailable services (#3879) ([d285a33](https://github.com/zowe/api-layer/commit/d285a33)), closes [#3879](https://github.com/zowe/api-layer/issues/3879)
* Bugfix:  Semantic of onboarded Gateways in the multitenancy deployment (#3884) ([a94029b](https://github.com/zowe/api-layer/commit/a94029b)), closes [#3884](https://github.com/zowe/api-layer/issues/3884)
* Bugfix:  Upgrade spring boot with HTTP headers workaround (#3882) ([8054063](https://github.com/zowe/api-layer/commit/8054063)), closes [#3882](https://github.com/zowe/api-layer/issues/3882)
* Bugfix:  Handle exceptions that could arise in the passticket authentication schema (#3871) ([defe1dc](https://github.com/zowe/api-layer/commit/defe1dc)), closes [#3871](https://github.com/zowe/api-layer/issues/3871)
* Bugfix:  Use default JDK DNS resolver (#3877) ([bf1f2ed](https://github.com/zowe/api-layer/commit/bf1f2ed)), closes [#3877](https://github.com/zowe/api-layer/issues/3877)
* Bugfix:  Trailing quotes in z/OSMF static definition not having matching initial ones (#3875) ([adefa8a](https://github.com/zowe/api-layer/commit/adefa8a)), closes [#3875](https://github.com/zowe/api-layer/issues/3875)
* Bugfix:  Restore handling mode of x-forwarded-prefix as it is used in v2 (#3874) ([a18df27](https://github.com/zowe/api-layer/commit/a18df27)), closes [#3874](https://github.com/zowe/api-layer/issues/3874)
* Bugfix:  Do not fail when headers cannot be modified (#3845) ([084eb6d](https://github.com/zowe/api-layer/commit/084eb6d)), closes [#3845](https://github.com/zowe/api-layer/issues/3845)
* Bugfix:  Fix error message in case of TLS error (#3864) ([945fc9c](https://github.com/zowe/api-layer/commit/945fc9c)), closes [#3864](https://github.com/zowe/api-layer/issues/3864)
* Bugfix:  Update Gateway schema with OIDC config parameters (#3867) ([19ece5e](https://github.com/zowe/api-layer/commit/19ece5e)), closes [#3867](https://github.com/zowe/api-layer/issues/3867)
* Bugfix:  Respect SSL strictness in enabler (#3813) ([bc55168](https://github.com/zowe/api-layer/commit/bc55168)), closes [#3813](https://github.com/zowe/api-layer/issues/3813)
* Bugfix:  Configure SSL context for webclient (#3811) ([476c69b](https://github.com/zowe/api-layer/commit/476c69b)), closes [#3811](https://github.com/zowe/api-layer/issues/3811)
* Bugfix:  Minor fixes in logs (#3806) ([5abc91e](https://github.com/zowe/api-layer/commit/5abc91e)), closes [#3806](https://github.com/zowe/api-layer/issues/3806)

## `APIML 3.0.36 / Zowe 3.0.0 (2024-10-01)`


__Breaking changes in API ML__

| Change in Zowe V3                                                                                                             | Required action
|-------------------------------------------------------------------------------------------------------------------------------| --
| Authentication endpoints no longer support the route /api/v1/gateway. Only /gateway/api/v1 is now supported.                  | If you use the endpoints directly, change the URLs to start with /gateway/api/v1. If you use ZAAS client to integrate with API Mediation Layer, no action is required as the change is handled in the ZAAS client code.
| Spring Enabler has been updated to Spring Boot 3 and Spring 6. Spring Boot 2 and Spring 5 versions are no longer be supported | Upgrade extending services based on the Spring Enabler to Spring Boot 3 and Spring 6.
| Datasets API has been archived                                                                                                | This service was disabled by default in Version 2. If you enable the service via components.data-sets.enabled: true and use the APIs documented in Data sets Swagger, it is necessary to move to the usage of the similar z/OSMF endpoints.
| Jobs API will be archived                                                                                                     | The service was disabled by default in Version 2. If you enable the service via components.jobs.enabled: true and use the APIs documented in Jobs Swagger, it is necessary to move to the usage of the similar z/OSMF endpoints.
| Metrics service has been archived                                                                                             | The service was in Technical Preview. Currently there is no replacement. In V3, the Open Telemetry standard will be implemented, which will serve as a replacement.
| IMS API has been archived                                                                                                     | The service was not fully supported. If you were using the API, please reach out to the IBM team for follow-up steps.
| Java 17 is required to run the API Mediation Layer                                                                            | For V3, it is necessary to update z/OS to version 2.5 or later as this brings support of Java 17. It is necessary to install Java 17 and provide the path to Java 17 to Zowe Java configuration.
| z/OSMF in version V2R5 with APAR PH12143 applied                                                                              | If you are running a version of z/OS before 3.1, validate that the PH12143 APAR was applied to the z/OSMF installation used by Zowe. The Zowe YAML parameter components.gateway.apiml.security.auth.zosmf.jwtAutoconfiguration for the gateway component has changed. The value auto is no longer allowed. Choose either the default jwt or ltpa depending on if your z/OSMF is set up for JWT use as recommended. See example-zowe.yaml for new component values.
| Configuration of keyrings now requires transformation from safkeyring://// to safkeyring://                                   | If your Zowe configuration contains safkeyring:////, change this part to safkeyring://.
| Support access to z/OSMF only through /ibmzosmf route. V3 will not support access through the /zosmf route                    | If you use z/OSMF via {apimlUrl}/zosmf/{zosmfEndpoint} you need to move to {apimlUrl}/ibmzosmf/{zosmfEndpoint}.

__New features and enhancements in API ML__

The current API Gateway contains the Authentication and Authorization Service. This service will be separated as a standalone service. The Authentication and Authorization Service is the only API ML service that directly requires z/OS.

__Changelog__

* Feature:  Use networking standard config (improved) (#3765) ([aef67a3](https://github.com/zowe/api-layer/commit/aef67a3)), closes [#3765](https://github.com/zowe/api-layer/issues/3765)
* Feature:  GraphiQL Playground (#3660) ([9e23fba](https://github.com/zowe/api-layer/commit/9e23fba)), closes [#3660](https://github.com/zowe/api-layer/issues/3660)
* Feature:  Websocket connection configuration (#3700) ([eb98b13](https://github.com/zowe/api-layer/commit/eb98b13)), closes [#3700](https://github.com/zowe/api-layer/issues/3700)
* Feature:  Disable routing to Discovery and ZAAS from Gateway (#3688) ([1139243](https://github.com/zowe/api-layer/commit/1139243)), closes [#3294](https://github.com/zowe/api-layer/issues/3294)
* Feature:  Add deterministic routing and sticky session load balancing (#3658) ([0f62119](https://github.com/zowe/api-layer/commit/0f62119)), closes [#3658](https://github.com/zowe/api-layer/issues/3658)
* Feature:  Create ZAAS service, use Cloud Gateway as Gateway (#3568) ([4953604](https://github.com/zowe/api-layer/commit/4953604)), closes [#3568](https://github.com/zowe/api-layer/issues/3568) [#3567](https://github.com/zowe/api-layer/issues/3567) [#3571](https://github.com/zowe/api-layer/issues/3571) [#3572](https://github.com/zowe/api-layer/issues/3572)
* Feature:  Catalog version in footer for Login, Dasboard and Detail pages (#3554) ([fd75d1b](https://github.com/zowe/api-layer/commit/fd75d1b)), closes [#3554](https://github.com/zowe/api-layer/issues/3554)
* Feature:  Cloud Gateway support of AT-TLS (#3545) ([e9c9da6](https://github.com/zowe/api-layer/commit/e9c9da6)), closes [#3545](https://github.com/zowe/api-layer/issues/3545)
* Feature:  Add OIDC login flow schema and enable allowedUsers customization in zowe.yaml (#3533) ([43a7c57](https://github.com/zowe/api-layer/commit/43a7c57)), closes [#3533](https://github.com/zowe/api-layer/issues/3533)
* Feature:  OIDC authentication flow (#3510) ([0275eff](https://github.com/zowe/api-layer/commit/0275eff)), closes [#3510](https://github.com/zowe/api-layer/issues/3510)


* Bugfix:  Fix Discovery Eureka response if the service is not registred to allow to reconnect by Enabler (#3795) ([9f58010](https://github.com/zowe/api-layer/commit/9f58010)), closes [#3795](https://github.com/zowe/api-layer/issues/3795)
* Bugfix:  Move security configuration back to gateway section (#3775) ([2513ff1](https://github.com/zowe/api-layer/commit/2513ff1)), closes [#3775](https://github.com/zowe/api-layer/issues/3775)
* Bugfix:  Gateway ends with internal server error if cookies are invalid (#3767) ([eeaee5c](https://github.com/zowe/api-layer/commit/eeaee5c)), closes [#3767](https://github.com/zowe/api-layer/issues/3767)
* Bugfix:  Do not resolve hostname when not required  (#3751) ([39e75b1](https://github.com/zowe/api-layer/commit/39e75b1)), closes [#3751](https://github.com/zowe/api-layer/issues/3751)
* Bugfix:  ClosableHttpClient.execute() resource leak on API catalog (#3722) ([a330907](https://github.com/zowe/api-layer/commit/a330907)), closes [#3722](https://github.com/zowe/api-layer/issues/3722)
* Bugfix:  The API ML prefix for registry configuration (#3746) ([f972d0c](https://github.com/zowe/api-layer/commit/f972d0c)), closes [#3746](https://github.com/zowe/api-layer/issues/3746)
* Bugfix:  ZAAS reads configuration from Gateway as default, possible to override with local configuration (#3744) ([fc7ae4e](https://github.com/zowe/api-layer/commit/fc7ae4e)), closes [#3744](https://github.com/zowe/api-layer/issues/3744)
* Bugfix:  Remove "AUTO" from JWT configuration and clean up outdated APARs from mock service (#3717) ([a81abe8](https://github.com/zowe/api-layer/commit/a81abe8)), closes [#3717](https://github.com/zowe/api-layer/issues/3717)
* Bugfix:  Update default javax.net.ssl log levels (#3716) ([f46561c](https://github.com/zowe/api-layer/commit/f46561c)), closes [#3716](https://github.com/zowe/api-layer/issues/3716)
* Bugfix:  Use Zowe provided java location if available (#3714) ([fb2863c](https://github.com/zowe/api-layer/commit/fb2863c)), closes [#3714](https://github.com/zowe/api-layer/issues/3714)
* Bugfix:  Stacktrace on unreachable swagger and remove handling for deprecated method (#3699) ([3606dd6](https://github.com/zowe/api-layer/commit/3606dd6)), closes [#3699](https://github.com/zowe/api-layer/issues/3699)
* Bugfix:  Protect health endpoint with authentication as default (#3676) ([806de5c](https://github.com/zowe/api-layer/commit/806de5c)), closes [#3676](https://github.com/zowe/api-layer/issues/3676)
* Bugfix:  Pretty path URL in Gateway Swagger documentation in the API catalog (#3679) ([a88ace6](https://github.com/zowe/api-layer/commit/a88ace6)), closes [#3679](https://github.com/zowe/api-layer/issues/3679)
* Bugfix:  Fix Swagger API documentation for Gateway (#3678) ([abbd08f](https://github.com/zowe/api-layer/commit/abbd08f)), closes [#3678](https://github.com/zowe/api-layer/issues/3678)
* Bugfix:  Support customized code snippets in case of endpoint with query params (#3666) ([7c5c067](https://github.com/zowe/api-layer/commit/7c5c067)), closes [#3666](https://github.com/zowe/api-layer/issues/3666)
* Bugfix:  Tweak gateway status page to have consistent casing (#3560) ([7d55cd9](https://github.com/zowe/api-layer/commit/7d55cd9)), closes [#3560](https://github.com/zowe/api-layer/issues/3560)
* Bugfix:  Independent scanning and loading of extension's classes (#3548) ([8d2d3bb](https://github.com/zowe/api-layer/commit/8d2d3bb)), closes [#3548](https://github.com/zowe/api-layer/issues/3548)
* Bugfix:  Fix SSL Context switching (#3531) ([e7575f6](https://github.com/zowe/api-layer/commit/e7575f6)), closes [#3531](https://github.com/zowe/api-layer/issues/3531)
* Bugfix:  Fix z/OSMF URL (#3478) ([567c261](https://github.com/zowe/api-layer/commit/567c261)), closes [#3478](https://github.com/zowe/api-layer/issues/3478)
* Bugfix:  Updating of SSL configuration in the Tomcat (#3403) ([ba86c0e](https://github.com/zowe/api-layer/commit/ba86c0e)), closes [#3403](https://github.com/zowe/api-layer/issues/3403)
* Bugfix:  Keyring init z/OS (#3314) ([0a97850](https://github.com/zowe/api-layer/commit/0a97850)), closes [#3314](https://github.com/zowe/api-layer/issues/3314)
* Bugfix:  Using `ibmzosmf` as service ID (#3302) ([305dea3](https://github.com/zowe/api-layer/commit/305dea3)), closes [#3302](https://github.com/zowe/api-layer/issues/3302)
* Bugfix:  Update z/OSMF service ID (#3296) ([037391a](https://github.com/zowe/api-layer/commit/037391a)), closes [#3296](https://github.com/zowe/api-layer/issues/3296)
* Bugfix:  Update serviceId in the Gateway starting script (#3255) ([4acb107](https://github.com/zowe/api-layer/commit/4acb107)), closes [#2889](https://github.com/zowe/api-layer/issues/2889)
* Bugfix:  Fix truststore for websockets in Spring Cloud Gateway (#3248) ([96c4cc8](https://github.com/zowe/api-layer/commit/96c4cc8)), closes [#3248](https://github.com/zowe/api-layer/issues/3248)
* Bugfix:  Fixing static definition of z/OSMF in discovery package (#3251) ([4c3ccb2](https://github.com/zowe/api-layer/commit/4c3ccb2)), closes [#2889](https://github.com/zowe/api-layer/issues/2889)


## Previous versions

To show changelog of older versions follow on one these links:
 - [v2.x.x](https://github.com/zowe/api-layer/blob/v2.x.x/CHANGELOG.md)
 - [v1.x.x](https://github.com/zowe/api-layer/blob/v1.x.x/CHANGELOG.md)
