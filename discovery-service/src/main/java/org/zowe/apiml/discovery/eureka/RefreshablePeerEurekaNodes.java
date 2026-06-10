/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.EurekaIdentityHeaderFilter;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerIdentity;
import com.netflix.eureka.cluster.DynamicGZIPContentEncodingFilter;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.JerseyReplicationClient;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters;
import org.springframework.context.ApplicationListener;
import org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
public class RefreshablePeerEurekaNodes extends PeerEurekaNodes
    implements ApplicationListener<EnvironmentChangeEvent> {

    private Supplier<EurekaJerseyClientImpl.EurekaJerseyClientBuilder> eurekaJerseyClientBuilder;
    private ReplicationClientAdditionalFilters replicationClientAdditionalFilters;
    private int maxPeerRetries;

    public RefreshablePeerEurekaNodes(final Supplier<EurekaJerseyClientImpl.EurekaJerseyClientBuilder> eurekaJerseyClientBuilder,
                                      final PeerAwareInstanceRegistry registry,
                                      final EurekaServerConfig serverConfig,
                                      final EurekaClientConfig clientConfig, final ServerCodecs serverCodecs,
                                      final ApplicationInfoManager applicationInfoManager,
                                      final ReplicationClientAdditionalFilters replicationClientAdditionalFilters,
                                      final int maxPeerRetries) {
        super(registry, serverConfig, clientConfig, serverCodecs,
            applicationInfoManager);
        this.eurekaJerseyClientBuilder = eurekaJerseyClientBuilder;
        this.replicationClientAdditionalFilters = replicationClientAdditionalFilters;
        this.maxPeerRetries = maxPeerRetries;
    }

    private JerseyReplicationClient createReplicationClient(EurekaServerConfig config, ServerCodecs serverCodecs, String serviceUrl) {
        String name = JerseyReplicationClient.class.getSimpleName() + ": " + serviceUrl + "apps/: ";

        EurekaJerseyClient jerseyClient;
        try {
            String hostname;
            try {
                hostname = new URL(serviceUrl).getHost();
            } catch (MalformedURLException e) {
                hostname = serviceUrl;
            }

            String jerseyClientName = "Discovery-PeerNodeClient-" + hostname;
            EurekaJerseyClientImpl.EurekaJerseyClientBuilder clientBuilder = eurekaJerseyClientBuilder.get()
                .withClientName(jerseyClientName)
                .withUserAgent("Java-EurekaClient-Replication")
                .withEncoderWrapper(serverCodecs.getFullJsonCodec())
                .withDecoderWrapper(serverCodecs.getFullJsonCodec())
                .withConnectionTimeout(config.getPeerNodeConnectTimeoutMs())
                .withReadTimeout(config.getPeerNodeReadTimeoutMs())
                .withMaxConnectionsPerHost(config.getPeerNodeTotalConnectionsPerHost())
                .withMaxTotalConnections(config.getPeerNodeTotalConnections())
                .withConnectionIdleTimeout(config.getPeerNodeConnectionIdleTimeoutSeconds());

            if (serviceUrl.startsWith("https://") &&
                "true".equals(System.getProperty("com.netflix.eureka.shouldSSLConnectionsUseSystemSocketFactory"))) {
                clientBuilder.withSystemSSLConfiguration();
            }
            jerseyClient = clientBuilder.build();
        } catch (Throwable e) {
            throw new RuntimeException("Cannot Create new Replica Node :" + name, e);
        }

        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Cannot find localhost ip", e);
        }

        ApacheHttpClient4 jerseyApacheClient = jerseyClient.getClient();
        jerseyApacheClient.addFilter(new DynamicGZIPContentEncodingFilter(config));

        EurekaServerIdentity identity = new EurekaServerIdentity(ip);
        jerseyApacheClient.addFilter(new EurekaIdentityHeaderFilter(identity));

        return new JerseyReplicationClient(jerseyClient, serviceUrl);
    }

    @Override
    public PeerEurekaNode createPeerEurekaNode(String peerEurekaNodeUrl) {
        JerseyReplicationClient replicationClient = createReplicationClient(serverConfig, serverCodecs, peerEurekaNodeUrl);

        this.replicationClientAdditionalFilters.getFilters()
            .forEach(replicationClient::addReplicationClientFilter);

        String targetHost = hostFromUrl(peerEurekaNodeUrl);
        if (targetHost == null) {
            targetHost = "host";
        }
        return new ApimlPeerEurekaNode(registry, targetHost, peerEurekaNodeUrl,
            replicationClient, serverConfig, maxPeerRetries);
    }

    @Override
    public void onApplicationEvent(final EnvironmentChangeEvent event) {
        if (shouldUpdate(event.getKeys())) {
            updatePeerEurekaNodes(resolvePeerUrls());
        }
    }

    /*
     * Check whether specific properties have changed.
     */
    public boolean shouldUpdate(final Set<String> changedKeys) {
        assert changedKeys != null;

        // if eureka.client.use-dns-for-fetching-service-urls is true, then
        // service-url will not be fetched from environment.
        if (this.clientConfig.shouldUseDnsForFetchingServiceUrls()) {
            return false;
        }

        if (changedKeys.contains("eureka.client.region")) {
            return true;
        }

        for (final String key : changedKeys) {
            // property keys are not expected to be null.
            if (key.startsWith("eureka.client.service-url.")
                || key.startsWith("eureka.client.availability-zones.")) {
                return true;
            }
        }
        return false;
    }

}
