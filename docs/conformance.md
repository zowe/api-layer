# API Mediation Layer Conformance

## Application Conformance

An application is Zowe API ML conformant if it follows these criteria:

1. The application provides a discoverable API service that is registered dynamically to the Discovery Service
   - The service ID can be set in the configuration of the application by the system programmer
   - The application provides a default service ID that is prefixed by the provider name (for example: 'ca', 'ibm', 'rocket')
   - Every service ID follows these rules:
     - Lower case, no symbols (the service ID is case insensitive for users and can be used in places that does not allow different case or symbols such as SAF resource names)
     - It has up to 64 characters
   - At least one REST API is defined and routed
   - The API ID follows same rules as for Java packages

      `org.zowe.apiml.apicatalog`

   - The routing follows the conventions for the Gateway URL:
     - For versioned APIs:

      `api/v1/{serviceId}`, `ui/v1/{serviceId}`, `ws/v1/{serviceId}` for versioned APIs or
     - For non-versioned APIs or APIs versioned differently (e.g.z/OSMF):

      `api/{serviceId}`, `ui/{serviceId}`, `ws/{serviceId}`
   - Names and titles reviewed by Tech Materials (previously Tech Info)
2. The provided REST API is documented by Swagger/OpenAPI 2.0 JSON document. This document needs to be valid
3. The REST API follows these best practices:
   - Encoded slash is not used
   - URL encoding of values in the URL does not change how the values are interpreted
   - REST API is designed to follow rules defined at https://broadcom.ent.box.com/notes/288959969273
4. Authentication
   - The resources of REST APIs are protected by mainframe credentials
   - Some resources can be public
   - The basic authentication with valid mainframe credentials is accepted
   - The Zowe JWT token in the cookie is accepted (For more information, see https://github.com/zowe/api-layer/wiki/Zowe-Authentication-and-Authorization-Service)
5. User interface (applies only if the UI is provided)
   - UI uses relative URLs
   - (Proposal) If the application uses absolute URLs, it needs to be aware of running behind API gateway and use absolute URLs that are routed via the API gateway
   - Accepts JWT token
6. User documentation requirements
   - Reviewed by Tech Materials (previously Tech Info)  
7. The provided WebSocket APIs need to be routed via the `ws/vn/serviceId` path
   - There are no rules about the format of WebSocket messages
   - The public WebSocket APIs need to be documented in the documentation of the application

### Validation 

Phase 1 is manual validation. Phase 2 is automated validation implemented as a REST API endpoint in the API ML that verifies rules and returns a JSON list with results (pass/fail). This validation includes a check of the Swagger JSON by an existing tool.
