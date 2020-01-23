/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.ribbon;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


@Import(GatewayRibbonLoadBalancingHttpClientTest.TestConfiguration.class)
public class GatewayRibbonLoadBalancingHttpClientTest {

    private GatewayRibbonLoadBalancingHttpClient gatewayRibbonLoadBalancingHttpClient;
    private CloseableHttpClient closeableHttpClient;
    private IClientConfig iClientConfig;

    @Autowired
    private ServerIntrospector serverIntrospector;

    @Before
    public void setup() {
        closeableHttpClient = mock(CloseableHttpClient.class);
        iClientConfig = IClientConfig.Builder.newBuilder(DefaultClientConfigImpl.class, "apicatalog").withSecure(false).withFollowRedirects(false).withDeploymentContextBasedVipAddresses("apicatalog").withLoadBalancerEnabled(false).build();
        gatewayRibbonLoadBalancingHttpClient = new GatewayRibbonLoadBalancingHttpClient(closeableHttpClient, iClientConfig, serverIntrospector);
    }

    @Test
    public void shouldReconstructURIWithServerWhenUnsecurePortEnabled() throws URISyntaxException {
        HttpGet httpGet = mock(HttpGet.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        try {
            when(closeableHttpClient.execute(httpGet)).thenReturn(closeableHttpResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        URI request = new URI("/apicatalog/");

        Server server = createServer("localhost", 10014, false, true, "defaultZone");
        URI reconstructedURI = gatewayRibbonLoadBalancingHttpClient.reconstructURIWithServer(server, request);
        assertEquals("http://localhost:10014/apicatalog/", reconstructedURI.toString());
    }

    @Test
    public void shouldReconstructURIWithServerWhenSecurePortEnabled() throws URISyntaxException {
        HttpGet httpGet = mock(HttpGet.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        try {
            when(closeableHttpClient.execute(httpGet)).thenReturn(closeableHttpResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        URI request = new URI("/apicatalog/");

        Server server = createServer("localhost", 10014, true, false, "defaultZone");
        URI reconstructedURI = gatewayRibbonLoadBalancingHttpClient.reconstructURIWithServer(server, request);
        assertEquals("https://localhost:10014/apicatalog/", reconstructedURI.toString());
    }

    @Test
    public void shouldReconstructEncodedURIWithServer() throws URISyntaxException {
        HttpGet httpGet = mock(HttpGet.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        try {
            when(closeableHttpClient.execute(httpGet)).thenReturn(closeableHttpResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        URI request = new URI("/api%2fcatalog/");

        Server server = createServer("localhost", 10014, true, false, "defaultZone");
        URI reconstructedURI = gatewayRibbonLoadBalancingHttpClient.reconstructURIWithServer(server, request);
        assertEquals("https://localhost:10014/api%2fcatalog/", reconstructedURI.toString());
    }

    private DiscoveryEnabledServer createServer(String host, int port, boolean isSecureEnabled, boolean isUnsecureEnabled, String zone) {

        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
        InstanceInfo info = builder
            .setAppName("apicatalog")
            .setInstanceId("apicatalog")
            .setHostName(host)
            .setPort(port)
            .enablePort(InstanceInfo.PortType.SECURE, isSecureEnabled)
            .enablePort(InstanceInfo.PortType.UNSECURE, isUnsecureEnabled)
            .setSecurePort(port)
            .build();
        DiscoveryEnabledServer server = new DiscoveryEnabledServer(info, false, false);
        server.setZone(zone);
        return server;
    }

    @Configuration
    protected static class TestConfiguration {

        @Bean
        public DefaultServerIntrospector defaultServerIntrospector() {
            return new DefaultServerIntrospector();
        }

    }

}
