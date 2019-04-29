# API Mediation Layer Conformance

## Application Conformance

The conformance requirements are split into:

1. **Day 1 requirements** - effective immediately
2. **Day 2 requirements** - effective later after API ML provides functionality to achieve them

### Day 1 Requirements

An application is *Zowe API ML conformant* if it follows these criteria:

1. The application must provide a discoverable API service that is registered dynamically to the Discovery Service
   - The system programmer must be able to set the service ID in the configuration of the application
   - The application must provides a default service ID that is prefixed by the provider name (for example: `ca`, `ibm`, `rocket`)
   - Every service ID must follow these rules:
     - Lower case, no symbols (the service ID is case insensitive for users and can be used in places that does not allow different case or symbols such as SAF resource names)
     - It has up to 64 characters
   - At least one REST API must be defined and routed
   - The API ID must follow same rules as for Java packages, for example `org.zowe.apiml.apicatalog`
   - The routing must follow the conventions for the gateway URL:
     - For versioned APIs: `api/v1/{serviceId}`, `ui/v1/{serviceId}`, `ws/v1/{serviceId}`
     - For non-versioned APIs or APIs versioned differently (e.g. z/OSMF): `api/{serviceId}`, `ui/{serviceId}`, `ws/{serviceId}`
     - The requirements for UI routing must be followed since Day 1
   - Names and titles reviewed by Tech Materials (previously Tech Info)
2. The provided REST API is documented by Swagger/OpenAPI 2.0 JSON document. This document needs to be valid
3. The REST API must follow these best practices:
   - Encoded slash is not used
   - URL encoding of values in the URL does not change how the values are interpreted
4. REST API should be designed to follow rules defined at <https://broadcom.ent.box.com/notes/288959969273>
   - JSON must be the primary type for requests and responses
   - lowerCamelCase must be used for names of resources, parameters, and properties
5. Authentication
   - The resources of REST APIs are protected by mainframe credentials
   - Some resources can be public
   - The basic authentication with valid mainframe credentials is accepted
   - The Zowe JWT token in the cookie is accepted (For more information, see <https://github.com/zowe/api-layer/wiki/Zowe-Authentication-and-Authorization-Service)>
6. User documentation requirements
   - Reviewed by Tech Materials (previously Tech Info)  
   - Every public resource must be documented and the meaning of each resource is described
   - Every method must be documented and use cases for these methods are explained
   - Every use case must be demonstrated by an example
   - Every parameter must be documented and all possible values and their meaning is defined
7. API versioning
   - API versioning must be follow semantic versioning
   - At least two last major versions must be supported by API services
   - API version must be supported for at least two years
8. The provided WebSocket APIs must be routed via the `ws/vn/serviceId` path
   - There are no rules about the format of WebSocket messages
   - The public WebSocket APIs must be documented in the documentation of the application
9. Zowe version support
   - The application must support last two major versions of Zowe is possible
   - The applications must support the most recent version of Zowe at least a month after its release
  
### Day 2 Requirements

1. User interface must support routing via the API ML gateway (applies only if the UI is provided)
   - Either UI uses relative URLs or uses a method that provides absolute URLs that are routed via the gateway:
     - If the application uses absolute URLs, it needs to be aware of running behind API gateway and use absolute URLs that are routed via the API gateway
     - (Not available) Application provides URL transformation metadata to the API gateway
2. The application must follow Zowe rules for installation, operation, and diagnostics (TBD)

### Additional Labels

**High Availability** - the application that provides API is *highly available* when multiple instances can be registered under the same user ID and provide the same responses no matter which instance is accessed by an API client

### Validation

Phase 1 is manual validation. Phase 2 is automated validation implemented as a REST API endpoint in the API ML that verifies rules and returns a JSON list with results (pass/fail). This validation includes a check of the Swagger JSON by an existing tool.
