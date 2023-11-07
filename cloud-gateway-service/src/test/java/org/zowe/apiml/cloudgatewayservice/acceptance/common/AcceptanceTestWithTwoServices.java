/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.common;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApplicationRegistry;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.Mockito.doReturn;

@Slf4j
@AcceptanceTest
public class AcceptanceTestWithTwoServices extends AcceptanceTestWithBasePath {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    @Qualifier("test")
    protected ApimlDiscoveryClientStub discoveryClient;
    @Autowired
    protected ApplicationRegistry applicationRegistry;

    @Value("${currentApplication:#{null}}")
    private String defaultCurrentApplication;

    @MockBean
    protected InstanceInfoService instanceInfoService;

    public ApplicationRegistry getApplicationRegistry() {
        return applicationRegistry;
    }

    protected HttpServer server;
    protected Service serviceWithDefaultConfiguration = new Service("serviceid2", "/serviceid2/**", "serviceid2");
    protected Service serviceWithCustomConfiguration = new Service("serviceid1", "/serviceid1/**", "serviceid1");

    private Set<MockService> createdMockServices = new HashSet<>();

    @BeforeEach
    public void prepareApplications() {
        applicationRegistry.clearApplications();
        applicationRegistry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(), false);
        applicationRegistry.addApplication(serviceWithCustomConfiguration, MetadataBuilder.customInstance(), false);
        if (defaultCurrentApplication != null) {
            applicationRegistry.setCurrentApplication(defaultCurrentApplication);
        }
    }

    @AfterEach
    public void tearDown() {
        if (server != null) server.stop(0);
        for (Iterator<MockService> i = createdMockServices.iterator(); i.hasNext(); ) {
            MockService mockService = i.next();
            i.remove();
            mockService.close();
        }

        MockService.checkAssertionErrors();
    }

    // Use rather mockService method - it is resource safe and more useful
    @Deprecated
    protected AtomicInteger mockServerWithSpecificHttpResponse(int statusCode, String uri, int port, Consumer<Headers> assertion, byte[] body) throws IOException {
        if (port == 0) {
            port = applicationRegistry.findFreePort();
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        AtomicInteger counter = new AtomicInteger();
        server.createContext(uri, (t) -> {
            t.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
            t.sendResponseHeaders(statusCode, 0);

            t.getResponseBody().write(body);

            assertion.accept(t.getRequestHeaders());
            t.getResponseBody().close();

            counter.getAndIncrement();
        });
        server.setExecutor(null);
        server.start();
        return counter;
    }

    protected void updateRoutingRules() {
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent("List of services changed"));
    }

    protected MockService.MockServiceBuilder mockService(String serviceId) {
        return MockService.builder()
            .statusChangedlistener(mockService -> {
                List<ServiceInstance> allInstances = createdMockServices.stream()
                    .filter(ms -> StringUtils.equals(serviceId, ms.getServiceId()))
                    .map(MockService::getEurekaServiceInstance)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                createdMockServices.add(mockService);

                if (mockService.isStarted()) {
                    allInstances.add(mockService.getEurekaServiceInstance());
                    applicationRegistry.addApplication(mockService.getInstanceInfo());
                }
                doReturn(Mono.just(allInstances)).when(instanceInfoService).getServiceInstance(serviceId);

                updateRoutingRules();
            })
            .serviceId(serviceId);
    }

}
