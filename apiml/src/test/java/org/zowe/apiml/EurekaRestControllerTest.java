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
import com.netflix.eureka.resources.ApplicationsResource;
import com.netflix.eureka.resources.InstancesResource;
import com.netflix.eureka.resources.PeerReplicationResource;
import com.netflix.eureka.resources.SecureVIPResource;
import com.netflix.eureka.resources.ServerInfoResource;
import com.netflix.eureka.resources.VIPResource;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

    @InjectMocks
    private EurekaRestController controller;

    @BeforeEach
    void setUp() {
        lenient().when(serverWebExchange.getRequest()).thenReturn(httpRequest);

        lenient().when(response.getStatus()).thenReturn(200);
        lenient().when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    @Test
    void getContainers() {
        when(applicationsResource.getContainers(eq("v2"), eq("application/json"), eq("chunked"), eq("Eureka-Accept-Value"), any(UriInfo.class), eq("regions")))
            .thenReturn(response);
        StepVerifier.create(controller.getContainers(serverWebExchange, "application/json", "chunked", "Eureka-Accept-Value", "regions"))
            .expectNextMatches(entity -> entity.getStatusCode().equals(HttpStatusCode.valueOf(200)))
            .verifyComplete();
    }

    @Test
    void getContainerDifferential() {

    }

    @Test
    void getApplicationResource() {

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
