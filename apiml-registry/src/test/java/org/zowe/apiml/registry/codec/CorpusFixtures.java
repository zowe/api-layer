/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.codec;

import org.zowe.apiml.registry.model.ActionType;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The wire-contract corpus, rebuilt against our own model.
 * <p>
 * Deliberately a mirror of {@code org.zowe.apiml.discovery.contract.WireContractCorpus} rather than a shared
 * class: that one is expressed in Netflix types and this one is not, and the whole point is to prove the two
 * independent representations serialise identically. Values are fixed so the comparison is byte-stable.
 */
final class CorpusFixtures {

    static final long T_REGISTERED = 1_700_000_000_000L;
    static final long T_RENEWED = 1_700_000_030_000L;
    static final long T_UP = 1_700_000_001_000L;
    static final long T_DIRTY = 1_700_000_000_500L;
    static final long T_UPDATED = 1_700_000_002_000L;

    private CorpusFixtures() {
    }

    private static Lease lease(int renewalIntervalSecs, int durationSecs, Lease.Kind kind) {
        return Lease.builder()
            .kind(kind)
            .renewalIntervalSecs(renewalIntervalSecs)
            .durationSecs(durationSecs)
            .registrationTimestamp(T_REGISTERED)
            .lastRenewalTimestamp(T_RENEWED)
            .serviceUpTimestamp(T_UP)
            .evictionTimestamp(0L)
            .build();
    }

    private static Map<String, String> apimlMetadata() {
        Map<String, String> metadata = new TreeMap<>();
        metadata.put("apiml.routes.api-v1.gatewayUrl", "/api/v1");
        metadata.put("apiml.routes.api-v1.serviceUrl", "/discoverableclient/api/v1");
        metadata.put("apiml.apiInfo.0.apiId", "zowe.apiml.discoverableclient");
        metadata.put("apiml.apiInfo.0.version", "1.0.0");
        metadata.put("apiml.apiInfo.0.gatewayUrl", "api/v1");
        metadata.put("apiml.authentication.scheme", "httpBasicPassTicket");
        metadata.put("apiml.authentication.applid", "ZOWEAPPL");
        metadata.put("apiml.service.title", "Service Integration Test Client");
        metadata.put("apiml.corsEnabled", "true");
        metadata.put("apiml.gateway.rateLimiterCapacity", "100");
        return metadata;
    }

    static ServiceInstance apimlService() {
        return ServiceInstance.builder()
            .instanceId("localhost:discoverableclient:10012")
            .appName("DISCOVERABLECLIENT")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10012, false)
            .securePort(10012, true)
            .vipAddress("discoverableclient")
            .secureVipAddress("discoverableclient")
            .status(InstanceStatus.UP)
            .overriddenStatus(InstanceStatus.UNKNOWN)
            .homePageUrl("https://localhost:10012/discoverableclient/")
            .statusPageUrl("https://localhost:10012/discoverableclient/application/info")
            .secureHealthCheckUrl("https://localhost:10012/discoverableclient/application/health")
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .lease(lease(30, 90, Lease.Kind.RENEWABLE))
            .metadata(apimlMetadata())
            .lastUpdatedTimestamp(T_UPDATED)
            .lastDirtyTimestamp(T_DIRTY)
            .actionType(ActionType.ADDED)
            .build();
    }

    static ServiceInstance staticService() {
        Map<String, String> metadata = new TreeMap<>();
        metadata.put("apiml.routes.api-v1.gatewayUrl", "/api/v1");
        metadata.put("apiml.routes.api-v1.serviceUrl", "/staticclient/api/v1");
        metadata.put("apiml.service.title", "Statically Onboarded Service");
        metadata.put("apiml.authentication.scheme", "bypass");

        return ServiceInstance.builder()
            .instanceId("localhost:staticclient:10013")
            .appName("STATICCLIENT")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10013, true)
            // Not set by the static definition, so it keeps Eureka's default secure port, disabled.
            .securePort(PortInfo.disabledDefault(PortInfo.DEFAULT_SECURE_PORT))
            .vipAddress("staticclient")
            .secureVipAddress("staticclient")
            .status(InstanceStatus.UP)
            .overriddenStatus(InstanceStatus.UNKNOWN)
            .homePageUrl("http://localhost:10013/staticclient/")
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .lease(lease(Lease.PERMANENT_DURATION_SECS, Lease.PERMANENT_DURATION_SECS, Lease.Kind.PERMANENT))
            .metadata(metadata)
            .lastUpdatedTimestamp(T_UPDATED)
            .lastDirtyTimestamp(T_DIRTY)
            .actionType(ActionType.ADDED)
            .build();
    }

    static ServiceInstance minimalInstance() {
        return ServiceInstance.builder()
            .instanceId("localhost:minimal:10099")
            .appName("MINIMAL")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10099, true)
            .securePort(PortInfo.disabledDefault(PortInfo.DEFAULT_SECURE_PORT))
            .vipAddress("minimal")
            .status(InstanceStatus.OUT_OF_SERVICE)
            .overriddenStatus(InstanceStatus.OUT_OF_SERVICE)
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .lease(lease(30, 90, Lease.Kind.RENEWABLE))
            .metadata(Map.of())
            .lastUpdatedTimestamp(T_UPDATED)
            .lastDirtyTimestamp(T_DIRTY)
            .build();
    }

    static ServiceInstance overriddenDownInstance() {
        return apimlService().toBuilder()
            .instanceId("localhost:discoverableclient:10014")
            .status(InstanceStatus.DOWN)
            .overriddenStatus(InstanceStatus.OUT_OF_SERVICE)
            .build();
    }

    static ServiceInstance allFieldsInstance() {
        Map<String, String> metadata = new TreeMap<>();
        metadata.put("apiml.service.title", "Everything Set");

        return apimlService().toBuilder()
            .instanceId("localhost:everything:10020")
            .appName("EVERYTHING")
            .appGroupName("ZOWE_GROUP")
            .sid("sid-value")
            .countryId(42)
            .asgName("asg-value")
            .homePageUrl("https://localhost:10020/home")
            .statusPageUrl("https://localhost:10020/info")
            .healthCheckUrl("http://localhost:10020/health")
            .secureHealthCheckUrl("https://localhost:10020/health")
            .vipAddress("everything")
            .secureVipAddress("everything")
            .coordinatingDiscoveryServer(true)
            .status(InstanceStatus.STARTING)
            .overriddenStatus(InstanceStatus.OUT_OF_SERVICE)
            .actionType(ActionType.MODIFIED)
            .metadata(metadata)
            .build();
    }

    static Application singleApplication() {
        return new Application("DISCOVERABLECLIENT", List.of(apimlService()));
    }

    static Applications registry() {
        return new Applications(
            List.of(
                new Application("DISCOVERABLECLIENT", List.of(apimlService(), overriddenDownInstance())),
                new Application("STATICCLIENT", List.of(staticService()))
            ),
            7L,
            "UP_3_"
        );
    }

    static Applications delta() {
        return new Applications(
            List.of(
                new Application("DISCOVERABLECLIENT",
                    List.of(apimlService().toBuilder().actionType(ActionType.ADDED).build())),
                new Application("STATICCLIENT",
                    List.of(staticService().toBuilder().actionType(ActionType.DELETED).build()))
            ),
            8L,
            "UP_3_"
        );
    }

    static Applications emptyRegistry() {
        return new Applications(List.of(), 1L, "");
    }

    static Applications mixedStatusRegistry() {
        ServiceInstance template = apimlService();
        Application app = new Application("MIXED", List.of(
            template.toBuilder().instanceId("localhost:mixed:1").appName("MIXED").status(InstanceStatus.UP).build(),
            template.toBuilder().instanceId("localhost:mixed:2").appName("MIXED").status(InstanceStatus.UP).build(),
            template.toBuilder().instanceId("localhost:mixed:3").appName("MIXED").status(InstanceStatus.DOWN).build(),
            template.toBuilder().instanceId("localhost:mixed:4").appName("MIXED")
                .status(InstanceStatus.STARTING).build(),
            template.toBuilder().instanceId("localhost:mixed:5").appName("MIXED")
                .status(InstanceStatus.OUT_OF_SERVICE).build()
        ));
        return new Applications(List.of(app), 11L, Applications.computeHashCode(List.of(app)));
    }

}
