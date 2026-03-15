# #3299 — Fix homepage URL at API Catalog

**GitHub Issue:** https://github.com/zowe/api-layer/issues/3299
**Labels:** enhancement, good first issue, Priority: Medium | **Created:** 2024-01-30 | **State:** open

---

## Description

The API Catalog displays each service's homepage URL as provided by the service during Eureka registration. This URL typically points directly to the service's host and port (e.g., `https://internal-host:7552/ui`), which is inaccessible from outside the z/OS network. Users clicking the link receive a connection error.

`ContainerService.getInstanceHomePageUrl()` already calls `transformService.transformURL()` to convert internal URLs to Gateway URLs. However, when the transformation fails (e.g., because the service has no registered UI route), the original internal URL is returned unchanged rather than being hidden or replaced with a Gateway-relative fallback.

---

## Acceptance Criteria

- When `transformURL()` succeeds, the catalog displays the transformed Gateway URL (existing behaviour, unchanged).
- When `transformURL()` fails for a non-APIML service with no UI route, the catalog displays **no homepage URL** (empty or null) rather than the internal service URL.
- The homepage link is hidden in the API Catalog UI when `homePageUrl` is null or empty.
- APIML services (gateway, discovery, catalog itself) continue to display their original URLs unchanged (they are not transformed).
- The `WARN` log for a transformation failure on an API-only service (one without a UI route) is demoted to `DEBUG` — a missing UI route is normal and should not generate a warning on every catalog refresh.

---

## Technical Solution

### Files to change

- `api-catalog-services/src/main/java/org/zowe/apiml/apicatalog/swagger/ContainerService.java`
- `api-catalog-ui/frontend/src/components/ServiceTile/ServiceTile.jsx` (or equivalent) — hide homepage link when URL is null/empty

### Changes

**`ContainerService.getInstanceHomePageUrl()`**

```java
private String getInstanceHomePageUrl(ServiceInstance serviceInstance) {
    String serviceId = StringUtils.lowerCase(serviceInstance.getServiceId());
    String instanceHomePage = getHomePageUrl(serviceInstance);

    if (!hasHomePage(serviceInstance) || GATEWAY.getServiceId().equals(serviceId)) {
        return instanceHomePage; // APIML services: return as-is
    }

    RoutedServices routes = metadataParser.parseRoutes(serviceInstance.getMetadata());
    try {
        return transformService.transformURL(
            ServiceType.UI, serviceId, instanceHomePage, routes, isClientAttlsEnabled);
    } catch (URLTransformationException | IllegalArgumentException e) {
        if (!ApiLayerServices.isApiLayerService(serviceId)) {
            // Demoted from WARN to DEBUG — missing UI route is expected for API-only services
            log.debug("Could not transform homepage URL for service {}: {}", serviceId, e.getMessage());
        }
        // Return null: the UI will hide the link rather than showing an internal URL
        return null;
    }
}
```

**API Catalog UI — hide homepage link when null/empty:**

```jsx
// ServiceTile.jsx (or equivalent)
{service.homePageUrl && (
    <a href={service.homePageUrl} target="_blank" rel="noopener noreferrer">
        Home Page
    </a>
)}
```

### Tests

**New / updated `ContainerServiceTest`:**
- `givenServiceWithNoUiRoute_whenGetHomePageUrl_thenReturnsNull()` — mock `transformService.transformURL()` throwing `URLTransformationException`, assert `null` is returned.
- `givenServiceWithWorkingUiRoute_whenGetHomePageUrl_thenReturnsGatewayUrl()` — confirm the happy path returns the transformed URL.
- `givenApimlService_whenGetHomePageUrl_thenReturnOriginalUrl()` — APIML services bypass transformation and return the original URL.
- `givenTransformFailure_whenGetHomePageUrl_thenDebugLogEmitted()` — assert a `DEBUG` log (not `WARN`) is emitted using a Logback `ListAppender`.

**API Catalog UI test (Jest):**
- `givenHomePageUrlIsNull_whenRenderTile_thenHomepageLinkNotPresent()` — render the `ServiceTile` component with `homePageUrl: null` and assert the homepage anchor element does not appear in the output.
- `givenHomePageUrlIsEmpty_whenRenderTile_thenHomepageLinkNotPresent()`.
- `givenHomePageUrlIsValid_whenRenderTile_thenHomepageLinkPresent()`.
