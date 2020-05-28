# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## `1.12.0`

- Provide Zowe Authentication and Authorization Service (ZAAS) client
- Refresh the static client definitions from the API Catalog UI
- Switch to sso-auth instead of apiml-auth
- Added logout endpoint api doc
- Make jjwt only a test dependency
- Fix order of fetching JWT from request

## `1.11.0`

- Allow users of the API Catalog to test public and private endpoints directly from the UI
- Provide Endpoints to get public keys for JWT verification (z/OSMF and Zowe)
- Log errors found while processing static definitions
- Use uppercase parameters for PassTicket service
