/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.codec.WireFormat;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.replication.ReplicationAction;
import org.zowe.apiml.registry.replication.ReplicationBatch;
import org.zowe.apiml.registry.replication.ReplicationItem;
import org.zowe.apiml.registry.replication.ReplicationResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code /eureka} surface, expressed without any web framework.
 * <p>
 * The standalone Discovery Service is a servlet application and the modulith is reactive, so the surface has to be
 * served twice. Having the logic here and only thin adapters on top is a direct response to how the Eureka
 * version went wrong: {@code EurekaRestController} existed solely to marshal a {@code ServerWebExchange} into a
 * JAX-RS {@code UriInfo} so Netflix's servlet-shaped resources could be called reactively, and every status code
 * and header decision was duplicated there. Two adapters over one implementation cannot drift.
 * <p>
 * Every status code and representation rule below is pinned by
 * {@code apiml-registry/src/test/resources/wire-contract/http-contract.json}, captured from the running
 * Eureka-based service.
 */
@RequiredArgsConstructor
public class RegistryEndpoints {

    /**
     * An HTTP response, reduced to what the registry actually decides.
     *
     * @param status      the status code
     * @param contentType the content type, or null - which is meaningful: a 404 carries no Content-Type at all
     * @param body        the body, or null for an empty response
     */
    public record Result(int status, String contentType, String body) {

        static Result ok(String body, WireFormat format) {
            return new Result(200, format.syntax() == WireFormat.Syntax.JSON
                ? "application/json"
                : "application/xml", body);
        }

        static Result json(String body) {
            return new Result(200, "application/json", body);
        }

        /** 404 with no body and no Content-Type. Enablers detect "must re-register" from exactly this. */
        static Result notFound() {
            return new Result(404, null, null);
        }

        static Result noContent() {
            return new Result(204, null, null);
        }

        static Result empty(int status) {
            return new Result(status, null, null);
        }
    }

    private final ServiceRegistry registry;
    private final RegistryCodec codec;

    // ---------------------------------------------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------------------------------------------

    public Result applications(String accept, String eurekaAccept) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return Result.ok(codec.encode(registry.applications(), format), format);
    }

    public Result delta(String accept, String eurekaAccept) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return Result.ok(codec.encode(registry.delta(), format), format);
    }

    public Result application(String appId, String accept, String eurekaAccept) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return registry.application(appId)
            .map(application -> Result.ok(codec.encode(application, format), format))
            .orElseGet(Result::notFound);
    }

    public Result instance(String appId, String instanceId, String accept, String eurekaAccept) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return registry.instance(appId, instanceId)
            .map(found -> Result.ok(codec.encode(found, format), format))
            .orElseGet(Result::notFound);
    }

    public Result instanceById(String instanceId, String accept, String eurekaAccept) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return registry.applications().applications().stream()
            .flatMap(application -> application.instances().stream())
            .filter(candidate -> instanceId.equals(candidate.instanceId()))
            .findFirst()
            .map(found -> Result.ok(codec.encode(found, format), format))
            .orElseGet(Result::notFound);
    }

    public Result vip(String vipAddress, String accept, String eurekaAccept) {
        return vipResult(registry.byVipAddress(vipAddress), accept, eurekaAccept);
    }

    public Result secureVip(String svipAddress, String accept, String eurekaAccept) {
        return vipResult(registry.bySecureVipAddress(svipAddress), accept, eurekaAccept);
    }

    /** A VIP lookup answers with the registry envelope, filtered to the matching instances. */
    private Result vipResult(List<ServiceInstance> instances, String accept, String eurekaAccept) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        Map<String, List<ServiceInstance>> grouped = new LinkedHashMap<>();
        for (ServiceInstance instance : instances) {
            grouped.computeIfAbsent(instance.appName(), name -> new ArrayList<>()).add(instance);
        }
        List<Application> applications = new ArrayList<>();
        grouped.forEach((name, list) -> applications.add(new Application(name, list)));

        Applications snapshot = registry.applications();
        Applications filtered = new Applications(applications, snapshot.version(), snapshot.appsHashCode());
        return Result.ok(codec.encode(filtered, format), format);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------------------------------------------

    public Result register(String payload, boolean replication) {
        ServiceInstance instance = codec.decodeInstance(payload);
        registry.register(instance, replication ? RegistrationKind.REPLICATED : RegistrationKind.DYNAMIC);
        // 204, matching Eureka: register returns no content and a client treats a body here as unexpected.
        return Result.noContent();
    }

    public Result renew(String appId, String instanceId, boolean replication) {
        return registry.renew(appId, instanceId, replication) ? Result.empty(200) : Result.notFound();
    }

    public Result cancel(String appId, String instanceId, boolean replication) {
        return registry.cancel(appId, instanceId, replication) ? Result.empty(200) : Result.notFound();
    }

    public Result overrideStatus(String appId, String instanceId, String value, boolean replication) {
        return registry.overrideStatus(appId, instanceId, InstanceStatus.fromWire(value), replication)
            ? Result.empty(200)
            : Result.notFound();
    }

    public Result clearStatusOverride(String appId, String instanceId, String value, boolean replication) {
        InstanceStatus revertTo = value == null ? InstanceStatus.UP : InstanceStatus.fromWire(value);
        return registry.clearStatusOverride(appId, instanceId, revertTo, replication)
            ? Result.empty(200)
            : Result.notFound();
    }

    public Result updateMetadata(String appId, String instanceId, Map<String, String> metadata) {
        return registry.updateMetadata(appId, instanceId, metadata) ? Result.empty(200) : Result.notFound();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Status
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Registry status as JSON, replacing Eureka's Freemarker dashboard (decision D4).
     * <p>
     * {@code /eureka/status} and {@code /eureka/lastn} are the only two paths that are deliberately <em>not</em>
     * part of the frozen contract, so changing the representation from HTML to JSON is sanctioned. The data is
     * what the old page showed and what an operator triaging a registration problem actually needs: which
     * instances are known, their effective status, how long since each last renewed, and whether eviction is
     * currently suspended by self-preservation. The API Catalog UI renders it.
     * <p>
     * Worth knowing: on a locally-run Discovery Service the page this replaces returned HTTP 500 - a
     * NullPointerException out of the Freemarker view - so this is not a like-for-like regression risk.
     */
    public Result status(long now) {
        var snapshot = registry.applications();
        StringBuilder json = new StringBuilder(256);
        json.append('{');
        json.append("\"registrySize\":").append(registry.size()).append(',');
        json.append("\"evictionAllowed\":").append(registry.evictionAllowed()).append(',');
        json.append("\"appsHashCode\":").append(quote(snapshot.appsHashCode())).append(',');
        json.append("\"version\":").append(snapshot.version()).append(',');
        json.append("\"applications\":[");

        boolean firstApp = true;
        for (Application application : snapshot.applications()) {
            if (!firstApp) {
                json.append(',');
            }
            firstApp = false;
            json.append('{').append("\"name\":").append(quote(application.name())).append(',');
            json.append("\"instances\":[");
            boolean firstInstance = true;
            for (ServiceInstance instance : application.instances()) {
                if (!firstInstance) {
                    json.append(',');
                }
                firstInstance = false;
                var lease = instance.lease();
                json.append('{')
                    .append("\"instanceId\":").append(quote(instance.instanceId())).append(',')
                    .append("\"status\":").append(quote(instance.effectiveStatus().name())).append(',')
                    .append("\"reportedStatus\":").append(quote(String.valueOf(instance.status()))).append(',')
                    .append("\"permanentLease\":").append(lease != null && lease.permanent()).append(',')
                    .append("\"secondsSinceRenewal\":")
                    .append(lease == null ? -1 : Math.max(0, (now - lease.lastRenewalTimestamp()) / 1000))
                    .append('}');
            }
            json.append("]}");
        }
        json.append("]}");
        return Result.json(json.toString());
    }

    /** Minimal JSON string escaping - registry values are ids and status names, not free text. */
    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    // ---------------------------------------------------------------------------------------------------------
    // Peer replication
    // ---------------------------------------------------------------------------------------------------------

    public Result replicate(String payload) {
        ReplicationBatch batch = codec.decodeReplicationBatch(payload);
        List<ReplicationResponse.Item> results = new ArrayList<>(batch.size());
        for (ReplicationItem item : batch.items()) {
            results.add(new ReplicationResponse.Item(apply(item), null));
        }
        return Result.json(codec.encode(new ReplicationResponse(results)));
    }

    /**
     * Applies one replicated change.
     * <p>
     * The 404 on an unknown heartbeat is the load-bearing case: it tells the sender "I have never seen that
     * instance, send me the full registration". Answering 200 instead leaves the instance permanently absent from
     * this node after a partition heals.
     */
    private int apply(ReplicationItem item) {
        ReplicationAction action = item.action();
        if (action == null) {
            return 400;
        }
        boolean applied;
        switch (action) {
            case Register -> {
                if (item.instance() == null) {
                    applied = false;
                } else {
                    registry.register(item.instance(), RegistrationKind.REPLICATED);
                    applied = true;
                }
            }
            case Heartbeat -> applied = registry.renew(item.appName(), item.id(), true);
            case Cancel -> applied = registry.cancel(item.appName(), item.id(), true);
            case StatusUpdate -> applied = registry.overrideStatus(
                item.appName(), item.id(), InstanceStatus.fromWire(item.status()), true);
            case DeleteStatusOverride -> applied = registry.clearStatusOverride(
                item.appName(), item.id(), InstanceStatus.fromWire(item.status()), true);
            default -> applied = false;
        }
        return applied ? 200 : 404;
    }

    public static boolean isReplication(String header) {
        return Boolean.parseBoolean(header);
    }

}
