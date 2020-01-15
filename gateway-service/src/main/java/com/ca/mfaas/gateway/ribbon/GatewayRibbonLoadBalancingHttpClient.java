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
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpRequest;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToSecureConnectionIfNeeded;

@Slf4j
public class GatewayRibbonLoadBalancingHttpClient extends RibbonLoadBalancingHttpClient {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    /**
     * Ribbon load balancer
     * @param secureHttpClient custom http client for our certificates
     * @param config configuration details
     * @param serverIntrospector introspector
     */
    public GatewayRibbonLoadBalancingHttpClient(CloseableHttpClient secureHttpClient, IClientConfig config, ServerIntrospector serverIntrospector) {
        super(secureHttpClient, config, serverIntrospector);
    }

    @Override
    public URI reconstructURIWithServer(Server server, URI original) {
        URI uriToSend;
        URI updatedURI = updateToSecureConnectionIfNeeded(original, this.config, this.serverIntrospector,
            server);
        final URI uriWithServer = super.reconstructURIWithServer(server, updatedURI);

        // if instance is not secure, override with http:
        DiscoveryEnabledServer discoveryEnabledServer = (DiscoveryEnabledServer) server;
        final InstanceInfo instanceInfo = discoveryEnabledServer.getInstanceInfo();
        if (instanceInfo.isPortEnabled(InstanceInfo.PortType.UNSECURE)) {
            log.debug("Resetting scheme to HTTP based on instance info of instance: " + instanceInfo.getId());
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uriWithServer).scheme(HTTP);
            uriToSend = uriComponentsBuilder.build(true).toUri();
        } else {
            uriToSend = uriWithServer;
            if (uriWithServer.getScheme().equalsIgnoreCase("http")) {
                uriToSend = UriComponentsBuilder.fromUri(uriWithServer).scheme(HTTPS).build(true).toUri();
            }
        }
        return uriToSend;
    }


    @Override
    public RibbonApacheHttpResponse execute(RibbonApacheHttpRequest request, IClientConfig configOverride) throws Exception {
        RibbonApacheHttpRequest sendRequest = null;
        if (HTTPS.equals(request.getURI().getScheme())) {
            configOverride.set(CommonClientConfigKey.IsSecure, true);
        } else {
            configOverride.set(CommonClientConfigKey.IsSecure, false);
        }
        final RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(configOverride.get(
            CommonClientConfigKey.ConnectTimeout, this.connectTimeout));
        builder.setSocketTimeout(configOverride.get(
            CommonClientConfigKey.ReadTimeout, this.readTimeout));
        builder.setRedirectsEnabled(configOverride.get(
            CommonClientConfigKey.FollowRedirects, this.followRedirects));
        builder.setContentCompressionEnabled(false);

        final RequestConfig requestConfig = builder.build();
        if (HTTPS.equals(request.getURI().getScheme())) {
            final URI secureUri = UriComponentsBuilder.fromUri(request.getUri())
                .scheme(HTTPS).build(true).toUri();
            sendRequest = request.withNewUri(secureUri);
        }
        if (sendRequest == null) {
            sendRequest = request;
        }
        final HttpUriRequest httpUriRequest = sendRequest.toRequest(requestConfig);
        final HttpResponse httpResponse = this.delegate.execute(httpUriRequest);
        return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
    }
}
