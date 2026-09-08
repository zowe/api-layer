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

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.codec.WireFormat;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.replication.ReplicationAction;
import org.zowe.apiml.registry.replication.ReplicationBatch;
import org.zowe.apiml.registry.replication.ReplicationItem;
import org.zowe.apiml.registry.replication.ReplicationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code /eureka} HTTP surface, served from {@link ServiceRegistry}.
 * <p>
 * Replaces Eureka's Jersey resources and the {@code EurekaRestController} adapter the modulith needed to call them
 * from a reactive stack. Every status code, header and body shape here is pinned by
 * {@code apiml-registry/src/test/resources/wire-contract} - both the response bodies and
 * {@code http-contract.json}, which was captured from the running Eureka-based service.
 * <p>
 * Two things that look like bugs and are not:
 * <ul>
 *     <li>A miss returns {@code 404} with a <b>completely empty body and no Content-Type</b>. Enablers rely on
 *         this to detect that they must re-register; returning a JSON error document breaks reconnect
 *         (api-layer commit {@code 9f58010c6}).</li>
 *     <li>XML is the default representation. See {@link RegistryRepresentation}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/eureka")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(ServiceRegistry.class)
public class RegistryController {

    /*
     * The condition above keeps this dormant until something defines a ServiceRegistry bean.
     * <p>
     * While the Eureka implementation is still wired in, Jersey owns /eureka/* and there is no registry bean, so
     * activating this would both clash on the path and fail context startup. It is not a runtime feature toggle -
     * decision D2 rules those out - it is a build-order guard that disappears by itself at cutover, when the
     * Spring configuration that replaces EurekaConfig defines the registry.
     */

    private final ServiceRegistry registry;
    private final RegistryCodec codec = new RegistryCodec();

    // ---------------------------------------------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------------------------------------------

    @GetMapping({"/apps", "/apps/"})
    public ResponseEntity<String> applications(
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept,
        // Accepted and ignored: APIML does not federate regions, but a client sending it must not get a 400.
        @Nullable @RequestParam(value = "regions", required = false) String regions
    ) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return body(codec.encode(registry.applications(), format), format);
    }

    @GetMapping("/apps/delta")
    public ResponseEntity<String> delta(
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept,
        @Nullable @RequestParam(value = "regions", required = false) String regions
    ) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return body(codec.encode(registry.delta(), format), format);
    }

    @GetMapping("/apps/{appId}")
    public ResponseEntity<String> application(
        @PathVariable String appId,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept
    ) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return registry.application(appId)
            .map(application -> body(codec.encode(application, format), format))
            .orElseGet(RegistryController::notFound);
    }

    @GetMapping("/apps/{appId}/{instanceId}")
    public ResponseEntity<String> instance(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept
    ) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return registry.instance(appId, instanceId)
            .map(found -> body(codec.encode(found, format), format))
            .orElseGet(RegistryController::notFound);
    }

    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<String> instanceById(
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept
    ) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        return registry.applications().applications().stream()
            .flatMap(application -> application.instances().stream())
            .filter(candidate -> instanceId.equals(candidate.instanceId()))
            .findFirst()
            .map(found -> body(codec.encode(found, format), format))
            .orElseGet(RegistryController::notFound);
    }

    @GetMapping("/vips/{vipAddress}")
    public ResponseEntity<String> vip(
        @PathVariable String vipAddress,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept
    ) {
        return vipResponse(registry.byVipAddress(vipAddress), accept, eurekaAccept);
    }

    @GetMapping("/svips/{svipAddress}")
    public ResponseEntity<String> secureVip(
        @PathVariable String svipAddress,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = RegistryRepresentation.EUREKA_ACCEPT_HEADER, required = false) String eurekaAccept
    ) {
        return vipResponse(registry.bySecureVipAddress(svipAddress), accept, eurekaAccept);
    }

    /** A VIP lookup answers with the same envelope as a full registry read, filtered to the matching instances. */
    private ResponseEntity<String> vipResponse(
        List<ServiceInstance> instances, String accept, String eurekaAccept
    ) {
        WireFormat format = RegistryRepresentation.resolve(accept, eurekaAccept);
        Map<String, List<ServiceInstance>> grouped = new java.util.LinkedHashMap<>();
        for (ServiceInstance instance : instances) {
            grouped.computeIfAbsent(instance.appName(), name -> new ArrayList<>()).add(instance);
        }
        List<org.zowe.apiml.registry.model.Application> applications = new ArrayList<>();
        grouped.forEach((name, list) ->
            applications.add(new org.zowe.apiml.registry.model.Application(name, list)));

        var snapshot = registry.applications();
        var filtered = new org.zowe.apiml.registry.model.Applications(
            applications, snapshot.version(), snapshot.appsHashCode());
        return body(codec.encode(filtered, format), format);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------------------------------------------

    @PostMapping("/apps/{appId}")
    public ResponseEntity<Void> register(
        @PathVariable String appId,
        @RequestBody String payload,
        @Nullable @RequestHeader(value = "x-netflix-discovery-replication", required = false) String replication
    ) {
        ServiceInstance instance = codec.decodeInstance(payload);
        registry.register(instance, isReplication(replication) ? RegistrationKind.REPLICATED : RegistrationKind.DYNAMIC);
        // 204, not 200: Eureka's register returns no content, and clients treat a body here as unexpected.
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/apps/{appId}/{instanceId}")
    public ResponseEntity<Void> renew(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = "x-netflix-discovery-replication", required = false) String replication
    ) {
        return registry.renew(appId, instanceId, isReplication(replication))
            ? ResponseEntity.ok().build()
            : emptyNotFound();
    }

    @DeleteMapping("/apps/{appId}/{instanceId}")
    public ResponseEntity<Void> cancel(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = "x-netflix-discovery-replication", required = false) String replication
    ) {
        return registry.cancel(appId, instanceId, isReplication(replication))
            ? ResponseEntity.ok().build()
            : emptyNotFound();
    }

    @PutMapping("/apps/{appId}/{instanceId}/status")
    public ResponseEntity<Void> overrideStatus(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @RequestParam("value") String value,
        @Nullable @RequestParam(value = "lastDirtyTimestamp", required = false) Long lastDirtyTimestamp,
        @Nullable @RequestHeader(value = "x-netflix-discovery-replication", required = false) String replication
    ) {
        return registry.overrideStatus(appId, instanceId, InstanceStatus.fromWire(value), isReplication(replication))
            ? ResponseEntity.ok().build()
            : emptyNotFound();
    }

    @DeleteMapping("/apps/{appId}/{instanceId}/status")
    public ResponseEntity<Void> clearStatusOverride(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestParam(value = "value", required = false) String value,
        @Nullable @RequestHeader(value = "x-netflix-discovery-replication", required = false) String replication
    ) {
        InstanceStatus revertTo = value == null ? InstanceStatus.UP : InstanceStatus.fromWire(value);
        return registry.clearStatusOverride(appId, instanceId, revertTo, isReplication(replication))
            ? ResponseEntity.ok().build()
            : emptyNotFound();
    }

    /**
     * Metadata update by query parameter, e.g. {@code ?apiml.externalUrl=https://…}.
     * <p>
     * Kept because it is in use: the integration suite drives it, and it is how the domain allow-list is
     * exercised (see DiscoverableClientIntegrationTest).
     */
    @PutMapping("/apps/{appId}/{instanceId}/metadata")
    public ResponseEntity<Void> updateMetadata(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @RequestParam Map<String, String> metadata
    ) {
        return registry.updateMetadata(appId, instanceId, metadata)
            ? ResponseEntity.ok().build()
            : emptyNotFound();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Peer replication
    // ---------------------------------------------------------------------------------------------------------

    @PostMapping({"/peerreplication/batch", "/peerreplication/batch/"})
    public ResponseEntity<String> replicate(@RequestBody String payload) {
        ReplicationBatch batch = codec.decodeReplicationBatch(payload);
        List<ReplicationResponse.Item> results = new ArrayList<>(batch.size());

        for (ReplicationItem item : batch.items()) {
            results.add(new ReplicationResponse.Item(apply(item), null));
        }
        String encoded = codec.encode(new ReplicationResponse(results));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(encoded);
    }

    /**
     * Applies one replicated change and reports the peer-visible status.
     * <p>
     * The 404 on an unknown heartbeat is the important one: it is how this node tells the sender "I have never
     * seen that instance, send me the full registration". Answering 200 instead leaves the instance permanently
     * absent from this node's registry after a partition.
     */
    private int apply(ReplicationItem item) {
        ReplicationAction action = item.action();
        if (action == null) {
            return HttpStatus.BAD_REQUEST.value();
        }
        boolean applied = switch (action) {
            case Register -> {
                if (item.instance() == null) {
                    yield false;
                }
                registry.register(item.instance(), RegistrationKind.REPLICATED);
                yield true;
            }
            case Heartbeat -> registry.renew(item.appName(), item.id(), true);
            case Cancel -> registry.cancel(item.appName(), item.id(), true);
            case StatusUpdate -> registry.overrideStatus(
                item.appName(), item.id(), InstanceStatus.fromWire(item.status()), true);
            case DeleteStatusOverride -> registry.clearStatusOverride(
                item.appName(), item.id(), InstanceStatus.fromWire(item.status()), true);
        };
        return applied ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------

    private static boolean isReplication(String header) {
        return Boolean.parseBoolean(header);
    }

    private static ResponseEntity<String> body(String payload, WireFormat format) {
        return ResponseEntity.ok()
            .contentType(RegistryRepresentation.contentTypeFor(format))
            .body(payload);
    }

    /**
     * A miss: 404, no body, no Content-Type.
     * <p>
     * Spring would happily add a Content-Type for a null body, so the header is not set at all here. That
     * emptiness is contractual - see the class comment.
     */
    private static ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private static ResponseEntity<Void> emptyNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** Exposed so tests can assert the read path without a servlet container. */
    Optional<ServiceInstance> lookup(String appName, String instanceId) {
        return registry.instance(appName, instanceId);
    }

}
