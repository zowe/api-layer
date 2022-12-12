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

import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UrlPathHelper;
import org.zowe.apiml.acceptance.common.Service;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;

import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@Slf4j
@TestConfiguration
public class GatewayOverrideConfig {
    protected static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";

    @Bean
    @Primary
    public ServiceRouteMapper serviceRouteMapper() {
        return new SimpleServiceRouteMapper();
    }

    @Autowired
    protected ServerProperties server;
    @Autowired
    protected ZuulProperties zuulProperties;

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
    public ApplicationRegistry registry() {

        MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
        defaultBuilder.withZosmf();
        ApplicationRegistry applicationRegistry = new ApplicationRegistry();
        Service zosmfService = new Service("zosmf", "/zosmf/**", "zosmf");
        applicationRegistry.addApplication(zosmfService, defaultBuilder, false);
        applicationRegistry.setCurrentApplication("zosmf");
        return applicationRegistry;
    }

    @Bean
    @ConditionalOnMissingBean(PreDecorationFilter.class)
    public PreDecorationFilter preDecorationFilter(RouteLocator routeLocator,
                                                   ProxyRequestHelper proxyRequestHelper) {
        return new ApimlPredecorationFilter(routeLocator,
            this.server.getServlet().getContextPath(), this.zuulProperties,
            proxyRequestHelper);
    }


    class ApimlPredecorationFilter extends PreDecorationFilter {
        private UrlPathHelper urlPathHelper = new UrlPathHelper();

        public ApimlPredecorationFilter(RouteLocator routeLocator, String dispatcherServletPath, ZuulProperties properties, ProxyRequestHelper proxyRequestHelper) {
            super(routeLocator, dispatcherServletPath, properties, proxyRequestHelper);
        }

        @Override
        public boolean shouldFilter() {
            RequestContext ctx = RequestContext.getCurrentContext();
            for(String s : ctx.keySet()) {
                log.error("context key: " + s);
            }
            boolean sr = !ctx.containsKey(FORWARD_TO_KEY) // a filter has already forwarded
                && !ctx.containsKey(SERVICE_ID_KEY);
            log.error(FORWARD_TO_KEY + ctx.get(FORWARD_TO_KEY) + SERVICE_ID_KEY + ctx.get(SERVICE_ID_KEY));
            log.error("should run" + sr);
            return sr;// a filter has already determined
            // serviceId
        }

        @Override
        public Object run() {
            RequestContext ctx = RequestContext.getCurrentContext();
            final String requestURI = this.urlPathHelper
                .getPathWithinApplication(ctx.getRequest());
            log.error("running filter on uri" + requestURI);
            return super.run();
        }
    }
}
