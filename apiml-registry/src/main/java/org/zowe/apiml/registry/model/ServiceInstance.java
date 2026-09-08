/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One registered instance of one service - the replacement for Netflix's {@code InstanceInfo}.
 * <p>
 * Immutable; mutate via {@link #toBuilder()}. The field set is the wire contract and is pinned by the fixtures in
 * apiml-registry/src/test/resources/wire-contract, so fields must not be added, removed or renamed without a
 * deliberate contract change. Serialisation lives in {@code org.zowe.apiml.registry.codec} - this class carries no
 * Jackson annotations, so that the domain does not depend on how it happens to be encoded.
 */
public final class ServiceInstance {

    /** Eureka emitted this literal for instances that did not set a SID. Kept for byte-compatibility. */
    public static final String DEFAULT_SID = "na";

    /** Eureka's default; APIML never sets a country, but the field is always present on the wire. */
    public static final int DEFAULT_COUNTRY_ID = 1;

    private final String instanceId;
    private final String appName;
    private final String appGroupName;
    private final String hostName;
    private final String ipAddr;
    private final String sid;
    private final PortInfo port;
    private final PortInfo securePort;
    private final String homePageUrl;
    private final String statusPageUrl;
    private final String healthCheckUrl;
    private final String secureHealthCheckUrl;
    private final String vipAddress;
    private final String secureVipAddress;
    private final int countryId;
    private final DataCenterInfo dataCenterInfo;
    private final InstanceStatus status;
    private final InstanceStatus overriddenStatus;
    private final Lease lease;
    private final boolean coordinatingDiscoveryServer;
    private final Map<String, String> metadata;
    private final long lastUpdatedTimestamp;
    private final long lastDirtyTimestamp;
    private final ActionType actionType;
    private final String asgName;

    private ServiceInstance(Builder builder) {
        this.instanceId = builder.instanceId;
        this.appName = builder.appName;
        this.appGroupName = builder.appGroupName;
        this.hostName = builder.hostName;
        this.ipAddr = builder.ipAddr;
        this.sid = builder.sid;
        this.port = builder.port;
        this.securePort = builder.securePort;
        this.homePageUrl = builder.homePageUrl;
        this.statusPageUrl = builder.statusPageUrl;
        this.healthCheckUrl = builder.healthCheckUrl;
        this.secureHealthCheckUrl = builder.secureHealthCheckUrl;
        this.vipAddress = builder.vipAddress;
        this.secureVipAddress = builder.secureVipAddress;
        this.countryId = builder.countryId;
        this.dataCenterInfo = builder.dataCenterInfo;
        this.status = builder.status;
        this.overriddenStatus = builder.overriddenStatus;
        this.lease = builder.lease;
        this.coordinatingDiscoveryServer = builder.coordinatingDiscoveryServer;
        this.metadata = builder.metadata.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new TreeMap<>(builder.metadata));
        this.lastUpdatedTimestamp = builder.lastUpdatedTimestamp;
        this.lastDirtyTimestamp = builder.lastDirtyTimestamp;
        this.actionType = builder.actionType;
        this.asgName = builder.asgName;
    }

    public String instanceId() {
        return instanceId;
    }

    public String appName() {
        return appName;
    }

    public String appGroupName() {
        return appGroupName;
    }

    public String hostName() {
        return hostName;
    }

    public String ipAddr() {
        return ipAddr;
    }

    public String sid() {
        return sid;
    }

    public PortInfo port() {
        return port;
    }

    public PortInfo securePort() {
        return securePort;
    }

    public String homePageUrl() {
        return homePageUrl;
    }

    public String statusPageUrl() {
        return statusPageUrl;
    }

    public String healthCheckUrl() {
        return healthCheckUrl;
    }

    public String secureHealthCheckUrl() {
        return secureHealthCheckUrl;
    }

    public String vipAddress() {
        return vipAddress;
    }

    public String secureVipAddress() {
        return secureVipAddress;
    }

    public int countryId() {
        return countryId;
    }

    public DataCenterInfo dataCenterInfo() {
        return dataCenterInfo;
    }

    public InstanceStatus status() {
        return status;
    }

    public InstanceStatus overriddenStatus() {
        return overriddenStatus;
    }

    public Lease lease() {
        return lease;
    }

    public boolean coordinatingDiscoveryServer() {
        return coordinatingDiscoveryServer;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public long lastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public long lastDirtyTimestamp() {
        return lastDirtyTimestamp;
    }

    public ActionType actionType() {
        return actionType;
    }

    public String asgName() {
        return asgName;
    }

    /**
     * The service id this instance belongs to, lower-cased.
     * <p>
     * {@link #appName()} is upper-case on the wire because the Java enabler upper-cases it on registration, so
     * callers that want to compare against a configured service id should use this.
     */
    public String serviceId() {
        return appName == null ? null : appName.toLowerCase();
    }

    /** Whether the instance is routable: it must be UP, unless an override says otherwise. */
    public InstanceStatus effectiveStatus() {
        if (overriddenStatus != null && overriddenStatus != InstanceStatus.UNKNOWN) {
            return overriddenStatus;
        }
        return status;
    }

    public Builder toBuilder() {
        return new Builder()
            .instanceId(instanceId)
            .appName(appName)
            .appGroupName(appGroupName)
            .hostName(hostName)
            .ipAddr(ipAddr)
            .sid(sid)
            .port(port)
            .securePort(securePort)
            .homePageUrl(homePageUrl)
            .statusPageUrl(statusPageUrl)
            .healthCheckUrl(healthCheckUrl)
            .secureHealthCheckUrl(secureHealthCheckUrl)
            .vipAddress(vipAddress)
            .secureVipAddress(secureVipAddress)
            .countryId(countryId)
            .dataCenterInfo(dataCenterInfo)
            .status(status)
            .overriddenStatus(overriddenStatus)
            .lease(lease)
            .coordinatingDiscoveryServer(coordinatingDiscoveryServer)
            .metadata(metadata)
            .lastUpdatedTimestamp(lastUpdatedTimestamp)
            .lastDirtyTimestamp(lastDirtyTimestamp)
            .actionType(actionType)
            .asgName(asgName);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServiceInstance that)) {
            return false;
        }
        return Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(instanceId);
    }

    @Override
    public String toString() {
        return "ServiceInstance[" + instanceId + " " + status + "]";
    }

    public static final class Builder {

        private String instanceId;
        private String appName;
        private String appGroupName;
        private String hostName;
        private String ipAddr;
        private String sid = DEFAULT_SID;
        private PortInfo port = PortInfo.disabledDefault(PortInfo.DEFAULT_PORT);
        private PortInfo securePort = PortInfo.disabledDefault(PortInfo.DEFAULT_SECURE_PORT);
        private String homePageUrl;
        private String statusPageUrl;
        private String healthCheckUrl;
        private String secureHealthCheckUrl;
        private String vipAddress;
        private String secureVipAddress;
        private int countryId = DEFAULT_COUNTRY_ID;
        private DataCenterInfo dataCenterInfo = DataCenterInfo.MY_OWN;
        private InstanceStatus status = InstanceStatus.UP;
        private InstanceStatus overriddenStatus = InstanceStatus.UNKNOWN;
        private Lease lease;
        private boolean coordinatingDiscoveryServer;
        private Map<String, String> metadata = new LinkedHashMap<>();
        private long lastUpdatedTimestamp;
        private long lastDirtyTimestamp;
        private ActionType actionType;
        private String asgName;

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder appGroupName(String appGroupName) {
            this.appGroupName = appGroupName;
            return this;
        }

        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public Builder ipAddr(String ipAddr) {
            this.ipAddr = ipAddr;
            return this;
        }

        public Builder sid(String sid) {
            this.sid = sid;
            return this;
        }

        public Builder port(PortInfo port) {
            this.port = port;
            return this;
        }

        public Builder port(int port, boolean enabled) {
            return port(new PortInfo(port, enabled));
        }

        public Builder securePort(PortInfo securePort) {
            this.securePort = securePort;
            return this;
        }

        public Builder securePort(int port, boolean enabled) {
            return securePort(new PortInfo(port, enabled));
        }

        public Builder homePageUrl(String homePageUrl) {
            this.homePageUrl = homePageUrl;
            return this;
        }

        public Builder statusPageUrl(String statusPageUrl) {
            this.statusPageUrl = statusPageUrl;
            return this;
        }

        public Builder healthCheckUrl(String healthCheckUrl) {
            this.healthCheckUrl = healthCheckUrl;
            return this;
        }

        public Builder secureHealthCheckUrl(String secureHealthCheckUrl) {
            this.secureHealthCheckUrl = secureHealthCheckUrl;
            return this;
        }

        public Builder vipAddress(String vipAddress) {
            this.vipAddress = vipAddress;
            return this;
        }

        public Builder secureVipAddress(String secureVipAddress) {
            this.secureVipAddress = secureVipAddress;
            return this;
        }

        public Builder countryId(int countryId) {
            this.countryId = countryId;
            return this;
        }

        public Builder dataCenterInfo(DataCenterInfo dataCenterInfo) {
            this.dataCenterInfo = dataCenterInfo;
            return this;
        }

        public Builder status(InstanceStatus status) {
            this.status = status;
            return this;
        }

        public Builder overriddenStatus(InstanceStatus overriddenStatus) {
            this.overriddenStatus = overriddenStatus;
            return this;
        }

        public Builder lease(Lease lease) {
            this.lease = lease;
            return this;
        }

        public Builder coordinatingDiscoveryServer(boolean coordinatingDiscoveryServer) {
            this.coordinatingDiscoveryServer = coordinatingDiscoveryServer;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
            return this;
        }

        public Builder putMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder lastUpdatedTimestamp(long lastUpdatedTimestamp) {
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
            return this;
        }

        public Builder lastDirtyTimestamp(long lastDirtyTimestamp) {
            this.lastDirtyTimestamp = lastDirtyTimestamp;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder asgName(String asgName) {
            this.asgName = asgName;
            return this;
        }

        public ServiceInstance build() {
            return new ServiceInstance(this);
        }

    }

}
