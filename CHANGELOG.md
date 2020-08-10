# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.
## `1.14.0`

- Prevent crashing of API ML on setting of null routes
- Add support to the X-Forwarded-* Headers
- Improve validator of configuration in enablers

## `1.13.0`

- Add CORS Headers Support 
- Option to set connection timeout for service
- SAF Keyrings support for ZAAS Client
- Spring Boot enabler configuration validation

## `1.12.0`

- Provide Zowe Authentication and Authorization Service (ZAAS) client
- Refresh the static client definitions from the API Catalog UI
- Switch to sso-auth instead of apiml-auth
- Added logout endpoint api doc
- Make jjwt only a test dependency
- Fix order of fetching JWT from request
- Implement request retrying for service instances

## `1.11.0`

- Allow users of the API Catalog to test public and private endpoints directly from the UI
- Provide Endpoints to get public keys for JWT verification (z/OSMF and Zowe)
- Log errors found while processing static definitions
- Use uppercase parameters for PassTicket service
