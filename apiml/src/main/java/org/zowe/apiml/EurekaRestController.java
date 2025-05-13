/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zowe.apiml;

import com.netflix.appinfo.EurekaAccept;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.protocol.ReplicationList;
import com.netflix.eureka.resources.ASGResource;
import com.netflix.eureka.resources.ApplicationsResource;
import com.netflix.eureka.resources.InstancesResource;
import com.netflix.eureka.resources.PeerReplicationResource;
import com.netflix.eureka.resources.SecureVIPResource;
import com.netflix.eureka.resources.ServerInfoResource;
import com.netflix.eureka.resources.VIPResource;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.ACCEPT_ENCODING;
import static org.zowe.apiml.EurekaConfiguration.JACKSON_JSON;
import static reactor.core.publisher.Mono.just;

@SuppressWarnings("java:S1452") // Generic type wildcard needed due to legacy code usage
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/eureka", produces = { "application/xml", "application/json" })
@DependsOn("modulithConfig")
@Slf4j
public class EurekaRestController {

    private static final String EUREKA_VERSION = "v2";

    private final ApplicationsResource applicationsResource;
    private final VIPResource vipResource;
    private final ServerInfoResource serverInfoResource;
    private final SecureVIPResource secureVIPResource;
    private final InstancesResource instancesResource;
    private final ASGResource asgResource;
    private final PeerReplicationResource peerReplicationResource;

    private UriInfo getUriInfo(ServerWebExchange serverWebExchange) {
        return new UriInfoAdapter(serverWebExchange.getRequest());
    }

    private ResponseEntity<?> convertResponse(Response response) {
        return ResponseEntity
            .status(response.getStatus())
            .headers(headers -> response.getHeaders().entrySet().forEach(
                newHeader -> headers.addAll(newHeader.getKey(), newHeader.getValue().stream().map(String::valueOf).toList()))
            )
            .body(response.getEntity());
    }

    @GetMapping(value = {"/apps", "/apps/"}, produces = { "application/xml", "application/json" })
    public Mono<ResponseEntity<?>> getContainers(
        ServerWebExchange serverWebExchange,
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam("regions") String regionsStr
    ) {
        return just(convertResponse(applicationsResource.getContainers(
            EUREKA_VERSION, acceptHeader, acceptEncoding, eurekaAccept, getUriInfo(serverWebExchange), regionsStr
        )));
    }

    @GetMapping("/apps/delta")
    public Mono<ResponseEntity<?>> getContainerDifferential(
        ServerWebExchange serverWebExchange,
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam("regions") String regionsStr
    ) {
        return just(convertResponse(applicationsResource.getContainerDifferential(
            EUREKA_VERSION, acceptHeader, acceptEncoding, eurekaAccept, getUriInfo(serverWebExchange), regionsStr
        )));
    }

    @GetMapping("/apps/{appId}")
    public Mono<ResponseEntity<?>> getApplicationResource(
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,

        @PathVariable String appId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        return just(convertResponse(app.getApplication(EUREKA_VERSION, acceptHeader, eurekaAccept)));
    }

    @PostMapping("/apps/{appId}")
    public Mono<ResponseEntity<?>> addInstance(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,

        @RequestBody String instanceInfoString,
        @PathVariable String appId
    ) throws IOException {
        var instanceInfo = JACKSON_JSON.decode(instanceInfoString, InstanceInfo.class);
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);

        return just(ResponseEntity.ok())
            .publishOn(Schedulers.boundedElastic())
            .map(bodyBuilder -> {
                var response = app.addInstance(instanceInfo, isReplication);
                return convertResponse(response);
            });
    }

    @GetMapping("/apps/{appId}/{instanceId}")
    public Mono<ResponseEntity<?>> getInstanceInfo(
        @PathVariable String appId,
        @PathVariable String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return just(convertResponse(instance.getInstanceInfo()));
    }

    @PutMapping("/apps/{appId}/{instanceId}")
    public Mono<ResponseEntity<?>> renewLease(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("overriddenstatus") String overriddenStatus,
        @Nullable @RequestParam String status,
        @Nullable @RequestParam String lastDirtyTimestamp,

        @PathVariable String appId,
        @PathVariable String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return just(convertResponse(instance.renewLease(isReplication, overriddenStatus, status, lastDirtyTimestamp)));
    }

    @PutMapping("/apps/{appId}/{instanceId}/status")
    public Mono<ResponseEntity<?>> statusUpdate(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("value") String newStatus,
        @Nullable @RequestParam String lastDirtyTimestamp,

        @PathVariable String appId,
        @PathVariable String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return just(convertResponse(instance.statusUpdate(newStatus, isReplication, lastDirtyTimestamp)));
    }

    @DeleteMapping("/apps/{appId}/{instanceId}/status")
    public Mono<ResponseEntity<?>> deleteStatusUpdate(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("value") String newStatusValue,
        @Nullable @RequestParam String lastDirtyTimestamp,

        @PathVariable String appId,
        @PathVariable String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return just(convertResponse(instance.deleteStatusUpdate(isReplication, newStatusValue, lastDirtyTimestamp)));
    }

    @PutMapping("/apps/{appId}/{instanceId}/metadata")
    public Mono<ResponseEntity<?>> updateMetadata(
        ServerWebExchange serverWebExchange,

        @PathVariable String appId,
        @PathVariable String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return just(convertResponse(instance.updateMetadata(getUriInfo(serverWebExchange))));
    }

    @DeleteMapping("/apps/{appId}/{instanceId}")
    public Mono<ResponseEntity<?>> cancelLease(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,

        @PathVariable String appId,
        @PathVariable String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return just(convertResponse(instance.cancelLease(isReplication)));
    }

    @GetMapping("/instances/{id}")
    public Mono<ResponseEntity<?>> getById(
        @PathVariable String id
    ) {
        return just(convertResponse(instancesResource.getById(EUREKA_VERSION, id)));
    }

    @GetMapping("/svips/{svipAddress}")
    public Mono<ResponseEntity<?>> secureVipStatusUpdate(
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,

        @PathVariable String svipAddress
    ) {
        return just(convertResponse(secureVIPResource.statusUpdate(EUREKA_VERSION, svipAddress, acceptHeader, eurekaAccept)));
    }

    @GetMapping("/vips/{vipAddress}")
    public Mono<ResponseEntity<?>> vipStatusUpdate(
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,

        @PathVariable String vipAddress
    ) {
        return just(convertResponse(vipResource.statusUpdate(EUREKA_VERSION, vipAddress, acceptHeader, eurekaAccept)));
    }

    @GetMapping("/serverinfo/statusoverrides")
    public Mono<ResponseEntity<?>> getOverrides() throws Exception {
        return just(convertResponse(serverInfoResource.getOverrides()));
    }

    @PutMapping("/asg/{asgName}/status")
    public Mono<ResponseEntity<?>> asgStatusUpdate(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("value") String newStatus,

        @PathVariable String asgName
    ) {
        return just(convertResponse(asgResource.statusUpdate(asgName, newStatus, isReplication)));
    }

    @PostMapping
    public Mono<ResponseEntity<?>> batchReplication(
        @RequestBody String replicationListString
    ) throws IOException {
        var replicationList = JACKSON_JSON.decode(replicationListString, ReplicationList.class);
        return just(convertResponse(peerReplicationResource.batchReplication(replicationList)));
    }

    @RequiredArgsConstructor
    static class UriInfoAdapter implements UriInfo {

        private final ServerHttpRequest request;

        @Override
        public String getPath() {
            return request.getURI().getPath();
        }

        @Override
        public String getPath(boolean decode) {
            return getPath();
        }

        @Override
        public List<PathSegment> getPathSegments() {
            return request.getPath().contextPath().elements().stream().map(
                e -> new PathSegment() {
                    @Override
                    public String getPath() {
                        return e.value();
                    }

                    @Override
                    public MultivaluedMap<String, String> getMatrixParameters() {
                        return new MultivaluedHashMap<>();
                    }
                }
            )
            .map(PathSegment.class::cast)
            .toList();
        }

        @Override
        public List<PathSegment> getPathSegments(boolean decode) {
            return getPathSegments();
        }

        @Override
        public URI getRequestUri() {
            return request.getURI();
        }

        @Override
        public UriBuilder getRequestUriBuilder() {
            return UriBuilder.fromUri(request.getURI());
        }

        @Override
        public URI getAbsolutePath() {
            return request.getURI();
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            return UriBuilder.fromUri(getAbsolutePath());
        }

        @Override
        public URI getBaseUri() {
            return getRequestUriBuilder().path("/").build();
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return UriBuilder.fromUri(getBaseUri());
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            return getPathParameters();
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            var map = new MultivaluedHashMap<String, String>();
            request.getQueryParams().entrySet().forEach(e -> map.addAll(e.getKey(), e.getValue()));
            return map;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            return getQueryParameters();
        }

        @Override
        public List<String> getMatchedURIs() {
            return List.of();
        }

        @Override
        public List<String> getMatchedURIs(boolean decode) {
            return List.of();
        }

        @Override
        public List<Object> getMatchedResources() {
            return List.of();
        }

        @Override
        public URI resolve(URI uri) {
            return uri;
        }

        @Override
        public URI relativize(URI uri) {
            return uri;
        }

    }

}
