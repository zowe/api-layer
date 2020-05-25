# API Mediation Layer Conformance

## Application Conformance

The conformance requirements are split into:

1. **Day 1 requirements** - effective immediately
2. **Day 2 requirements** - effective later after API ML provides functionality to achieve the requirements

### Day 1 Requirements

An application is *Zowe API ML conformant* if it follows these criteria:

1. An application must provide at least one service or UI
2. All exposed services of the application must be discoverable by the API Mediation Layer Discovery Service 
   - A service must be registered using one of the following methods:
     - Dynamic registration - **preferred** (best practice)
     - Static definition - **minimum requirement**
   - The service must provide a default service ID that is prefixed by the provider name (for example: `ca`, `ibm`, `rocket`)
   - The service ID must be configurable externally after deployment
   - The service ID must written in lower case, contain no symbols, and is limited to 64 characters
   - The API ID must follow the same rules as for Java packages. The example of the API ID: `org.zowe.apiml.apicatalog`
   - The published service URL must follow the gateway URL conventions:
     - For versioned APIs, service URLs must contain a service version before the service ID in the following formats:
       - `api/v1/{serviceId}` reserved for REST APIs
       - `ui/v1/{serviceId}` reserved for UIs
       - `ws/v1/{serviceId}` reserved for WebSockets
     - For non-versioned APIs or APIs versioned differently (e.g. z/OSMF), use the following formats: 
       - `api/{serviceId}` reserved for REST APIs
       - `ui/{serviceId}` reserved for UIs
       - `ws/{serviceId}` reserved for WebSockets

3. The API must be documented according to the Swagger/OpenAPI 2.0 or OpenAPI 3.0 specification. For more information about Swagger, see [Swagger documentation](https://swagger.io/resources/articles/documenting-apis-with-swagger/). Additionally, the following criteria must be satisfied: 
   - Documentation must be Swagger/OpenAPI 2.0/OpenAPI 3.0 compliant
   - Every public resource must be documented with a description of each resource
   - Every method of each REST endpoint must be documented
   - Every method of each REST endpoint must be demonstrated by an example
   - Every parameter (headers, query parameters, payload, cookies, etc.) must be documented with definitions of all possible values and their associated meanings
   - Every HTTP error code must be documented. If endpoint has additional more granular error codes just the documentation reference can be provided for these.

Note: WebSockets must be documented. The documentation location and format is determined by the provider.

Tip: We strongly recommend all documentation be reviewed by a technical writer.

4. API naming and addressing must be consistent with the rest of the Zowe ecosystem. The following criteria apply:
   - Encoded slash is not used - **preferred** (best practice)
   - The service must interpret values independent of their URL encoding
   - `lowerCamelCase` should be used for names of resources, parameters, and JSON properties

5. Service requests and responses
   - API 
     - Request and response payloads should be in JSON or binary data format - **preferred** (best practice)
     - In JSON format, links must be relative, and must not contain the schema, hostname, and port 
   - WebSocket
     - Service URIs contained in WebSocket messages payload must be addressed through the API Meditation Layer Gateway.
     
   - UI
     - UI must use relative links and must not contain the schema, hostname, and port

6. Published services must be protected by mainframe-based Authentication and Authorization
   - The resources must be protected by mainframe credentials
   - Some endpoints such as the login endpoint (non-sensitive diagnostics information, or API documentation) can be unprotected 
   - Services must accept basic authentication - **minimum requirement**
   - Services should accept Zowe JWT token in the cookie - **preferred** (best practice)

Note: For more information, see [ZAAS Client](https://docs.zowe.org/stable/extend/extend-apiml/api-mediation-security.html#zaas-client)

7. Service implementation should follow the [semantic versioning model](https://semver.org/)
   - At least the last two major versions must be supported by API services
   - The major service version must be supported for at least two years from its release

8. UI that runs behind the API ML Gateway must support routing to services through the API ML Gateway 
    - UI must use only relative URLs.

9. WebSocket services
     - WebSocket connection creation, all subsequent communication between WebSocket client, and server must be routed through the API ML Gateway.
     - WebSocket connections must be closed by the initiator through API ML Gateway.

### Day 2 Requirements

1. Application UI that uses absolute URL to address services must provide URL transformation metadata to the API ML gateway.
2. The application must follow Zowe rules for installation, operation, and diagnostics.

### Additional Labels

**High Availability** - The application that provides an API is *highly available* when multiple instances can be registered under the same service ID and provide the same responses regardless of the instance that is accessed by an API client.

### Conformance Validation
 * Initially conformance criteria are validated manually. 
 * Later an automated validation will be implemented as a REST API endpoint in the API ML. The automated validation will check Swagger JSON files.
