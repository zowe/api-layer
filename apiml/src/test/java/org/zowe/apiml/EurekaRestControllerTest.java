/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.cluster.protocol.ReplicationList;
import com.netflix.eureka.resources.ASGResource;
import com.netflix.eureka.resources.ApplicationResource;
import com.netflix.eureka.resources.ApplicationsResource;
import com.netflix.eureka.resources.InstanceResource;
import com.netflix.eureka.resources.InstancesResource;
import com.netflix.eureka.resources.PeerReplicationResource;
import com.netflix.eureka.resources.SecureVIPResource;
import com.netflix.eureka.resources.ServerInfoResource;
import com.netflix.eureka.resources.VIPResource;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.EurekaRestController.UriInfoAdapter;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EurekaRestControllerTest {

    @Mock private ApplicationsResource applicationsResource;
    @Mock private VIPResource vipResource;
    @Mock private ServerInfoResource serverInfoResource;
    @Mock private SecureVIPResource secureVIPResource;
    @Mock private InstancesResource instancesResource;
    @Mock private ASGResource asgResource;
    @Mock private PeerReplicationResource peerReplicationResource;

    @Mock private ServerWebExchange serverWebExchange;

    @Mock private MockServerHttpRequest httpRequest;

    @Mock private Response response;

    private static final int[] RETURN_CODES = new int[]{ 200, 204, 404, 500, 503 };

    private int returnCode;
    private Object body;
    private HttpHeaders headers = HttpHeaders.EMPTY;

    @InjectMocks
    private EurekaRestController controller;

    private Predicate<? super ResponseEntity<?>> entityMatcher = entity -> {
        return entity.getStatusCode().equals(HttpStatusCode.valueOf(returnCode))
            && Objects.equals(entity.getBody(), body)
            && headersEqual(entity.getHeaders(), headers);
    };

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    void setUp() {
        lenient().when(serverWebExchange.getRequest()).thenReturn(httpRequest);

        MultivaluedMap<String, Object> headersMap = new MultivaluedHashMap<>(
            Map.of("Accept", "application/json")
        );

        returnCode = RETURN_CODES[new Random().nextInt(5)];
        body = new Object();
        headers = new HttpHeaders(CollectionUtils.toMultiValueMap((Map)headersMap));

        lenient().when(response.getStatus()).thenReturn(returnCode);
        lenient().when(response.getEntity()).thenReturn(body);
        lenient().when(response.getHeaders()).thenReturn(headersMap);
    }

    private boolean headersEqual(HttpHeaders h1, HttpHeaders h2) {
        var equal = true;

        var set1 = h1.headerSet();
        var set2 = h2.headerSet();

        var i1 = set1.iterator();
        var i2 = set2.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            var e1 = i1.next();
            var e2 = i2.next();

            equal = e1.getKey().equals(e2.getKey()) && Arrays.equals(e1.getValue().toArray(), e2.getValue().toArray());
        }

        return set1.size() == set2.size() && equal;
    }

    @Test
    void getContainers() {
        when(applicationsResource.getContainers(eq("v2"), eq("application/json"), eq("chunked"), eq("Eureka-Accept-Value"), any(UriInfo.class), eq("regions")))
            .thenReturn(response);
        StepVerifier.create(controller.getContainers(serverWebExchange, "application/json", "chunked", "Eureka-Accept-Value", "regions"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void getContainerDifferential() {
        when(applicationsResource.getContainerDifferential(eq("v2"), eq("application/json"), eq("chunked"), eq("Eureka-Accept-Value"), any(UriInfo.class), eq("regions")))
            .thenReturn(response);
        StepVerifier.create(controller.getContainerDifferential(serverWebExchange, "application/json", "chunked", "Eureka-Accept-Value", "regions"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();

    }

    @Test
    void getApplicationResource() {
        var appResourceMock = mock(ApplicationResource.class);
        when(applicationsResource.getApplicationResource("v2", "applicationId"))
            .thenReturn(appResourceMock);
        when(appResourceMock.getApplication("v2", "application/json", "value"))
            .thenReturn(response);

        StepVerifier.create(controller.getApplicationResource("application/json", "value", "applicationId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void addInstance() throws IOException {
        var appResource = mock(ApplicationResource.class);
        when(applicationsResource.getApplicationResource("v2", "anAppId"))
            .thenReturn(appResource);
        when(appResource.addInstance(any(InstanceInfo.class), eq("true")))
            .thenReturn(response);

        var instanceInfoJson = """
            {
                "instance": {
                "hostName": "localhost",
                "app": "TESTAPP",
                "ipAddr": "127.0.0.1",
                "vipAddress": "testapp",
                "secureVipAddress": "testapp",
                "status": "UP",
                "port": {
                    "$": 8080,
                    "@enabled": "true"
                },
                "dataCenterInfo": {
                    "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    "name": "MyOwn"
                }
                }
            }
            """;

        StepVerifier.create(controller.addInstance("true", instanceInfoJson, "anAppId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void getInstanceInfo() {
        var appResource = mock(ApplicationResource.class);
        var instanceInfo = mock(InstanceResource.class);
        when(applicationsResource.getApplicationResource("v2", "anAppId"))
            .thenReturn(appResource);
        when(appResource.getInstanceInfo("anInstanceId"))
            .thenReturn(instanceInfo);
        when(instanceInfo.getInstanceInfo())
            .thenReturn(response);

        StepVerifier.create(controller.getInstanceInfo("anAppId", "anInstanceId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void renewLease() {
        var appResource = mock(ApplicationResource.class);
        var instanceInfo = mock(InstanceResource.class);
        when(applicationsResource.getApplicationResource("v2", "anAppId"))
            .thenReturn(appResource);
        when(appResource.getInstanceInfo("anInstanceId"))
            .thenReturn(instanceInfo);
        when(instanceInfo.renewLease("true", "DOWN", "UP", "12:00:00"))
            .thenReturn(response);

        StepVerifier.create(controller.renewLease("true", "DOWN", "UP", "12:00:00", "anAppId", "anInstanceId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void statusUpdate() {
        var appResource = mock(ApplicationResource.class);
        var instanceInfo = mock(InstanceResource.class);
        when(applicationsResource.getApplicationResource("v2", "anAppId"))
            .thenReturn(appResource);
        when(appResource.getInstanceInfo("anInstanceId"))
            .thenReturn(instanceInfo);
        when(instanceInfo.statusUpdate("UP", "true", "12:00:00"))
            .thenReturn(response);

        StepVerifier.create(controller.statusUpdate("true", "UP", "12:00:00", "anAppId", "anInstanceId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void deleteStatusUpdate() {
        var appResource = mock(ApplicationResource.class);
        var instanceInfo = mock(InstanceResource.class);
        when(applicationsResource.getApplicationResource("v2", "anAppId"))
            .thenReturn(appResource);
        when(appResource.getInstanceInfo("anInstanceId"))
            .thenReturn(instanceInfo);
        when(instanceInfo.deleteStatusUpdate("true", "UP", "12:00:00"))
            .thenReturn(response);

        StepVerifier.create(controller.deleteStatusUpdate("true", "UP", "12:00:00", "anAppId", "anInstanceId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void updateMetadata() {
        var appResource = mock(ApplicationResource.class);
        var instanceInfo = mock(InstanceResource.class);
        when(applicationsResource.getApplicationResource("v2", "anAppId"))
            .thenReturn(appResource);
        when(appResource.getInstanceInfo("anInstanceId"))
            .thenReturn(instanceInfo);
        when(instanceInfo.updateMetadata(any(UriInfo.class)))
            .thenReturn(response);

        StepVerifier.create(controller.updateMetadata(serverWebExchange, "anAppId", "anInstanceId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void cancelLease() {
        var app = mock(ApplicationResource.class);
        var instanceResource = mock(InstanceResource.class);
        when(applicationsResource.getApplicationResource("v2", "appId"))
            .thenReturn(app);
        when(app.getInstanceInfo("instanceId"))
            .thenReturn(instanceResource);
        when(instanceResource.cancelLease("true"))
            .thenReturn(response);

        StepVerifier.create(controller.cancelLease("true", "appId", "instanceId"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void getById() {
        when(instancesResource.getById("v2", "id"))
            .thenReturn(response);

        StepVerifier.create(controller.getById("id"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void secureVipStatusUpdate() {
        when(secureVIPResource.statusUpdate("v2", "hostname", "application/json", "value"))
            .thenReturn(response);

        StepVerifier.create(controller.secureVipStatusUpdate("application/json", "value", "hostname"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void vipStatusUpdate() {
        when(vipResource.statusUpdate("v2", "hostname", "application/json", "value"))
            .thenReturn(response);

        StepVerifier.create(controller.vipStatusUpdate("application/json", "value", "hostname"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void getOverrides() throws Exception {
        when(serverInfoResource.getOverrides()).thenReturn(response);

        StepVerifier.create(controller.getOverrides())
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void argStatusUpdate() {
        when(asgResource.statusUpdate("name", "UP", "false"))
            .thenReturn(response);

        StepVerifier.create(controller.asgStatusUpdate("false", "UP", "name"))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Test
    void batchReplication() throws IOException {
        when(peerReplicationResource.batchReplication(any(ReplicationList.class)))
            .thenReturn(response);

        var replicationListJson = """
            {
              "replicationList": [
                {
                  "action": "Register",
                  "appName": "TESTAPP",
                  "id": "localhost:TESTAPP:8080"
                }
              ]
            }
            """;

        StepVerifier.create(controller.batchReplication(replicationListJson))
            .expectNextMatches(entityMatcher)
            .verifyComplete();
    }

    @Nested
    class UriInfoAdapterTest {

        private UriInfoAdapter adapter;
        private MockServerHttpRequest request;

        @BeforeEach
        void setUp() {
            request = MockServerHttpRequest.get("https://localhost:10011/eureka").queryParam("param1", "value1").build();
            this.adapter = new UriInfoAdapter(request);
        }

        @Test
        void getPath() {
            assertEquals("/eureka", adapter.getPath());
        }

        @Test
        void getPathWithDecode() {
            assertEquals("/eureka", adapter.getPath(true));
            assertEquals("/eureka", adapter.getPath(false));
        }

        @Test
        void getPathSegments() {
            request = MockServerHttpRequest.get("https://localhost:10011/eureka/1/2").contextPath("/eureka").queryParam("param1", "value1").build();
            this.adapter = new UriInfoAdapter(request);
            var pathSegments = adapter.getPathSegments();
            assertEquals(2, pathSegments.size());
            assertEquals("/", pathSegments.get(0).getPath());
            assertEquals("eureka", pathSegments.get(1).getPath());
        }

        @Test
        void getPathSegments_withDecode() {
            request = MockServerHttpRequest.get("https://localhost:10011/eureka/1/2").contextPath("/eureka").queryParam("param1", "value1").build();
            this.adapter = new UriInfoAdapter(request);
            var pathSegments = adapter.getPathSegments(true);
            assertEquals(2, pathSegments.size());
            assertEquals("/", pathSegments.get(0).getPath());
            assertTrue(pathSegments.get(0).getMatrixParameters().isEmpty());
            assertEquals("eureka", pathSegments.get(1).getPath());
        }

        @Test
        void getRequestUri() {
            assertEquals(request.getURI(), adapter.getRequestUri());
        }

        @Test
        void getRequestUriBuilder() {
            assertEquals(request.getURI(), adapter.getRequestUriBuilder().build());
        }

        @Test
        void getAbsolutePath() {
            assertEquals(request.getURI(), adapter.getAbsolutePath());
        }

        @Test
        void getAbsolutePathBuilder() {
            assertEquals(request.getURI(), adapter.getAbsolutePathBuilder().build());
        }

        @Test
        void getBaseUri() {
            assertEquals(URI.create("https://localhost:10011/eureka/?param1=value1"), adapter.getBaseUri());
        }

        @Test
        void getBaseUriBuilder() {
            assertEquals(URI.create("https://localhost:10011/eureka/?param1=value1"), adapter.getBaseUriBuilder().build());
        }

        @Test
        void getPathParameters() {
            assertTrue(adapter.getPathParameters().isEmpty());
        }

        @Test
        void getPathParametersWithDecode() {
            assertTrue(adapter.getPathParameters(true).isEmpty());
            assertTrue(adapter.getPathParameters(false).isEmpty());
        }

        @Test
        void getQueryParameters() {
            var params = adapter.getQueryParameters();
            assertEquals(1, params.size());
            assertEquals(List.of("value1"), params.get("param1"));
        }

        @Test
        void getQueryParametersWithDecode() {
            var params = adapter.getQueryParameters(true);
            assertEquals(1, params.size());
            assertEquals(List.of("value1"), params.get("param1"));
        }

        @Test
        void getMatchedURIs() {
            assertTrue(adapter.getMatchedURIs().isEmpty());
        }

        @Test
        void getMatchedURIsWithDecode() {
            assertTrue(adapter.getMatchedURIs(false).isEmpty());
            assertTrue(adapter.getMatchedURIs(true).isEmpty());
        }

        @Test
        void getMatchedResources() {
            assertTrue(adapter.getMatchedResources().isEmpty());
        }

        @Test
        void resolveUri() {
            var uri = URI.create("https://localhost:10011/eureka");
            assertEquals(uri, adapter.resolve(uri));
        }

        @Test
        void relativizeUri() {
            var uri = URI.create("https://localhost:10011/eureka");
            assertEquals(uri, adapter.relativize(uri));
        }

    }

}
