/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.loadbalancer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.ApimlConstants.X_INSTANCEID;

class DeterministicLoadBalancerTest {

    ServiceInstance serviceInstance1 = new DefaultServiceInstance("instance1", "service1", "localhost", 10010, true);
    ServiceInstance serviceInstance2 = new DefaultServiceInstance("instance2", "service1", "localhost", 10010, true);
    ServiceInstance service2Instance2 = new DefaultServiceInstance("instance2", "service2", "localhost", 10010, true);

    LoadBalancerClientsProperties properties = new LoadBalancerClientsProperties();
    LoadBalancerClientFactory factory = new LoadBalancerClientFactory(properties);
    RequestData requestData = mock(RequestData.class);

    @Nested
    class GivenListOfServices {
        ServiceInstanceListSupplier delegate = new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just();
            }

            @Override
            public Flux<List<ServiceInstance>> get(Request request) {
                return Flux.just(Arrays.asList(serviceInstance1, serviceInstance2, service2Instance2));
            }
        };

        @Test
        void givenExistingInstanceId_thenReturnCorrectInstance() {
            var loadBalancer = new DeterministicLoadBalancer(delegate, factory);

            var headers = new HttpHeaders();
            headers.add(X_INSTANCEID, "instance1");
            when(requestData.getHeaders()).thenReturn(headers);
            var context = new RequestDataContext(requestData);

            var request = new DefaultRequest<>(context);
            StepVerifier.create(loadBalancer.get(request)).assertNext((services) -> assertEquals("instance1", services.get(0).getInstanceId())).expectComplete().verify();
        }

        @Test
        void givenIncorrectInstanceId_thenThrowException() {
            var loadBalancer = new DeterministicLoadBalancer(delegate, factory);

            var headers = new HttpHeaders();
            headers.add(X_INSTANCEID, "wrongInstanceId");
            when(requestData.getHeaders()).thenReturn(headers);
            var context = new RequestDataContext(requestData);

            var request = new DefaultRequest<>(context);
            StepVerifier.create(loadBalancer.get(request)).consumeErrorWith((throwable) -> {
                assertInstanceOf(ResponseStatusException.class, throwable);
                assertTrue(throwable.getMessage().startsWith("404 NOT_FOUND"));
            }).verify();
        }

        @Test
        void givenMissingHeader_thenThrowException() {
            var loadBalancer = new DeterministicLoadBalancer(delegate, factory);

            var headers = new HttpHeaders();
            when(requestData.getHeaders()).thenReturn(headers);
            var context = new RequestDataContext(requestData);

            var request = new DefaultRequest<>(context);
            StepVerifier.create(loadBalancer.get(request)).consumeErrorWith((throwable) -> {
                assertInstanceOf(ResponseStatusException.class, throwable);
                assertTrue(throwable.getMessage().startsWith("404 NOT_FOUND"));
            }).verify();
        }
    }

    ;
}
