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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maps a {@link WireNode} tree onto the model.
 * <p>
 * Deliberately lenient. This code reads payloads produced by five enabler implementations across three languages,
 * several of them years old, plus hand-rolled XML written by customers following Zowe's documentation. Anything
 * unrecognised is skipped and anything absent falls back to the documented default rather than failing the whole
 * registration - a strict reader here would reject services that work today.
 */
final class WireMapper {

    private WireMapper() {
    }

    static ServiceInstance toInstance(WireNode node) {
        ServiceInstance.Builder builder = ServiceInstance.builder()
            .instanceId(node.string("instanceId"))
            .appName(node.string("app", "appName"))
            .appGroupName(node.string("appGroupName"))
            .hostName(node.string("hostName"))
            .ipAddr(node.string("ipAddr"))
            .homePageUrl(node.string("homePageUrl"))
            .statusPageUrl(node.string("statusPageUrl"))
            .healthCheckUrl(node.string("healthCheckUrl"))
            .secureHealthCheckUrl(node.string("secureHealthCheckUrl"))
            .vipAddress(node.string("vipAddress"))
            .secureVipAddress(node.string("secureVipAddress"))
            .status(InstanceStatus.fromWire(node.string("status")))
            .actionType(ActionType.fromWire(node.string("actionType")))
            .asgName(node.string("asgName"))
            .coordinatingDiscoveryServer(node.bool(false, "isCoordinatingDiscoveryServer"))
            .lastUpdatedTimestamp(node.number(0L, "lastUpdatedTimestamp"))
            .lastDirtyTimestamp(node.number(0L, "lastDirtyTimestamp"));

        String sid = node.string("sid");
        if (sid != null) {
            builder.sid(sid);
        }

        WireNode countryId = node.child("countryId");
        if (countryId != null && countryId.text() != null) {
            builder.countryId((int) node.number(ServiceInstance.DEFAULT_COUNTRY_ID, "countryId"));
        }

        // An absent overriddenStatus is UNKNOWN, which is how "no override" is spelled on the wire.
        String overridden = node.string("overriddenStatus", "overriddenstatus");
        builder.overriddenStatus(overridden == null ? InstanceStatus.UNKNOWN : InstanceStatus.fromWire(overridden));

        builder.port(readPort(node.child("port"), PortInfo.DEFAULT_PORT));
        builder.securePort(readPort(node.child("securePort"), PortInfo.DEFAULT_SECURE_PORT));

        WireNode dataCenterInfo = node.child("dataCenterInfo");
        builder.dataCenterInfo(dataCenterInfo == null
            ? DataCenterInfo.MY_OWN
            : DataCenterInfo.fromWire(dataCenterInfo.string("name")));

        builder.lease(readLease(node.child("leaseInfo")));
        builder.metadata(readMetadata(node.child("metadata")));

        return builder.build();
    }

    /**
     * Reads a port in any of the shapes it arrives in: a JSON object with {@code $}/{@code @enabled}, XML element
     * text with an {@code enabled} attribute, or - from some older clients - a bare number with no flag at all.
     */
    private static PortInfo readPort(WireNode node, int defaultPort) {
        if (node == null) {
            return PortInfo.disabledDefault(defaultPort);
        }
        String value = node.valueOrChild(WireConstants.PORT_VALUE_KEY);
        int port = defaultPort;
        if (value != null && !value.isBlank()) {
            try {
                port = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                port = defaultPort;
            }
        }
        // A bare number carries no flag; treat a declared port as enabled, which is what the sender meant.
        boolean hasFlag = node.child(WireConstants.PORT_ENABLED_KEY, WireConstants.PORT_ENABLED_ATTRIBUTE) != null;
        boolean enabled = hasFlag
            ? node.bool(false, WireConstants.PORT_ENABLED_KEY, WireConstants.PORT_ENABLED_ATTRIBUTE)
            : value != null;
        return new PortInfo(port, enabled);
    }

    private static Lease readLease(WireNode node) {
        if (node == null) {
            return null;
        }
        int duration = (int) node.number(90L, "durationInSecs");
        return Lease.builder()
            // A duration at Eureka's overflow-workaround value is how a permanent lease appears on the wire.
            .kind(duration >= Lease.PERMANENT_DURATION_SECS ? Lease.Kind.PERMANENT : Lease.Kind.RENEWABLE)
            .renewalIntervalSecs((int) node.number(30L, "renewalIntervalInSecs"))
            .durationSecs(duration)
            .registrationTimestamp(node.number(0L, "registrationTimestamp"))
            .lastRenewalTimestamp(node.number(0L, "lastRenewalTimestamp"))
            .evictionTimestamp(node.number(0L, "evictionTimestamp"))
            .serviceUpTimestamp(node.number(0L, "serviceUpTimestamp"))
            .build();
    }

    private static Map<String, String> readMetadata(WireNode node) {
        Map<String, String> metadata = new TreeMap<>();
        if (node == null) {
            return metadata;
        }
        for (Map.Entry<String, List<WireNode>> entry : node.children().entrySet()) {
            String key = entry.getKey();
            // The empty-map marker is an encoding artefact, not a metadata entry.
            if (WireConstants.TYPE_DISCRIMINATOR_JSON.equals(key) || WireConstants.TYPE_DISCRIMINATOR_XML.equals(key)) {
                continue;
            }
            for (WireNode value : entry.getValue()) {
                if (value.text() != null) {
                    metadata.put(key, value.text());
                }
            }
        }
        return metadata;
    }

    static Application toApplication(WireNode node) {
        List<ServiceInstance> instances = new ArrayList<>();
        for (WireNode instance : node.all(WireConstants.ROOT_INSTANCE)) {
            instances.add(toInstance(instance));
        }
        return new Application(node.string("name"), instances);
    }

    static Applications toApplications(WireNode node) {
        List<Application> applications = new ArrayList<>();
        for (WireNode application : node.all(WireConstants.ROOT_APPLICATION)) {
            applications.add(toApplication(application));
        }
        String version = node.string(WireConstants.FIELD_VERSIONS_DELTA);
        Long parsedVersion = null;
        if (version != null && !version.isBlank()) {
            try {
                parsedVersion = Long.parseLong(version.trim());
            } catch (NumberFormatException e) {
                parsedVersion = null;
            }
        }
        String hashCode = node.string(WireConstants.FIELD_APPS_HASHCODE);
        return new Applications(applications, parsedVersion, hashCode == null ? "" : hashCode);
    }

}
