# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.
## `1.15.0`

- Feature: API Path Pattern supports serviceId as first element [#688](https://github.com/zowe/api-layer/issues/688)
- Feature: Add SAF Provider as a possible authentication provider [#472](https://github.com/zowe/api-layer/issues/472)
- Bugfix: Fix SSL validation when Eureka is running in HTTP mode [#792](https://github.com/zowe/api-layer/issues/792)
- Bugfix: Fix problem in error handling when no api-doc is available [#571](https://github.com/zowe/api-layer/issues/571)
- Feature: Provide the Swagger URL for zOSMF [#665](https://github.com/zowe/api-layer/issues/665)
- Feature: Allow character encoding by default [#777](https://github.com/zowe/api-layer/issues/777)

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
