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
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
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
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.codec.RegistryCodec;

import java.util.Map;

/**
 * Servlet adapter for the {@code /eureka} surface. All behaviour lives in {@link RegistryEndpoints}.
 * <p>
 * Active only on a servlet stack - the standalone Discovery Service. The modulith is reactive and uses
 * {@code ReactiveRegistryController} instead.
 */
@RestController
@RequestMapping("/eureka")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RegistryController {

    private static final String REPLICATION_HEADER = "x-netflix-discovery-replication";
    private static final String EUREKA_ACCEPT = RegistryRepresentation.EUREKA_ACCEPT_HEADER;

    private final RegistryEndpoints endpoints;

    public RegistryController(ServiceRegistry registry, RegistryCodec codec) {
        this.endpoints = new RegistryEndpoints(registry, codec);
    }

    private static ResponseEntity<String> toResponse(RegistryEndpoints.Result result) {
        var builder = ResponseEntity.status(result.status());
        // Deliberately no Content-Type when the result carries none: an empty 404 must stay completely empty.
        if (result.contentType() != null) {
            builder = builder.contentType(MediaType.parseMediaType(result.contentType()));
        }
        return result.body() == null ? builder.build() : builder.body(result.body());
    }

    @GetMapping({"/apps", "/apps/"})
    public ResponseEntity<String> applications(
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept,
        @Nullable @RequestParam(value = "regions", required = false) String regions
    ) {
        return toResponse(endpoints.applications(accept, eurekaAccept));
    }

    @GetMapping("/apps/delta")
    public ResponseEntity<String> delta(
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept,
        @Nullable @RequestParam(value = "regions", required = false) String regions
    ) {
        return toResponse(endpoints.delta(accept, eurekaAccept));
    }

    @GetMapping("/apps/{appId}")
    public ResponseEntity<String> application(
        @PathVariable String appId,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept
    ) {
        return toResponse(endpoints.application(appId, accept, eurekaAccept));
    }

    @GetMapping("/apps/{appId}/{instanceId}")
    public ResponseEntity<String> instance(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept
    ) {
        return toResponse(endpoints.instance(appId, instanceId, accept, eurekaAccept));
    }

    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<String> instanceById(
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept
    ) {
        return toResponse(endpoints.instanceById(instanceId, accept, eurekaAccept));
    }

    @GetMapping("/vips/{vipAddress}")
    public ResponseEntity<String> vip(
        @PathVariable String vipAddress,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept
    ) {
        return toResponse(endpoints.vip(vipAddress, accept, eurekaAccept));
    }

    @GetMapping("/svips/{svipAddress}")
    public ResponseEntity<String> secureVip(
        @PathVariable String svipAddress,
        @Nullable @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @Nullable @RequestHeader(value = EUREKA_ACCEPT, required = false) String eurekaAccept
    ) {
        return toResponse(endpoints.secureVip(svipAddress, accept, eurekaAccept));
    }

    @PostMapping("/apps/{appId}")
    public ResponseEntity<String> register(
        @PathVariable String appId,
        @RequestBody String payload,
        @Nullable @RequestHeader(value = REPLICATION_HEADER, required = false) String replication
    ) {
        return toResponse(endpoints.register(payload, RegistryEndpoints.isReplication(replication)));
    }

    @PutMapping("/apps/{appId}/{instanceId}")
    public ResponseEntity<String> renew(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = REPLICATION_HEADER, required = false) String replication
    ) {
        return toResponse(endpoints.renew(appId, instanceId, RegistryEndpoints.isReplication(replication)));
    }

    @DeleteMapping("/apps/{appId}/{instanceId}")
    public ResponseEntity<String> cancel(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestHeader(value = REPLICATION_HEADER, required = false) String replication
    ) {
        return toResponse(endpoints.cancel(appId, instanceId, RegistryEndpoints.isReplication(replication)));
    }

    @PutMapping("/apps/{appId}/{instanceId}/status")
    public ResponseEntity<String> overrideStatus(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @RequestParam("value") String value,
        @Nullable @RequestParam(value = "lastDirtyTimestamp", required = false) Long lastDirtyTimestamp,
        @Nullable @RequestHeader(value = REPLICATION_HEADER, required = false) String replication
    ) {
        return toResponse(
            endpoints.overrideStatus(appId, instanceId, value, RegistryEndpoints.isReplication(replication)));
    }

    @DeleteMapping("/apps/{appId}/{instanceId}/status")
    public ResponseEntity<String> clearStatusOverride(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @Nullable @RequestParam(value = "value", required = false) String value,
        @Nullable @RequestHeader(value = REPLICATION_HEADER, required = false) String replication
    ) {
        return toResponse(
            endpoints.clearStatusOverride(appId, instanceId, value, RegistryEndpoints.isReplication(replication)));
    }

    @PutMapping("/apps/{appId}/{instanceId}/metadata")
    public ResponseEntity<String> updateMetadata(
        @PathVariable String appId,
        @PathVariable String instanceId,
        @RequestParam Map<String, String> metadata
    ) {
        return toResponse(endpoints.updateMetadata(appId, instanceId, metadata));
    }

    @GetMapping({"/status", "/lastn"})
    public ResponseEntity<String> status() {
        return toResponse(endpoints.status(System.currentTimeMillis()));
    }

    @PostMapping({"/peerreplication/batch", "/peerreplication/batch/"})
    public ResponseEntity<String> replicate(@RequestBody String payload) {
        return toResponse(endpoints.replicate(payload));
    }

}
