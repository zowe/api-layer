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
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class GatewayRibbonLoadBalancingHttpClientTest {

    private GatewayRibbonLoadBalancingHttpClient gatewayRibbonLoadBalancingHttpClient;

    @Before
    public void setup() {
        IClientConfig iClientConfig = IClientConfig.Builder.newBuilder(DefaultClientConfigImpl.class, "apicatalog")
            .withSecure(false)
            .withFollowRedirects(false)
            .withDeploymentContextBasedVipAddresses("apicatalog")
            .withLoadBalancerEnabled(false)
            .build();

        gatewayRibbonLoadBalancingHttpClient = new GatewayRibbonLoadBalancingHttpClient(
            null, iClientConfig, null);
    }

    @Test
    public void shouldReconstructURIWithServer_WhenUnsecurePortEnabled() throws URISyntaxException {
        URI request = new URI("/apicatalog/");

        Server server = createServer("localhost", 10014, false, true, "defaultZone");
        URI reconstructedURI = gatewayRibbonLoadBalancingHttpClient.reconstructURIWithServer(server, request);
        assertEquals("URI is not same with expected", "http://localhost:10014/apicatalog/", reconstructedURI.toString());
    }

    @Test
    public void shouldReconstructURIWithServer_WhenSecurePortEnabled() throws URISyntaxException {
        URI request = new URI("/apicatalog/");

        Server server = createServer("localhost", 10014, true, false, "defaultZone");
        URI reconstructedURI = gatewayRibbonLoadBalancingHttpClient.reconstructURIWithServer(server, request);
        assertEquals("URI is not same with expected", "https://localhost:10014/apicatalog/", reconstructedURI.toString());
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
}
