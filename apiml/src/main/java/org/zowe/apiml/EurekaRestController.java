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
import com.netflix.eureka.resources.*;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.*;
import static org.zowe.apiml.EurekaConfiguration.JACKSON_JSON;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/eureka", produces = { "application/xml", "application/json" })
@DependsOn("modulithConfig")
@Slf4j
public class EurekaRestController {

    private static final String EUREKA_VERSION = "v2";
    private static final String HEADER_GZIP_VALUE = "gzip";

    private final ApplicationsResource applicationsResource = new ApplicationsResource();
    private final VIPResource vipResource = new VIPResource();
    private final ServerInfoResource serverInfoResource = new ServerInfoResource();
    private final SecureVIPResource secureVIPResource = new SecureVIPResource();
    private final InstancesResource instancesResource = new InstancesResource();
    private final ASGResource asgResource = new ASGResource();
    private final PeerReplicationResource peerReplicationResource = new PeerReplicationResource();

    private String fixAcceptEncoding(String acceptEncoding) {
        if (acceptEncoding == null) {
            return null;
        }

        // to remove gzip
        return Arrays.stream(StringUtils.split(acceptEncoding, ","))
            .map(StringUtils::trim)
            .filter(value -> !StringUtils.equals(value, HEADER_GZIP_VALUE))
            .collect(Collectors.joining(", "));

    }

    private UriInfo getUriInfo(ServerWebExchange serverWebExchange) {
        return new UriInfoAdapter(serverWebExchange.getRequest());
    }

    private Mono<ResponseEntity<?>> convertResponse(Response response) {
        String contentType = response.getHeaderString(CONTENT_TYPE);
        if (StringUtils.isEmpty(contentType)) {
            contentType = APPLICATION_JSON;
        }

        return Mono.just(ResponseEntity
            .status(response.getStatus())
            .header(CONTENT_TYPE, contentType)
            .body(response.getEntity()));
    }

    @GetMapping(value = {"/apps", "/apps/"}, produces = { "application/xml", "application/json" })
    public Mono<ResponseEntity<?>> getContainers(
        ServerWebExchange serverWebExchange,
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam("regions") String regionsStr
    ) {
        return convertResponse(applicationsResource.getContainers(
            EUREKA_VERSION, acceptHeader, fixAcceptEncoding(acceptEncoding), eurekaAccept, getUriInfo(serverWebExchange), regionsStr
        ));
    }

    @GetMapping("/apps/delta")
    public Mono<ResponseEntity<?>> getContainerDifferential(
        ServerWebExchange serverWebExchange,
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam("regions") String regionsStr
    ) {
        return convertResponse(applicationsResource.getContainerDifferential(
            EUREKA_VERSION, acceptHeader, fixAcceptEncoding(acceptEncoding), eurekaAccept, getUriInfo(serverWebExchange), regionsStr
        ));
    }

    @GetMapping("/apps/{appId}")
    public Mono<ResponseEntity<?>> getApplicationResource(
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,

        @PathVariable("appId") String appId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        return convertResponse(app.getApplication(EUREKA_VERSION, acceptHeader, eurekaAccept));
    }

    @PostMapping("/apps/{appId}")
    public Mono<ResponseEntity<?>> addInstance(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,

        @RequestBody String instanceInfoString,
        @PathVariable("appId") String appId
    ) throws IOException {
        var instanceInfo = JACKSON_JSON.decode(instanceInfoString, InstanceInfo.class);
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        return convertResponse(app.addInstance(instanceInfo, isReplication));
    }

    @GetMapping("/apps/{appId}/{instanceId}")
    public Mono<ResponseEntity<?>> getInstanceInfo(
        @PathVariable("appId") String appId,
        @PathVariable("instanceId") String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return convertResponse(instance.getInstanceInfo());
    }

    @PutMapping("/apps/{appId}/{instanceId}")
    public Mono<ResponseEntity<?>> renewLease(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("overriddenstatus") String overriddenStatus,
        @Nullable @RequestParam("status") String status,
        @Nullable @RequestParam("lastDirtyTimestamp") String lastDirtyTimestamp,

        @PathVariable("appId") String appId,
        @PathVariable("instanceId") String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return convertResponse(instance.renewLease(isReplication, overriddenStatus, status, lastDirtyTimestamp));
    }

    @PutMapping("/apps/{appId}/{instanceId}/status")
    public Mono<ResponseEntity<?>> statusUpdate(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("value") String newStatus,
        @Nullable @RequestParam("lastDirtyTimestamp") String lastDirtyTimestamp,

        @PathVariable("appId") String appId,
        @PathVariable("instanceId") String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return convertResponse(instance.statusUpdate(newStatus, isReplication, lastDirtyTimestamp));
    }

    @DeleteMapping("/apps/{appId}/{instanceId}/status")
    public Mono<ResponseEntity<?>> deleteStatusUpdate(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("value") String newStatusValue,
        @Nullable @RequestParam("lastDirtyTimestamp") String lastDirtyTimestamp,

        @PathVariable("appId") String appId,
        @PathVariable("instanceId") String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return convertResponse(instance.deleteStatusUpdate(isReplication, newStatusValue, lastDirtyTimestamp));
    }

    @PutMapping("/apps/{appId}/{instanceId}/metadata")
    public Mono<ResponseEntity<?>> updateMetadata(
        ServerWebExchange serverWebExchange,

        @PathVariable("appId") String appId,
        @PathVariable("instanceId") String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return convertResponse(instance.updateMetadata(getUriInfo(serverWebExchange)));
    }

    @DeleteMapping("/apps/{appId}/{instanceId}")
    public Mono<ResponseEntity<?>> cancelLease(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,

        @PathVariable("appId") String appId,
        @PathVariable("instanceId") String instanceId
    ) {
        var app = applicationsResource.getApplicationResource(EUREKA_VERSION, appId);
        var instance = app.getInstanceInfo(instanceId);
        return convertResponse(instance.cancelLease(isReplication));
    }

    @GetMapping("/instances/{id}")
    public Mono<ResponseEntity<?>> getById(
        @PathVariable("id") String id
    ) {
        return convertResponse(instancesResource.getById(EUREKA_VERSION, id));
    }

    @GetMapping("/svips/{svipAddress}")
    public Mono<ResponseEntity<?>> secureVipStatusUpdate(
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,

        @PathVariable("svipAddress") String svipAddress
    ) {
        return convertResponse(secureVIPResource.statusUpdate(EUREKA_VERSION, svipAddress, acceptHeader, eurekaAccept));
    }

    @GetMapping("/vips/{vipAddress}")
    public Mono<ResponseEntity<?>> vipStatusUpdate(
        @Nullable @RequestHeader(ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,

        @PathVariable("vipAddress") String vipAddress
    ) {
        return convertResponse(vipResource.statusUpdate(EUREKA_VERSION, vipAddress, acceptHeader, eurekaAccept));
    }

    @GetMapping("/serverinfo/statusoverrides")
    public Mono<ResponseEntity<?>> getOverrides() throws Exception {
        return convertResponse(serverInfoResource.getOverrides());
    }

    @PutMapping("/asg/{asgName}/status")
    public Mono<ResponseEntity<?>> asgStatusUpdate(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @Nullable @RequestParam("value") String newStatus,

        @PathVariable("asgName") String asgName
    ) {
        return convertResponse(asgResource.statusUpdate(asgName, newStatus, isReplication));
    }

    @PostMapping
    public Mono<ResponseEntity<?>> batchReplication(
        @RequestBody String replicationListString
    ) throws IOException {
        var replicationList = JACKSON_JSON.decode(replicationListString, ReplicationList.class);
        return convertResponse(peerReplicationResource.batchReplication(replicationList));
    }

    @RequiredArgsConstructor
    private static class UriInfoAdapter implements UriInfo {

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
