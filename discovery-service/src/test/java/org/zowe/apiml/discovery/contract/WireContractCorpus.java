/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.contract;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The canonical corpus of registry objects used to pin the Eureka wire format.
 * <p>
 * Every value here is fixed - no timestamps from the clock, no hostnames from the environment - so that the
 * fixtures this corpus produces are byte-stable across machines and runs. That is what lets the fixtures act as
 * golden files rather than as a sample.
 * <p>
 * See TASK-Discovery-Native-Registry.md Phase 0.
 */
public final class WireContractCorpus {

    /** Fixed instants, so fixtures do not churn. */
    public static final long T_REGISTERED = 1_700_000_000_000L;
    public static final long T_RENEWED = 1_700_000_030_000L;
    public static final long T_UP = 1_700_000_001_000L;
    public static final long T_DIRTY = 1_700_000_000_500L;
    public static final long T_UPDATED = 1_700_000_002_000L;

    private WireContractCorpus() {
    }

    private static LeaseInfo lease(int renewalIntervalSecs, int durationSecs) {
        return LeaseInfo.Builder.newBuilder()
            .setRenewalIntervalInSecs(renewalIntervalSecs)
            .setDurationInSecs(durationSecs)
            .setRegistrationTimestamp(T_REGISTERED)
            .setRenewalTimestamp(T_RENEWED)
            .setServiceUpTimestamp(T_UP)
            .setEvictionTimestamp(0L)
            .build();
    }

    /**
     * A realistic APIML service instance: the metadata keys here are the ones the Gateway actually reads to build
     * routes, so this case protects the metadata contract as well as the envelope.
     */
    public static InstanceInfo apimlService() {
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

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId("localhost:discoverableclient:10012")
            .setAppName("DISCOVERABLECLIENT")
            .setAppGroupName(null)
            .setHostName("localhost")
            .setIPAddr("127.0.0.1")
            .setPort(10012)
            .setSecurePort(10012)
            .enablePort(InstanceInfo.PortType.UNSECURE, false)
            .enablePort(InstanceInfo.PortType.SECURE, true)
            .setVIPAddress("discoverableclient")
            .setSecureVIPAddress("discoverableclient")
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setOverriddenStatus(InstanceInfo.InstanceStatus.UNKNOWN)
            .setHomePageUrl(null, "https://localhost:10012/discoverableclient/")
            .setStatusPageUrl(null, "https://localhost:10012/discoverableclient/application/info")
            .setHealthCheckUrls(null, null, "https://localhost:10012/discoverableclient/application/health")
            .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
            .setLeaseInfo(lease(30, 90))
            .setMetadata(metadata)
            .setLastUpdatedTimestamp(T_UPDATED)
            .setLastDirtyTimestamp(T_DIRTY)
            .setActionType(InstanceInfo.ActionType.ADDED)
            .build();
    }

    /**
     * A statically-defined service: registered from YAML, never sends a heartbeat, and carries the immortal lease.
     * The huge durationInSecs is the Eureka int-overflow workaround described in the brief section 4.3.
     */
    public static InstanceInfo staticService() {
        Map<String, String> metadata = new TreeMap<>();
        metadata.put("apiml.routes.api-v1.gatewayUrl", "/api/v1");
        metadata.put("apiml.routes.api-v1.serviceUrl", "/staticclient/api/v1");
        metadata.put("apiml.service.title", "Statically Onboarded Service");
        metadata.put("apiml.authentication.scheme", "bypass");

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId("localhost:staticclient:10013")
            .setAppName("STATICCLIENT")
            .setHostName("localhost")
            .setIPAddr("127.0.0.1")
            .setPort(10013)
            .enablePort(InstanceInfo.PortType.UNSECURE, true)
            .enablePort(InstanceInfo.PortType.SECURE, false)
            .setVIPAddress("staticclient")
            .setSecureVIPAddress("staticclient")
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setOverriddenStatus(InstanceInfo.InstanceStatus.UNKNOWN)
            .setHomePageUrl(null, "http://localhost:10013/staticclient/")
            .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
            .setLeaseInfo(lease(Integer.MAX_VALUE / 1000, Integer.MAX_VALUE / 1000))
            .setMetadata(metadata)
            .setLastUpdatedTimestamp(T_UPDATED)
            .setLastDirtyTimestamp(T_DIRTY)
            .setActionType(InstanceInfo.ActionType.ADDED)
            .build();
    }

    /**
     * Minimal instance: no metadata, no optional URLs, non-UP status. This is the case that exposes how Eureka
     * encodes absent values - notably the empty-metadata representation, which is easy to get wrong.
     */
    public static InstanceInfo minimalInstance() {
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId("localhost:minimal:10099")
            .setAppName("MINIMAL")
            .setHostName("localhost")
            .setIPAddr("127.0.0.1")
            .setPort(10099)
            .enablePort(InstanceInfo.PortType.UNSECURE, true)
            .enablePort(InstanceInfo.PortType.SECURE, false)
            .setVIPAddress("minimal")
            .setStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE)
            .setOverriddenStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE)
            .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
            .setLeaseInfo(lease(30, 90))
            .setMetadata(new LinkedHashMap<>())
            .setLastUpdatedTimestamp(T_UPDATED)
            .setLastDirtyTimestamp(T_DIRTY)
            .build();
    }

    /** An instance whose status has been overridden - the DOWN-via-override path the Gateway must respect. */
    public static InstanceInfo overriddenDownInstance() {
        InstanceInfo base = apimlService();
        return new InstanceInfo.Builder(base)
            .setInstanceId("localhost:discoverableclient:10014")
            .setStatus(InstanceInfo.InstanceStatus.DOWN)
            .setOverriddenStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE)
            .build();
    }

    /** The full registry response: two applications, one with two instances, carrying delta bookkeeping. */
    public static Applications registry() {
        Application discoverable = new Application("DISCOVERABLECLIENT");
        discoverable.addInstance(apimlService());
        discoverable.addInstance(overriddenDownInstance());

        Application staticClient = new Application("STATICCLIENT");
        staticClient.addInstance(staticService());

        Applications applications = new Applications();
        applications.addApplication(discoverable);
        applications.addApplication(staticClient);
        applications.setAppsHashCode("UP_3_");
        applications.setVersion(7L);
        return applications;
    }

    /** A delta response: same envelope, but instances carry ADDED / DELETED action types. */
    public static Applications delta() {
        Application discoverable = new Application("DISCOVERABLECLIENT");
        discoverable.addInstance(new InstanceInfo.Builder(apimlService())
            .setActionType(InstanceInfo.ActionType.ADDED)
            .build());

        Application staticClient = new Application("STATICCLIENT");
        staticClient.addInstance(new InstanceInfo.Builder(staticService())
            .setActionType(InstanceInfo.ActionType.DELETED)
            .build());

        Applications applications = new Applications();
        applications.addApplication(discoverable);
        applications.addApplication(staticClient);
        applications.setAppsHashCode("UP_3_");
        applications.setVersion(8L);
        return applications;
    }

    /** A single application response, as served by GET /eureka/apps/{appId}. */
    public static Application singleApplication() {
        Application application = new Application("DISCOVERABLECLIENT");
        application.addInstance(apimlService());
        return application;
    }

    /** An empty registry - the state on a cold start, before anything registers. */
    public static Applications emptyRegistry() {
        Applications applications = new Applications();
        applications.setAppsHashCode("");
        applications.setVersion(1L);
        return applications;
    }

    /**
     * Every optional field populated.
     * <p>
     * The other cases leave appGroupName, the non-secure healthCheckUrl and asgName unset, so they reveal nothing
     * about where those fields sit in the emitted order. Field order is part of a byte-compared contract, so it
     * has to be observed rather than assumed.
     */
    public static InstanceInfo allFieldsInstance() {
        Map<String, String> metadata = new TreeMap<>();
        metadata.put("apiml.service.title", "Everything Set");

        return new InstanceInfo.Builder(apimlService())
            .setInstanceId("localhost:everything:10020")
            .setAppName("EVERYTHING")
            .setAppGroupName("ZOWE_GROUP")
            .setSID("sid-value")
            .setCountryId(42)
            .setASGName("asg-value")
            .setHomePageUrl(null, "https://localhost:10020/home")
            .setStatusPageUrl(null, "https://localhost:10020/info")
            .setHealthCheckUrls(null, "http://localhost:10020/health", "https://localhost:10020/health")
            .setVIPAddress("everything")
            .setSecureVIPAddress("everything")
            .setIsCoordinatingDiscoveryServer(true)
            .setStatus(InstanceInfo.InstanceStatus.STARTING)
            .setOverriddenStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE)
            .setActionType(InstanceInfo.ActionType.MODIFIED)
            .setMetadata(metadata)
            .build();
    }

    /**
     * A registry deliberately holding several different statuses.
     * <p>
     * This exists to pin the {@code apps__hashcode} ordering. Eureka builds that string from a TreeMap keyed by
     * the status <em>name</em>, so the order is alphabetical (DOWN, OUT_OF_SERVICE, ... UP) rather than the enum
     * declaration order - a difference that is invisible in an all-UP registry and therefore easy to get wrong.
     */
    public static Applications mixedStatusRegistry() {
        Application app = new Application("MIXED");
        app.addInstance(new InstanceInfo.Builder(apimlService())
            .setInstanceId("localhost:mixed:1").setAppName("MIXED")
            .setStatus(InstanceInfo.InstanceStatus.UP).build());
        app.addInstance(new InstanceInfo.Builder(apimlService())
            .setInstanceId("localhost:mixed:2").setAppName("MIXED")
            .setStatus(InstanceInfo.InstanceStatus.UP).build());
        app.addInstance(new InstanceInfo.Builder(apimlService())
            .setInstanceId("localhost:mixed:3").setAppName("MIXED")
            .setStatus(InstanceInfo.InstanceStatus.DOWN).build());
        app.addInstance(new InstanceInfo.Builder(apimlService())
            .setInstanceId("localhost:mixed:4").setAppName("MIXED")
            .setStatus(InstanceInfo.InstanceStatus.STARTING).build());
        app.addInstance(new InstanceInfo.Builder(apimlService())
            .setInstanceId("localhost:mixed:5").setAppName("MIXED")
            .setStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE).build());

        Applications applications = new Applications();
        applications.addApplication(app);
        // Let Eureka compute it, so the fixture records Eureka's answer rather than our expectation of it.
        applications.setAppsHashCode(applications.getReconcileHashCode());
        applications.setVersion(11L);
        return applications;
    }

    /** Named cases, in a stable order, so fixture files are deterministic. */
    public static Map<String, Object> allCases() {
        Map<String, Object> cases = new LinkedHashMap<>();
        cases.put("instance-apiml-service", apimlService());
        cases.put("instance-static-service", staticService());
        cases.put("instance-minimal", minimalInstance());
        cases.put("instance-overridden-down", overriddenDownInstance());
        cases.put("instance-all-fields", allFieldsInstance());
        cases.put("application-single", singleApplication());
        cases.put("applications-registry", registry());
        cases.put("applications-delta", delta());
        cases.put("applications-empty", emptyRegistry());
        cases.put("applications-mixed-status", mixedStatusRegistry());
        return cases;
    }

}
