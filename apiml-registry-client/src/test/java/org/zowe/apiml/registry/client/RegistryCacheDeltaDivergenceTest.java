/*
 * Proves that a diverged delta is published to the cache before it is validated.
 *
 * RegistryCache.applyDelta folds the delta into the current view and stores the result, and only afterwards
 * compares the recomputed hash with the one the registry declared. When they disagree it returns false, which
 * tells RegistryClient to fall back to a full fetch - but the inconsistent view is already visible to every
 * caller of upInstances()/serviceIds() until that fetch completes.
 *
 * A Gateway resolving a service during that window finds no instance and answers 404 instead of routing, which
 * is the observed CI symptom (OpenTelemetryResourceAttributesZosTest expecting 401 and getting 404, and the
 * API Catalog vanishing from /eureka/apps).
 */
package org.zowe.apiml.registry.client;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.model.ActionType;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryCacheDeltaDivergenceTest {

    private static ServiceInstance instance(String app, String id, InstanceStatus status, ActionType action) {
        return ServiceInstance.builder()
            .instanceId(id)
            .appName(app)
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(new PortInfo(10014, false))
            .homePageUrl("https://localhost:10014/" + app.toLowerCase())
            .status(status)
            .actionType(action)
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .build();
    }

    private static Applications snapshot(List<Application> apps) {
        return new Applications(apps, 1L, Applications.computeHashCode(apps));
    }

    @Test
    void aDivergedDeltaMustNotBecomeTheServedView() {
        RegistryCache cache = new RegistryCache();

        // The Gateway's starting view: the catalog is present and UP.
        List<Application> initial = List.of(new Application("APICATALOG",
            List.of(instance("APICATALOG", "apicatalog:1", InstanceStatus.UP, ActionType.ADDED))));
        cache.replace(snapshot(initial));
        assertEquals(1, cache.upInstances("APICATALOG").size(), "precondition: catalog routable");

        // A delta that the registry declares consistent, but whose contents do not actually match that hash.
        // This is the stale-base case the class javadoc says the hash check exists to catch.
        Application catalogDown = new Application("APICATALOG",
            List.of(instance("APICATALOG", "apicatalog:1", InstanceStatus.DOWN, ActionType.MODIFIED)));
        Applications lyingDelta = new Applications(List.of(catalogDown), 2L, "UP_1_");

        boolean consistent = cache.applyDelta(lyingDelta);
        assertFalse(consistent, "the hash mismatch must be reported to the caller");

        // The contract RegistryClient depends on: `false` means "do a full fetch rather than serve a divergent
        // view". If the cache has already stored it, the Gateway routes on it in the meantime.
        assertEquals(1, cache.upInstances("APICATALOG").size(),
            "a delta reported as inconsistent must not change the served view; "
                + "the caller only repairs it on its next refresh");
        assertTrue(cache.serviceIds().contains("apicatalog"),
            "the application must still be resolvable while the caller recovers");
    }

    @Test
    void aConsistentDeltaIsApplied() {
        RegistryCache cache = new RegistryCache();
        List<Application> initial = List.of(new Application("APICATALOG",
            List.of(instance("APICATALOG", "apicatalog:1", InstanceStatus.UP, ActionType.ADDED))));
        cache.replace(snapshot(initial));

        Application added = new Application("APICATALOG",
            List.of(instance("APICATALOG", "apicatalog:2", InstanceStatus.UP, ActionType.ADDED)));
        List<Application> merged = new ArrayList<>();
        merged.add(new Application("APICATALOG", List.of(
            instance("APICATALOG", "apicatalog:1", InstanceStatus.UP, ActionType.ADDED),
            instance("APICATALOG", "apicatalog:2", InstanceStatus.UP, ActionType.ADDED))));

        Applications delta = new Applications(List.of(added), 2L, Applications.computeHashCode(merged));
        assertTrue(cache.applyDelta(delta), "a delta that reconciles must be accepted");
        assertEquals(2, cache.upInstances("APICATALOG").size(), "and both instances are routable");
    }

}
