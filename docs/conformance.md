# API Mediation Layer Conformance

## Application Conformance

An application is Zowe API ML conformant if it follows these criteria:

1. The application provides a discoverable API service that is registered dynamically to the Discovery Service
   - The service ID can be set in the configuration of the application
   - The default service ID follows these rules:
     - Lower case, no symbols
     - Starts with the name of the vendor
   - At least one REST API is defined and routed
   - The API ID follows same rules as for Java packages
     **Example:**
     `org.zowe.apiml.apicatalog`
   - The routing follows the conventions for the Gateway URL:
     - For versioned APIs:
     
      `api/v1/{serviceId}`, `ui/v1/{serviceId}`, `ws/v1/{serviceId}` for versioned APIs or
     - For non-versioned APIs or APIs versioned differently (e.g.z/OSMF):
     
      `api/{serviceId}`, `ui/{serviceId}`, `ws/{serviceId}` 
   - Names and titles reviewed by Tech Materials (previously Tech Info) 
2. The provided API is documented by Swagger/OpenAPI 2.0 JSON document. This document needs to be valid
3. The REST API follows these best practices:
   - Encoded slash is not used
   - URL encoding of values in the URL does not change how the values are interpreted 
   - REST API is designed to follow this URL: https://broadcom.ent.box.com/notes/288959969273
4. Authentication
   - The resources of REST APIs are protected by mainframe credentials
   - Some resources can be public
   - The basic authentication with valid mainframe credentials is accepted
   - The Zowe JWT token in the cookie is accepted (For more information, see https://github.com/zowe/api-layer/wiki/Zowe-Authentication-and-Authorization-Service)
5. User interface (applies only if the UI is provided)
   - UI uses relative URLs
   - Accepts JWT token
6. User documentation requirements
   - Reviewed by Tech Materials (previously Tech Info)  

### Validation 

Phase 1 is manual validation. Phase 2 is automated validation implemented as a REST API endpoint in the API ML that verifies rules and returns a JSON list with results (pass/fail). This validation includes a check of the Swagger JSON by an existing tool.
