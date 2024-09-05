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
import com.netflix.discovery.provider.DiscoveryJerseyProvider;
import com.netflix.discovery.shared.transport.jersey3.EurekaIdentityHeaderFilter;
import com.netflix.discovery.shared.transport.jersey3.EurekaJersey3Client;
import com.netflix.discovery.shared.transport.jersey3.EurekaJersey3ClientImpl;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerIdentity;
import com.netflix.eureka.cluster.HttpReplicationClient;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.Jersey3DynamicGZIPContentEncodingFilter;
import com.netflix.eureka.transport.Jersey3ReplicationClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.CoreProtocolPNames;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Set;

import static com.netflix.discovery.util.DiscoveryBuildInfo.buildVersion;

@Slf4j
public class RefreshablePeerEurekaNodes extends PeerEurekaNodes
    implements ApplicationListener<EnvironmentChangeEvent> {

    private static final String USER_AGENT = "Java-EurekaClient-Replication";

    private Collection<ClientRequestFilter> replicationClientAdditionalFilters;
    private SSLContext secureSslContextWithoutKeystore;
    private int maxPeerRetries;

    public RefreshablePeerEurekaNodes(final PeerAwareInstanceRegistry registry,
                                      final EurekaServerConfig serverConfig,
                                      final EurekaClientConfig clientConfig, final ServerCodecs serverCodecs,
                                      final ApplicationInfoManager applicationInfoManager,
                                      final Collection<ClientRequestFilter> replicationClientAdditionalFilters,
                                      final @Qualifier("secureSslContextWithoutKeystore") SSLContext secureSslContextWithoutKeystore,
                                      final int maxPeerRetries) {
        super(registry, serverConfig, clientConfig, serverCodecs,
            applicationInfoManager);
        this.replicationClientAdditionalFilters = replicationClientAdditionalFilters;
        this.secureSslContextWithoutKeystore = secureSslContextWithoutKeystore;
        this.maxPeerRetries = maxPeerRetries;
    }

    @Override
    public PeerEurekaNode createPeerEurekaNode(String peerEurekaNodeUrl) {
        HttpReplicationClient replicationClient = createReplicationClient(serverConfig, serverCodecs, peerEurekaNodeUrl, replicationClientAdditionalFilters);


        String targetHost = hostFromUrl(peerEurekaNodeUrl);
        if (targetHost == null) {
            targetHost = "host";
        }
        return new ApimlPeerEurekaNode(registry, targetHost, peerEurekaNodeUrl, replicationClient, serverConfig, maxPeerRetries);
    }

    private Jersey3ReplicationClient createReplicationClient(EurekaServerConfig config,
                                                                    ServerCodecs serverCodecs, String serviceUrl, Collection<ClientRequestFilter> additionalFilters) {
        String name = Jersey3ReplicationClient.class.getSimpleName() + ": " + serviceUrl + "apps/: ";

        EurekaJersey3Client jerseyClient;
        try {
            String hostname;
            try {
                hostname = new URL(serviceUrl).getHost();
            } catch (MalformedURLException e) {
                hostname = serviceUrl;
            }

            String jerseyClientName = "Discovery-PeerNodeClient-" + hostname;
            var fullJsonCodec = serverCodecs.getFullJsonCodec();
            EurekaJersey3ClientImpl.EurekaJersey3ClientBuilder clientBuilder = new EurekaJersey3ClientImpl.EurekaJersey3ClientBuilder() {
                @Override
                public EurekaJersey3Client build() {
                    ClientConfig clientConfig = new ClientConfig() {
                        {

                            DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(fullJsonCodec, fullJsonCodec);
                            register(discoveryJerseyProvider);

                            // Common properties to all clients
                            ConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(secureSslContextWithoutKeystore, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                            Registry registry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", socketFactory).build();
                            var cm = new PoolingHttpClientConnectionManager(registry);


                            cm.setDefaultMaxPerRoute(config.getPeerNodeTotalConnectionsPerHost());
                            cm.setMaxTotal(config.getPeerNodeTotalConnections());
                            property(ApacheClientProperties.CONNECTION_MANAGER, cm);

                            String fullUserAgentName = USER_AGENT + "/v" + buildVersion();
                            property(CoreProtocolPNames.USER_AGENT, fullUserAgentName);

                            // To pin a client to specific server in case redirect happens, we handle redirects directly
                            // (see DiscoveryClient.makeRemoteCall methods).
                            property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE);
                            property(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
                        }
                    };
                    try {
                        return new EurekaJersey3ClientImpl(
                            config.getPeerNodeConnectTimeoutMs(),
                            config.getPeerNodeReadTimeoutMs(),
                            config.getPeerNodeConnectionIdleTimeoutSeconds(),
                            clientConfig);
                    } catch (Throwable e) {
                        throw new RuntimeException("Cannot create Jersey client ", e);
                    }
                }
            }
                .withClientName(jerseyClientName).withUserAgent(USER_AGENT)
                .withEncoderWrapper(fullJsonCodec)
                .withDecoderWrapper(fullJsonCodec)
                .withConnectionTimeout(config.getPeerNodeConnectTimeoutMs())
                .withReadTimeout(config.getPeerNodeReadTimeoutMs())
                .withMaxConnectionsPerHost(config.getPeerNodeTotalConnectionsPerHost())
                .withMaxTotalConnections(config.getPeerNodeTotalConnections())
                .withConnectionIdleTimeout(config.getPeerNodeConnectionIdleTimeoutSeconds());

            if (serviceUrl.startsWith("https://") && "true"
                .equals(System.getProperty("com.netflix.eureka.shouldSSLConnectionsUseSystemSocketFactory"))) {
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

        Client jerseyApacheClient = jerseyClient.getClient();
        jerseyApacheClient.register(new Jersey3DynamicGZIPContentEncodingFilter(config));

        for (ClientRequestFilter filter : additionalFilters) {
            jerseyApacheClient.register(filter);
        }

        EurekaServerIdentity identity = new EurekaServerIdentity(ip);
        jerseyApacheClient.register(new EurekaIdentityHeaderFilter(identity));

        return new Jersey3ReplicationClient(jerseyClient, serviceUrl);
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
    protected boolean shouldUpdate(final Set<String> changedKeys) {
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
