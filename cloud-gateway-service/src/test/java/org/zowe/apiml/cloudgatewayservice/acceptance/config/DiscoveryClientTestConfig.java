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
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MetadataBuilder;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.Service;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApplicationRegistry;

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

    protected Service serviceWithDefaultConfiguration = new Service("serviceid2", "/serviceid2/**", "serviceid2");
    protected Service serviceWithCustomConfiguration = new Service("serviceid1", "/serviceid1/**", "serviceid1");

    @Bean
    public ApplicationRegistry registry() {
        ApplicationRegistry applicationRegistry = new ApplicationRegistry();
        return applicationRegistry;
    }

    @Bean(destroyMethod = "shutdown", name = "test")
    @Primary
    @RefreshScope
    public ApimlDiscoveryClientStub eurekaClient(ApplicationInfoManager manager,
                                                 EurekaClientConfig config,
                                                 EurekaInstanceConfig instance,
                                                 @Autowired(required = false) HealthCheckHandler healthCheckHandler,
                                                 ApplicationRegistry applicationRegistry,
                                                 @Value("${currentApplication:}") String currentApplication
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }

        AbstractDiscoveryClientOptionalArgs<?> args = new MutableDiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClient(applicationRegistry, currentApplication));


        final ApimlDiscoveryClientStub discoveryClient = new ApimlDiscoveryClientStub(appManager, config, args, this.context, applicationRegistry);
        discoveryClient.registerHealthCheck(healthCheckHandler);

        discoveryClient.registerEventListener(event -> {
        });
        return discoveryClient;
    }

    private EurekaJerseyClient eurekaJerseyClient(ApplicationRegistry registry, String currentApplication) {
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

        registry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(), false);
        registry.addApplication(serviceWithCustomConfiguration, MetadataBuilder.customInstance(), false);
        registry.setCurrentApplication(currentApplication);
        when(response.getEntity(Applications.class)).thenReturn(registry.getApplications());

        return jerseyClient;
    }

}
