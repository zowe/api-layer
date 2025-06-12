/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.acceptance.common.Service;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import java.util.HashMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

@TestConfiguration
public class GatewayOverrideConfig {

    protected static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";

    @Value("${test.proxyAddress}")
    String proxyAddress;

    @Bean
    @Primary
    public ServiceRouteMapper serviceRouteMapper() {
        return new SimpleServiceRouteMapper();
    }

    @MockBean
    @Qualifier("mockProxy")
    public CloseableHttpClient mockProxy;

    @MockBean
    public ZosmfService zosmfService;

    @Bean
    @Qualifier("restTemplateWithoutKeystore")
    public RestTemplate restTemplateWithoutKeystore() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ResponseEntity<ZosmfService.ZosmfInfo> info = mock(ResponseEntity.class);
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(new org.springframework.http.HttpEntity<>(headers)), eq(ZosmfService.ZosmfInfo.class))).thenReturn(info);
        when(info.getStatusCode()).thenReturn(HttpStatus.OK);
        return restTemplate;
    }

    @Bean
    public SimpleRouteLocator simpleRouteLocator() {
        ZuulProperties properties = new ZuulProperties();
        properties.setRoutes(new HashMap<>());

        return new SimpleRouteLocator("", properties);
    }

    @Bean
    public ApplicationRegistry registry() {
        ApplicationRegistry applicationRegistry = new ApplicationRegistry();

        MetadataBuilder zosmfMetadataBuilder = MetadataBuilder.defaultInstance().withZosmf();
        Service zosmfService = new Service("zosmf", "/zosmf/**", "zosmf");
        applicationRegistry.addApplication(zosmfService, zosmfMetadataBuilder, false);

        applicationRegistry.setCurrentApplication("zosmf");
        return applicationRegistry;
    }

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }

    //For X-forwarded headers proxy tests
    @Bean
    @Profile("forward-headers-proxy-test")
    public ZuulFilter remoteAddressModifier() {
        return new ZuulFilter() {
            @Override
            public String filterType() {
                return PRE_TYPE;
            }

            @Override
            public int filterOrder() {
                return PRE_DECORATION_FILTER_ORDER - 1;
            }

            @Override
            public boolean shouldFilter() {
                return true;
            }

            @Override
            public Object run() {
                RequestContext ctx = RequestContext.getCurrentContext();

                ctx.setRequest(
                    new HttpServletRequestWrapper(ctx.getRequest()) {
                        @Override
                        public String getRemoteAddr() { return proxyAddress; }
                    }
                );
                return null;
            }
        };
    }
}
