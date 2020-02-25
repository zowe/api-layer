package org.zowe.apiml.gateway.ribbon;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.AuthenticationException;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.ExecutionContext;
import com.netflix.loadbalancer.reactive.ExecutionInfo;
import com.netflix.loadbalancer.reactive.ExecutionListener;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import lombok.Getter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpRequest;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl.AUTHENTICATION_COMMAND_KEY;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    GatewayRibbonLoadBalancingHttpClientImplTest.Context.class,
    CacheConfig.class
})
public class GatewayRibbonLoadBalancingHttpClientImplTest {

    @Autowired
    private GatewayRibbonLoadBalancingHttpClientTest bean;

    @Autowired
    private EurekaClient discoveryClient;

    @Autowired
    private IClientConfig config;

    private InstanceInfo createInstanceInfo(String instanceId) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getInstanceId()).thenReturn(instanceId);
        return out;
    }

    private Application createApplication(InstanceInfo...instanceInfos) {
        Application out = mock(Application.class);
        when(out.getInstances()).thenReturn(Arrays.asList(instanceInfos));
        return out;
    }

    @Test
    public void testCache() {
        InstanceInfo ii = mock(InstanceInfo.class);
        bean.putInstanceInfo("service1", "host:service1:123", ii);

        verify(discoveryClient, never()).getApplications(any());
        assertSame(ii, bean.getInstanceInfo("service1", "host:service1:123"));

        InstanceInfo ii1 = createInstanceInfo("host:service2:1");
        InstanceInfo ii2 = createInstanceInfo("host:service2:2");
        Application application = createApplication(ii1, ii2);
        when(discoveryClient.getApplication("service2")).thenReturn(application);
        assertSame(ii1, bean.getInstanceInfo("service2", "host:service2:1"));
        assertSame(ii2, bean.getInstanceInfo("service2", "host:service2:2"));
        verify(discoveryClient, times(1)).getApplication("service2");

        bean.evictCacheService("service2");

        assertSame(ii, bean.getInstanceInfo("service1", "host:service1:123"));
        verify(discoveryClient, times(1)).getApplication("service2");

        assertSame(ii2, bean.getInstanceInfo("service2", "host:service2:2"));
        verify(discoveryClient, times(2)).getApplication("service2");
    }

    private ExecutionListener<Object, RibbonApacheHttpResponse> getListener() {
        BuilderHolder builderHolder = new BuilderHolder();
        bean.customizeLoadBalancerCommandBuilder(mock(RibbonApacheHttpRequest.class), config, builderHolder.getBuilder());
        return builderHolder.getListener();
    }

    @Test
    public void testOnStartWithServer() {
        // base configuration about ZUUL
        ExecutionListener<Object, RibbonApacheHttpResponse> listener = getListener();
        RequestContext requestContext = new RequestContext();
        RequestContext.testSetCurrentContext(requestContext);
        ExecutionContext<Object> context = mock(ExecutionContext.class);
        ExecutionInfo info = mock(ExecutionInfo.class);

        // test without command
        listener.onStartWithServer(context, info);
        verify(info, never()).getServer();

        // configuration for deeply test with commands
        Server server = mock(Server.class);
        Server.MetaInfo metaInfo = mock(Server.MetaInfo.class);
        when(metaInfo.getServiceIdForDiscovery()).thenReturn("service3");
        when(metaInfo.getInstanceId()).thenReturn("host:service3:1");
        when(server.getMetaInfo()).thenReturn(metaInfo);
        when(info.getServer()).thenReturn(server);
        InstanceInfo ii1 = createInstanceInfo("host:service3:1");
        InstanceInfo ii2 = createInstanceInfo("host:service3:2");
        Application application = createApplication(ii1, ii2);
        when(discoveryClient.getApplication("service3")).thenReturn(application);
        RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
        RibbonCommandContext ribbonCommandContext = mock(RibbonCommandContext.class);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        when(ribbonCommandContext.getHeaders()).thenReturn(headers);
        when(request.getContext()).thenReturn(ribbonCommandContext);
        when(context.getRequest()).thenReturn(request);

        // test command without any action
        requestContext.put(AUTHENTICATION_COMMAND_KEY, AuthenticationCommand.EMPTY);
        listener.onStartWithServer(context, info);
        assertTrue(headers.isEmpty());

        // test command which throw exception
        requestContext.put(AUTHENTICATION_COMMAND_KEY, new TestAuthenticationCommand2());
        try {
            listener.onStartWithServer(context, info);
            fail();
        } catch (ExecutionListener.AbortExecutionException e) {
            assertTrue(e.getCause() instanceof AuthenticationException);
            assertEquals("Test exception", e.getCause().getMessage());
        }

        // test command with header set
        requestContext.put(AUTHENTICATION_COMMAND_KEY, new TestAuthenticationCommand());
        listener.onStartWithServer(context, info);
        assertEquals(2, headers.size());
        assertNotNull(headers.get("testkey"));
        assertEquals(1, headers.get("testkey").size());
        assertEquals("testValue", headers.get("testkey").get(0));
        assertNotNull(headers.get("testinstanceid"));
        assertEquals(1, headers.get("testinstanceid").size());
        assertEquals("host:service3:1", headers.get("testinstanceid").get(0));
        verify(discoveryClient, times(1)).getApplication("service3");
        bean.getInstanceInfo("service3", "host:service3:1");
        bean.getInstanceInfo("service3", "host:service3:2");
        verify(discoveryClient, times(1)).getApplication("service3");
    }

    @Test
    public void shouldReconstructURIWithServer_WhenUnsecurePortEnabled() throws URISyntaxException {
        URI request = new URI("/apicatalog/");

        Server server = createServer("localhost", 10014, false, true, "defaultZone");
        URI reconstructedURI = bean.reconstructURIWithServer(server, request);
        assertEquals("http://localhost:10014/apicatalog/", reconstructedURI.toString(),"URI is not same with expected");
    }

    @Test
    public void shouldReconstructURIWithServer_WhenSecurePortEnabled() throws URISyntaxException {
        URI request = new URI("/apicatalog/");

        Server server = createServer("localhost", 10014, true, false, "defaultZone");
        URI reconstructedURI = bean.reconstructURIWithServer(server, request);
        assertEquals("https://localhost:10014/apicatalog/", reconstructedURI.toString(),"URI is not same with expected");
    }

    private DiscoveryEnabledServer createServer(String host,
                                                int port,
                                                boolean isSecureEnabled,
                                                boolean isUnsecureEnabled,
                                                String zone) {
        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
        InstanceInfo info = builder
            .setAppName("apicatalog")
            .setInstanceId("apicatalog")
            .setVIPAddress("apicatalog")
            .setHostName(host)
            .setPort(port)
            .enablePort(InstanceInfo.PortType.SECURE, isSecureEnabled)
            .enablePort(InstanceInfo.PortType.UNSECURE, isUnsecureEnabled)
            .setSecurePort(port)
            .build();
        DiscoveryEnabledServer server = new DiscoveryEnabledServer(info, false, false);
        server.setZone(zone);

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(Collections.singletonList(info));
        when(discoveryClient.getApplication("apicatalog")).thenReturn(application);

        return server;
    }

    class TestAuthenticationCommand extends AuthenticationCommand {

        private static final long serialVersionUID = -2888552250075042413L;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            RequestContext.getCurrentContext().addZuulRequestHeader("testKey", "testValue");
            RequestContext.getCurrentContext().addZuulRequestHeader("testInstanceId", instanceInfo.getInstanceId());
        }

        @Override
        public boolean isExpired() {
            return false;
        }

    }

    class TestAuthenticationCommand2 extends AuthenticationCommand {

        private static final long serialVersionUID = 3708128389148113917L;

        @Override
        public void apply(InstanceInfo instanceInfo) throws AuthenticationException {
            throw new AuthenticationException("Test exception", null);
        }

        @Override
        public boolean isExpired() {
            return false;
        }

    }

    @Getter
    class BuilderHolder {

        private ExecutionListener<Object, RibbonApacheHttpResponse> listener;

        public LoadBalancerCommand.Builder<RibbonApacheHttpResponse> getBuilder() {
            LoadBalancerCommand.Builder<RibbonApacheHttpResponse> builder = mock(LoadBalancerCommand.Builder.class);
            doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    List<ExecutionListener<Object, RibbonApacheHttpResponse>> listeners = invocation.getArgument(0);
                    assertNotNull(listeners);
                    assertEquals(1, listeners.size());
                    listener = listeners.get(0);
                    return null;
                }
            }).when(builder).withListeners(any());
            return builder;
        }

    }

    @Configuration
    public static class Context {

        @Bean
        public EurekaClient getDiscoveryClient() {
            return mock(EurekaClient.class);
        }

        @Bean
        public CloseableHttpClient getSecureHttpClient() {
            return mock(CloseableHttpClient.class);
        }

        @Bean
        public IClientConfig getConfig() {
            return  IClientConfig.Builder.newBuilder(DefaultClientConfigImpl.class, "apicatalog")
                .withSecure(false)
                .withFollowRedirects(false)
                .withDeploymentContextBasedVipAddresses("apicatalog")
                .withLoadBalancerEnabled(false)
                .build();
        }

        @Bean
        public ServerIntrospector getServerIntrospector() {
            return mock(ServerIntrospector.class);
        }

        @Bean
        public GatewayRibbonLoadBalancingHttpClientTest ribbonLoadBalancingHttpClient(
            CloseableHttpClient secureHttpClient,
            IClientConfig config,
            ServerIntrospector serverIntrospector,
            EurekaClient discoveryClient,
            CacheManager cacheManager,
            ApplicationContext applicationContext
        ) {
            return new GatewayRibbonLoadBalancingHttpClientImplTestBean(secureHttpClient, config, serverIntrospector, discoveryClient, cacheManager, applicationContext);
        }

    }

    public interface GatewayRibbonLoadBalancingHttpClientTest extends GatewayRibbonLoadBalancingHttpClient {

        public void customizeLoadBalancerCommandBuilder(RibbonApacheHttpRequest request, IClientConfig config, LoadBalancerCommand.Builder<RibbonApacheHttpResponse> builder);

    }

    public static class GatewayRibbonLoadBalancingHttpClientImplTestBean extends GatewayRibbonLoadBalancingHttpClientImpl implements GatewayRibbonLoadBalancingHttpClientTest {

        /**
         * Ribbon load balancer
         *
         * @param secureHttpClient   custom http client for our certificates
         * @param config             configuration details
         * @param serverIntrospector introspector
         * @param discoveryClient    using discovery client
         */
        public GatewayRibbonLoadBalancingHttpClientImplTestBean(CloseableHttpClient secureHttpClient, IClientConfig config, ServerIntrospector serverIntrospector, EurekaClient discoveryClient, CacheManager cacheManager, ApplicationContext applicationContext) {
            super(secureHttpClient, config, serverIntrospector, discoveryClient, cacheManager, applicationContext);
        }

        @Override
        public void customizeLoadBalancerCommandBuilder(RibbonApacheHttpRequest request, IClientConfig config, LoadBalancerCommand.Builder<RibbonApacheHttpResponse> builder) {
            super.customizeLoadBalancerCommandBuilder(request, config, builder);
        }
    }

}
