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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Set;

import static org.zowe.apiml.security.SecurityUtils.loadKeyStore;

@Slf4j
public class RefreshablePeerEurekaNodes extends PeerEurekaNodes
    implements ApplicationListener<EnvironmentChangeEvent> {

    private Collection<ClientRequestFilter> replicationClientAdditionalFilters;
    private int maxPeerRetries;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStorePath;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    public RefreshablePeerEurekaNodes(final PeerAwareInstanceRegistry registry,
                                      final EurekaServerConfig serverConfig,
                                      final EurekaClientConfig clientConfig, final ServerCodecs serverCodecs,
                                      final ApplicationInfoManager applicationInfoManager,
                                      final Collection<ClientRequestFilter> replicationClientAdditionalFilters,
                                      final int maxPeerRetries) {
        super(registry, serverConfig, clientConfig, serverCodecs,
            applicationInfoManager);
        this.replicationClientAdditionalFilters = replicationClientAdditionalFilters;
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

    private void updateTrustStore(EurekaJersey3Client jerseyClient) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, KeyManagementException {
        if (StringUtils.isNotBlank(trustStoreType)) {
            var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = loadKeyStore(trustStoreType, trustStorePath, trustStorePassword);
            tmf.init(trustStore);
            jerseyClient.getClient().getSslContext().init(null, tmf.getTrustManagers(), new SecureRandom());
        } else {
            log.warn("Truststore is not configured");
        }
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
            EurekaJersey3ClientImpl.EurekaJersey3ClientBuilder clientBuilder = new EurekaJersey3ClientImpl.EurekaJersey3ClientBuilder()
                .withClientName(jerseyClientName).withUserAgent("Java-EurekaClient-Replication")
                .withEncoderWrapper(serverCodecs.getFullJsonCodec())
                .withDecoderWrapper(serverCodecs.getFullJsonCodec())
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
            updateTrustStore(jerseyClient);
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
