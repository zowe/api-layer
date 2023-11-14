/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApplicationRegistry;
import reactor.core.publisher.Flux;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.*;

/**
 * This configuration provides the bean for the ApplicationRegistry and overrides bean CloudEurekaClient with custom ApimlDiscoveryClient. This bean mocks Eureka Client to allow virtual services registration.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speeds up distribution of
 * changes in whole cloud gateway.
 */
@TestConfiguration
@RequiredArgsConstructor
public class DiscoveryClientTestConfig {

    private final ApplicationContext context;

    @Bean
    public ApplicationRegistry registry() {
        return new ApplicationRegistry();
    }

    @Bean
    public ReactiveDiscoveryClient mockServicesReactiveDiscoveryClient(ApplicationRegistry applicationRegistry) {
        return new ReactiveDiscoveryClient() {

            @Override
            public String description() {
                return "mocked services";
            }

            @Override
            public Flux<ServiceInstance> getInstances(String serviceId) {
                return Flux.just(applicationRegistry.getServiceInstance(serviceId).toArray(new ServiceInstance[0]));
            }

            @Override
            public Flux<String> getServices() {
                return Flux.just(applicationRegistry.getInstances().stream()
                    .map(a -> a.getId())
                    .distinct()
                    .toArray(String[]::new));
            }
        };
    }

    @Bean(destroyMethod = "shutdown", name = "test")
    @Primary
    @RefreshScope
    public ApimlDiscoveryClientStub eurekaClient(ApplicationInfoManager manager,
                                                 EurekaClientConfig config,
                                                 EurekaInstanceConfig instance,
                                                 @Autowired(required = false) HealthCheckHandler healthCheckHandler,
                                                 ApplicationRegistry applicationRegistry
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }

        AbstractDiscoveryClientOptionalArgs<?> args = new MutableDiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClient());


        final ApimlDiscoveryClientStub discoveryClient = new ApimlDiscoveryClientStub(appManager, config, args, this.context, applicationRegistry);
        discoveryClient.registerHealthCheck(healthCheckHandler);

        discoveryClient.registerEventListener(event -> {
        });
        return discoveryClient;
    }

    private EurekaJerseyClient eurekaJerseyClient() {
        EurekaJerseyClient jerseyClient = mock(EurekaJerseyClient.class);
        ApacheHttpClient4 httpClient4 = mock(ApacheHttpClient4.class);
        when(jerseyClient.getClient()).thenReturn(httpClient4);
        WebResource webResource = mock(WebResource.class);
        when(httpClient4.resource((String) any())).thenReturn(webResource);
        when(webResource.path(any())).thenReturn(webResource);
        WebResource.Builder builder = mock(WebResource.Builder.class);
        when(webResource.getRequestBuilder()).thenReturn(builder);
        when(builder.accept(MediaType.APPLICATION_JSON_TYPE)).thenReturn(builder);
        ClientResponse response = mock(ClientResponse.class);
        when(builder.get(ClientResponse.class)).thenReturn(response);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.hasEntity()).thenReturn(true);
        return jerseyClient;
    }

}
