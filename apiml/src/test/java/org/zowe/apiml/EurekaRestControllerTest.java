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

import com.netflix.eureka.resources.ASGResource;
import com.netflix.eureka.resources.ApplicationResource;
import com.netflix.eureka.resources.ApplicationsResource;
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
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

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
    void addInstance() {

    }

    @Test
    void getInstanceInfo() {

    }

    @Test
    void renewLease() {

    }

    @Test
    void statusUpdate() {

    }

    @Test
    void deleteStatusUpdate() {

    }

    @Test
    void updateMetadata() {

    }

    @Test
    void cancelLease() {

    }

    @Test
    void getById() {

    }

    @Test
    void secureVipStatusUpdate() {

    }

    @Test
    void vipStatusUpdate() {

    }

    @Test
    void getOverrides() {

    }

    @Test
    void argStatusUpdate() {

    }

    @Test
    void batchReplication() {

    }

}
