# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.
## `1.17.0`

- Feature: Multiple versions of one API will be shown in the Catalog if configured. [#844](https://github.com/zowe/api-layer/issues/844)
- Feature: Setting of APIML_DEBUG_MODE_ENABLED in instance.env is properly passed on to the all API ML services. [#901](https://github.com/zowe/api-layer/issues/901)
- Bugfix: Improve returned information while logging out via logout on Gateway. [#831](https://github.com/zowe/api-layer/issues/831) 
- Bugfix: Update API paths for the API ML in the API Catalog to use the service id in front. [#853](https://github.com/zowe/api-layer/issues/853) 

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
