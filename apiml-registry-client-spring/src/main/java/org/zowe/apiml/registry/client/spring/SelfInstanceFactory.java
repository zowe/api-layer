/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds this service's own registration from its configuration.
 * <p>
 * The replacement for Netflix's {@code InstanceInfoFactory}, and for the copy of it APIML kept in the
 * Gateway's {@code ConnectionsConfig} so that the Gateway could register under its external URL. Both are
 * here, and the external-URL case is just a different host and port passed in.
 */
public final class SelfInstanceFactory {

    private SelfInstanceFactory() {
    }

    public static ServiceInstance create(RegistryInstanceProperties config, long now) {
        return create(config, now, null);
    }

    /**
     * Registers under a base URL other than the one the service binds to.
     * <p>
     * The Gateway needs this: behind a load balancer or an ingress it must advertise the address clients can
     * actually reach ({@code apiml.service.externalUrl}), not its own host and port. Every URL is rebuilt from
     * that base and its configured path, and metadata values carrying the old base are rewritten - a service's
     * {@code swaggerUrl} pointing at an unreachable host is a broken API Catalog entry.
     *
     * @param advertisedBaseUrl scheme, host and port to advertise, e.g. {@code https://apiml.example.com:443};
     *                          null registers the service as configured
     */
    public static ServiceInstance create(RegistryInstanceProperties config, long now, String advertisedBaseUrl) {
        String hostname = config.resolvedHostname();
        int port = config.getNonSecurePort();
        int securePort = config.getSecurePort();

        String homePageUrl = config.resolvedHomePageUrl();
        String statusPageUrl = config.resolvedStatusPageUrl();
        String healthCheckUrl = config.resolvedHealthCheckUrl();
        String secureHealthCheckUrl = config.resolvedSecureHealthCheckUrl();
        Map<String, String> metadata = config.getMetadataMap();

        if (advertisedBaseUrl != null) {
            URI advertised = URI.create(advertisedBaseUrl);
            if (advertised.getScheme() == null || advertised.getHost() == null || advertised.getPort() < 0) {
                // Checked rather than allowed through: "localhost:10010" parses as a URI with no host, and a
                // registration with a null host is accepted by the registry and then unreachable by everyone.
                throw new IllegalArgumentException(
                    "apiml.service.externalUrl must be a full URL with scheme, host and port, but was: "
                        + advertisedBaseUrl);
            }
            String base = trimTrailingSlash(advertisedBaseUrl);
            hostname = advertised.getHost();
            // Both ports become the advertised one. A reverse proxy terminating TLS on 443 and forwarding to
            // 10010 makes the bound ports meaningless to a client.
            port = advertised.getPort();
            securePort = advertised.getPort();

            metadata = rewriteBase(metadata, homePageUrl, base);
            homePageUrl = base + orSlash(config.getHomePageUrlPath());
            statusPageUrl = config.getStatusPageUrlPath() == null ? null : base + config.getStatusPageUrlPath();
            healthCheckUrl = config.getHealthCheckUrlPath() == null || !config.isNonSecurePortEnabled()
                ? null
                : "http://" + hostname + ":" + port + config.getHealthCheckUrlPath();
            secureHealthCheckUrl = config.getHealthCheckUrlPath() == null || !config.isSecurePortEnabled()
                ? null
                : "https://" + hostname + ":" + securePort + config.getHealthCheckUrlPath();
        }

        // Netflix registered a new instance as STARTING unless instanceEnabledOnit was set, so that peers and
        // the routing table learn about it before it can receive traffic. The lifecycle promotes it to UP once
        // the application reports healthy.
        InstanceStatus initialStatus = config.isInstanceEnabledOnit()
            ? InstanceStatus.UP
            : InstanceStatus.STARTING;

        String instanceId = config.getInstanceId() != null
            ? config.getInstanceId()
            : hostname + ":" + config.getAppname() + ":" + config.advertisedPort();

        return ServiceInstance.builder()
            .instanceId(instanceId)
            .appName(config.getAppname())
            .appGroupName(config.getAppGroupName())
            .hostName(hostname)
            .ipAddr(config.getIpAddress())
            .port(new PortInfo(port, config.isNonSecurePortEnabled()))
            .securePort(new PortInfo(securePort, config.isSecurePortEnabled()))
            .homePageUrl(homePageUrl)
            .statusPageUrl(statusPageUrl)
            .healthCheckUrl(healthCheckUrl)
            .secureHealthCheckUrl(secureHealthCheckUrl)
            .vipAddress(config.getVirtualHostName())
            .secureVipAddress(config.getSecureVirtualHostName())
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .status(initialStatus)
            .lease(Lease.renewable(
                config.getLeaseRenewalIntervalInSeconds(),
                config.getLeaseExpirationDurationInSeconds(),
                now))
            .metadata(metadata)
            .lastUpdatedTimestamp(now)
            .lastDirtyTimestamp(now)
            .build();
    }

    /**
     * Replaces the configured base URL wherever it appears in a metadata value.
     * <p>
     * Empty values are dropped rather than carried, which is what Netflix's factory did: a metadata key with an
     * empty value is indistinguishable on the wire from one that was never set, and the API Catalog treats the
     * two differently.
     */
    private static Map<String, String> rewriteBase(Map<String, String> metadata, String configuredHomePageUrl, String advertisedBase) {
        String from = configuredHomePageUrl == null ? null : trimTrailingSlash(configuredHomePageUrl) + "/";
        String to = advertisedBase + "/";

        Map<String, String> rewritten = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (value == null || value.isEmpty()) {
                return;
            }
            rewritten.put(key, from == null ? value : value.replace(from, to));
        });
        return rewritten;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String orSlash(String path) {
        return path == null ? "/" : path;
    }

}
