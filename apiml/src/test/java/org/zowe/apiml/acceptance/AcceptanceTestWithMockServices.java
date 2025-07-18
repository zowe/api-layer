/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.zowe.apiml.gateway.ApplicationRegistry;
import org.zowe.apiml.gateway.MockService;

@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AcceptanceTestWithMockServices extends AcceptanceTestWithBasePath {

    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    @Qualifier("applicationRegistry")
    protected ApplicationRegistry applicationRegistry;

    @BeforeEach
    void resetCounters() {
        applicationRegistry.getMockServices().forEach(MockService::resetCounter);
    }

    @AfterEach
    void checkAssertionErrorsOnMockServices() {
        MockService.checkAssertionErrors();
    }

    protected void updateRoutingRules() {
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent("List of services changed"));
    }

    /**
     * Create mock service. It will be automatically registered and removed on the time. It is not necessary to handle
     * its lifecycle.
     *
     * Example:
     *
     * MockService myService;
     *
     * @BeforeAll
     * void createMyService() {
     *     myService = mockService("myservice").scope(MockService.Scope.CLASS)
     *          .addEndpoint("/test/500")
     *              .responseCode(500)
     *              .bodyJson("{}")
     *          .and().start();
     * }
     *
     * @param serviceId serviceId of the new service
     * @return builder to define a new MockService
     */
    protected MockService.MockServiceBuilder mockService(String serviceId) {
        return MockService.builder()
            .statusChangedlistener(mockService -> {
                applicationRegistry.update(mockService);
                updateRoutingRules();
            })
            .serviceId(serviceId);
    }

    @AfterEach
    void stopMocksWithTestScope() {
        applicationRegistry.afterTest();
    }

    @AfterAll
    void stopMocksWithClassScope() {
        applicationRegistry.afterClass();
    }

    protected void mockZosmfSuccess() throws JsonProcessingException {
        var headers = new Headers();
        headers.add("Set-Cookie", "jwtToken=jwt");
        headers.add("Set-Cookie", "LtpaToken2=ltpatoken");

        mockService("zosmf").scope(MockService.Scope.TEST)
            .addEndpoint("/zosmf/info")
                .responseCode(200)
                .contentType("application/json")
                .headers(headers)
                .bodyJson("{\"zosmf_version\":\"29\",\"zosmf_saf_realm\":\"SAFRealm\",\"zosmf_full_version\":\"29.0\"}")
        .and()
            .start();
    }

}
