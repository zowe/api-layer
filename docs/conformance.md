# API Mediation Layer Conformance

## Application Conformance

The conformance requirements are split into:

1. **Day 1 requirements** - effective immediately
2. **Day 2 requirements** - effective later after API ML provides functionality to achieve them

### Day 1 Requirements

An application is *Zowe API ML conformant* if it follows these criteria:

1. An application must provide at least one service or UI
2. All exposed services of the application must be discoverable by the API Mediation Layer Discovery Service 
   - A service must be registered by one of the following methods:
     - Dynamic registration - **prefered** (Best practice)
     - Static definition - **minimum requirement**
   - The service must provide a default service ID that is prefixed by the provider name (for example: `ca`, `ibm`, `rocket`)
   - The service ID must be configurable externally after deployment
   - The service ID must be lower case, contain no symbols, and is limited to 64 characters
   - The API ID must follow the same rules as apply to Java packages, for example `org.zowe.apiml.apicatalog`
   - The published service URL must follow the gateway URL conventions:
     - For versioned APIs, service URLs must contain a service version before the service ID, in the following formats:
       - `api/v1/{serviceId}` reserved for REST APIs
       - `ui/v1/{serviceId}` reserved for UIs
       - `ws/v1/{serviceId}` reserved for Websockets
     - For non-versioned APIs or APIs versioned differently (e.g. z/OSMF) use the following formats: 
       - `api/{serviceId}` reserved for REST APIs
       - `ui/{serviceId}` reserved for UIs
       - `ws/{serviceId}` reserved for Websockets
    
3. WebSocket services 
     - Websocket connection creation and all subsequent communication between WebSocket client and server must be routed through the API ML Gateway.
     - WebSocket connections must be properly closed by the initiator through API ML Gateway.     

4. The API must be documented in Swagger/OpenAPI 2.0 specification.  For more information about Swagger, see https://swagger.io/resources/articles/documenting-apis-with-swagger/. Additionally, the following criteria must be satisfied: 
   - Documentation must be Swagger 2.0 compliant
   - Every public resource must be documented with a description of each resource
   - Every method of each REST endpoint must be documented
   - Every method of each REST endpoint must be demonstrated by an example
   - Every parameter (headers, query parameters, payload, cookies, etc.) must be documented with definitions of all possible values and their associated meanings
   - Every error code, including errors returned in the payload must be documented

Note: Websockets must be documented. The documentation location and format is determined by the provider.
Tip: We strongly recommend all documentation be reviewed by a technical writer.

5. API design must be consistent with the rest of the Zowe ecosystem. The following criteria must be satisfied:
   - Encoded slash must not be used. For example: `/abc%2fgef` cannot be used
   - The service must interpret values independent of their URL encoding
   - Request payloads must be in JSON format
   - lowerCamelCase should be used for names of resources, parameters, and JSON properties

6. Service responses
   - API Responses
     - If it is in JSON format, links must be relative and must not contain the schema, hostname, and port 
     - If there is a payload it should be in JSON format
   - WebSocket
     - Service URIs contained in WebSocket messages payload must be addressed through the API Meditation Layer Gateway.
     
   - UI
     - UI must use relative links and must not contain the schema, hostname, and port

7. Published services must be protected by mainframe-based Authentication and Authorization
   - The resources must be protected by mainframe credentials
   - Some endpoints such as the login endpoint (non-sensitive diagnostics information, or API documentation) can be unprotected 
   - Services must accept basic authentication - **minimum requirement**
   - Services should accept Zowe JWT token in the cookie - **prefered** (Best practice)
     Note: For more information, see [Zowe Authentication and Authorization Service](https://github.com/zowe/api-layer/wiki/Zowe-Authentication-and-Authorization-Service)

8. Service implementation should follow the [semantic versioning model](https://semver.org/)
   - At least the last two major versions must be supported by API services
   - The major service version must be supported for at least two years from its release

8. Zowe version compatibility
   - The service provider must update the service to ensure compatibility with the latest two major versions of Zowe
   - The service must be updated to be compatible with the most recent version of Zowe **within a month** after its release

10. The UI that runs behind the API ML Gateway must support routing to services via the API ML Gateway 
    - The UI must either refer to services and resources using relative URLs or absolute URLs must contain the API ML Gateway hostname and port.

Note: Static resources such as images can be addressed directly.

### Day 2 Requirements

1. Application UI that uses absolute URL to address services must provide URL transformation metadata to the API ML gateway
2. The application must follow Zowe rules for installation, operation, and diagnostics (TBD)

### Additional Labels

**High Availability** - The application that provides an API is *highly available* when multiple instances can be registered under the same service ID and provide the same responses regardless of which instance is accessed by an API client

### Conformance Validation
 * Initially confromance criteria are valiedated manually. 
 * Later an automated validation will be implemented as a REST API endpoint in the API ML. This validation includes a check of the Swagger JSON by an existing tool
