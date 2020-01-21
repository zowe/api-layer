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

import com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpRequest;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static com.ca.mfaas.gateway.security.service.ServiceAuthenticationServiceImpl.AUTHENTICATION_COMMAND_KEY;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToSecureConnectionIfNeeded;

@Slf4j
public class GatewayRibbonLoadBalancingHttpClient extends RibbonLoadBalancingHttpClient {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    private final EurekaClient discoveryClient;

    /**
     * Ribbon load balancer
     *
     * @param secureHttpClient   custom http client for our certificates
     * @param config             configuration details
     * @param serverIntrospector introspector
     */
    public GatewayRibbonLoadBalancingHttpClient(CloseableHttpClient secureHttpClient, IClientConfig config, ServerIntrospector serverIntrospector, EurekaClient discoveryClient) {
        super(secureHttpClient, config, serverIntrospector);
        this.discoveryClient = discoveryClient;
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
        configOverride.set(CommonClientConfigKey.IsSecure, HTTPS.equals(request.getURI().getScheme()));
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
                .scheme(HTTPS).build().toUri();
            sendRequest = request.withNewUri(secureUri);
        }
        if (sendRequest == null) {
            sendRequest = request;
        }
        final HttpUriRequest httpUriRequest = sendRequest.toRequest(requestConfig);
        final HttpResponse httpResponse = this.delegate.execute(httpUriRequest);
        return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
    }

    /**
     * This methods write specific InstanceInfo into cache
     *
     * @param serviceId    serviceId of instance
     * @param instanceId   ID of instance
     * @param instanceInfo instanceInfo to store
     * @return cached instanceInfo object
     */
    @CachePut(value = "instanceInfoByInstanceId", key = "{#serviceId, #instanceId}")
    public InstanceInfo putInstanceInfo(String serviceId, String instanceId, InstanceInfo instanceInfo) {
        return instanceInfo;
    }

    /**
     * Get the InstanceInfo by id. For searching is used serviceId. It method found another instances it will
     * cache them for next using
     *
     * @param serviceId  service to call
     * @param instanceId selected instance of service
     * @return instance with matching service and instanceId
     */
    @Cacheable("instanceInfoByInstanceId")
    public InstanceInfo getInstanceInfo(String serviceId, String instanceId) {
        InstanceInfo output = null;

        Application application = discoveryClient.getApplication(serviceId);
        if (application == null) return null;

        for (final InstanceInfo instanceInfo : application.getInstances()) {
            if (StringUtils.equals(instanceId, instanceInfo.getInstanceId())) {
                // found instance, store it for output
                output = instanceInfo;
            }

            /*
             * Getting all instance is pretty heavy, cache them, therefor it is very probably, nex using of service
             * will use different instance and need to find it too.
             */
            putInstanceInfo(serviceId, instanceInfo.getInstanceId(), instanceInfo);
        }
        return output;
    }

    @Override
    protected void customizeLoadBalancerCommandBuilder(RibbonApacheHttpRequest request, IClientConfig config, LoadBalancerCommand.Builder<RibbonApacheHttpResponse> builder) {
        super.customizeLoadBalancerCommandBuilder(request, config, builder);

        /*
         * add into builder listener to work with request immediately when instance if selected
         * it is helpful for selecting {@com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand} in
         * case there is multiple instances with same serviceId which have different authentication. Therefor it
         * is necessary wait for selection of instance to apply right authentication command.
         */
        builder.withListeners(Collections.singletonList(new ExecutionListener<Object, RibbonApacheHttpResponse>() {

            /**
             * This method update current request by added values on sending in load balancer. It is used at least
             * for service authentication.
             *
             * Now it updates only headers, but could be extended for another values in future.
             *
             * @param context
             */
            private void updateRequestByZuulChanges(ExecutionContext<Object> context) {
                final Map<String, String> newHeaders = RequestContext.getCurrentContext().getZuulRequestHeaders();
                if (!newHeaders.isEmpty()) {
                    final RibbonApacheHttpRequest req = (RibbonApacheHttpRequest) context.getRequest();
                    for (Map.Entry<String, String> entry : newHeaders.entrySet()) {
                        req.getContext().getHeaders().set(entry.getKey(), entry.getValue());
                    }
                }
            }

            @Override
            public void onExecutionStart(ExecutionContext<Object> context) {
                // dont needed yet
            }

            @Override
            public void onStartWithServer(ExecutionContext<Object> context, ExecutionInfo info) {
                final AuthenticationCommand cmd = (AuthenticationCommand) RequestContext.getCurrentContext().get(AUTHENTICATION_COMMAND_KEY);
                if (cmd != null) {
                    // in context is a command, it means update of authentication is waiting for select an instance
                    final Server.MetaInfo metaInfo = info.getServer().getMetaInfo();
                    final InstanceInfo instanceInfo = getInstanceInfo(metaInfo.getServiceIdForDiscovery(), metaInfo.getInstanceId());
                    try {
                        cmd.apply(instanceInfo);
                    } catch (Exception e) {
                        throw new AbortExecutionException(String.valueOf(e), e);
                    }
                    updateRequestByZuulChanges(context);
                }
            }

            @Override
            public void onExceptionWithServer(ExecutionContext<Object> context, Throwable exception, ExecutionInfo info) {
                // dont needed yet
            }

            @Override
            public void onExecutionSuccess(ExecutionContext<Object> context, RibbonApacheHttpResponse response, ExecutionInfo info) {
                // dont needed yet
            }

            @Override
            public void onExecutionFailed(ExecutionContext<Object> context, Throwable finalException, ExecutionInfo info) {
                // dont needed yet
            }
        }));
        builder.withExecutionContext(new ExecutionContext<Object>(request, config, config, null));
    }
}
