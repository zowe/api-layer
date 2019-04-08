# API Mediation Layer Conformance

## Application Conformance

An application is Zowe API ML conformant if it follow following criteria:

1. The application provides a discoverable API service that is registered dynamically to the discovery service
   - The service ID can be set the configuration of the application
   - The default service ID follows these rules:
     - Lower case, no symbols
     - Starts with the vendor name
   - At least one REST API is defined and routed
     - The API ID follow same rules as for Java packages (e.g. `org.zowe.apiml.apicatalog`)
   - The routing follows conventions for the gateway URL:
     - `api/v1/{serviceId}`, `ui/v1/{serviceId}`, `ws/v1/{serviceId}` for versioned APIs or `api/{serviceId}`, `ui/{serviceId}`, `ws/{serviceId}` for APIs that are not versioned or versioned differently (e.g. z/OSMF)
   - Names and titles follow guidelines - TODO Andrew Jandacek
2. The provided API is documented by Swagger/OpenAPI 2.0 JSON document. This document needs to be valid
3. The REST API follow best practices:
   - Encoded slash is not used
   - URL encoding of values in the URL does not change their interpretation
   - REST API is designed to follow: https://broadcom.ent.box.com/notes/288959969273
4. Authentication
   - The resources of REST APIs are protected by mainframe credentials
   - Some resources can be public
   - The basic authentication with valid mainframe credentials is accepted
   - The Zowe JWT token in the cookie is accepted (details in https://github.com/zowe/api-layer/wiki/Zowe-Authentication-and-Authorization-Service)
5. User interface (applies only if UI is provided)
   - UI uses relative URLs
   - Accepts JWT token
6. User documentation requirements
   - TODO Andrew  

### Validation 

Phase 1 is manual validation. Phase 2 is automated validation implemented as a REST API endpoint in the API ML that verifies rules and returns JSON list with results (pass/fail). A part of the validation is check of the Swagger JSON by an existing tool.
