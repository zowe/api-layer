
# Secured endpoints with SAF resource 

Any endpoint can have limited access by checking SAF resource of logged user. This feature is implemented in module
`apiml-security-common`. Therefore, it can be used in each application with this dependency.

Verification of SAF resource can be provided via three providers:
- _the highest priority_: REST endpoint call (ZSS or similar one)
- JZOS
- _the lowest priority_: dummy implementation (defined in a file)

**note**: The first available by priority will be used.

## Checking providers

### REST endpoint call

This provider is once way how to enable the feature outside the mainframe (ie. running in Docker).

- Method: `GET`
- URL: `{base path}/{userId}/{class}/{entity}/{level}`
- Response:
```json5
    {
        "authorized": "{true|false}",
        "error": "{true|false}",
        "message": "{message}"
    }
```

**note**: see also ZSS implementation https://github.com/zowe/zss/blob/master/c/authService.c

#### Configuration

`apiml.security.safEndpoint.enabled`
- `true` or `false`, 'false' as default
- to enable endpoint

`apiml.security.safEndpoint.url`
- base path of endpoint's URL (`{base path}/{userId}/{class}/{entity}/{level}`) 
- as default `http://localhost:8542/saf-auth` (default location of ZSS endpoint)

### JZOS

This provider is the easiest way, how to use the feature on the mainframe.

Enable, when classes `com.ibm.os390.security.PlatformAccessControl` and `com.ibm.os390.security.PlatformReturned`
are available on classpath. It uses [method](https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.zsecurity.api.80.doc/com.ibm.os390.security/com/ibm/os390/security/PlatformAccessControl.html?view=kc#checkPermission-java.lang.String-java.lang.String-java.lang.String-int-), so
for right using, definition of method must be also matching.

### Dummy implementation

This provider is for testing purpose outside the mainframe.

You can create file `saf.yml` and locate it in the folder, where is application running or create file `mock-saf.yml` in the
test module (root folder). The highest priority is to read file outside the JAR. A file (inner or outside) has to exist.

Structure of YML file:
```yaml
  safAccess:
    {CLASS}:
      {RESOURCE}:
        - {UserID}
```

**notes**:
- Classes and resources are mapped into a map, user IDs into list.
- load method does not support formatting with dots, like {CLASS}.{RESOURCE}, each element has to be separated
- safAccess is not required to define empty file
- classes and resources cannot be defined without user ID list
- method could be called multiple times, definition are added to current (loaded)
- when a user has multiple definition of same class and resource, just the highest access level is loaded

## Protecting Access to REST API Endpoints

The REST API endpoints can be protected by the `org.springframework.security.access.prepost.PreAuthorize` annotation.

The modul `apiml-security-common` defines two new security expressions:

- `boolean hasSafResourceAccess(String resourceClass, String resourceName, String accessLevel)` - returns `true` when user has access to the resource
- `boolean hasSafServiceResourceAccess(String resourceNameSuffix, String accessLevel)` - similar as the previous one but the resource class and resource name prefix is taken from the service configuration under keys `apiml.security.saf.serviceResourceClass` and `apiml.security.saf.resourceNamePrefix`.

So you can do following:

```java
@GetMapping("/safProtectedResource")
@PreAuthorize("hasSafResourceAccess('FACILITY', 'BPX.SERVER', 'UPDATE')")
public Map<String, String> safProtectedResource(@ApiIgnore Authentication authentication) { /*...*/ }

@GetMapping("/anotherSafProtectedResource")
@PreAuthorize("hasSafServiceResourceAccess('RESOURCE', 'READ')")
public Map<String, String> anotherSafProtectedResource(@ApiIgnore Authentication authentication) { /*...*/ }
```

The second `@PreAuthorize` expression `hasSafServiceResourceAccess('RESOURCE', 'READ')` is effectively translated to `hasSafResourceAccess('${apiml.security.saf.serviceResourceClass}', '${apiml.security.saf.resourceNamePrefix}RESOURCE', 'READ')`. In case of default values it would be `hasSafResourceAccess('ZOWE', 'APIML.RESOURCE', 'READ')`.

### Configuration

Default value of `apiml.security.saf.serviceResourceClass` is `ZOWE`.

Default value of `apiml.security.saf.resourceNamePrefix` is `APIML.`.
