/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.JerseyReplicationClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.discovery.ApimlPeerEurekaNode;

import java.util.Set;

/**
 * Configuration to rewrite default Eureka's implementation with custom one
 */
@Configuration
public class EurekaConfig {

    @Value("${apiml.discovery.serviceIdPrefixReplacer:#{null}}")
    private String tuple;

    @Bean
    @Primary
    public ApimlInstanceRegistry getApimlInstanceRegistry(
        EurekaServerConfig serverConfig,
        EurekaClientConfig clientConfig,
        ServerCodecs serverCodecs,
        EurekaClient eurekaClient,
        InstanceRegistryProperties instanceRegistryProperties,
        ApplicationContext appCntx) {
        eurekaClient.getApplications(); // force initialization
        return new ApimlInstanceRegistry(serverConfig, clientConfig, serverCodecs, eurekaClient, instanceRegistryProperties, appCntx, new Tuple(tuple));
    }

    @Bean
    @Primary
    public PeerEurekaNodes peerEurekaNodes(PeerAwareInstanceRegistry registry,
                                           ServerCodecs serverCodecs,
                                           ReplicationClientAdditionalFilters replicationClientAdditionalFilters, ApplicationInfoManager applicationInfoManager, EurekaServerConfig eurekaServerConfig, EurekaClientConfig eurekaClientConfig) {
        return new RefreshablePeerEurekaNodes(registry, eurekaServerConfig,
            eurekaClientConfig, serverCodecs, applicationInfoManager,
            replicationClientAdditionalFilters);
    }

    /**
     * {@link PeerEurekaNodes} which updates peers when /refresh is invoked. Peers are
     * updated only if <code>eureka.client.use-dns-for-fetching-service-urls</code> is
     * <code>false</code> and one of following properties have changed.
     * <p>
     * </p>
     * <ul>
     * <li><code>eureka.client.availability-zones</code></li>
     * <li><code>eureka.client.region</code></li>
     * <li><code>eureka.client.service-url.&lt;zone&gt;</code></li>
     * </ul>
     */
    static class RefreshablePeerEurekaNodes extends PeerEurekaNodes
        implements ApplicationListener<EnvironmentChangeEvent> {

        private ReplicationClientAdditionalFilters replicationClientAdditionalFilters;

        RefreshablePeerEurekaNodes(final PeerAwareInstanceRegistry registry,
                                   final EurekaServerConfig serverConfig,
                                   final EurekaClientConfig clientConfig, final ServerCodecs serverCodecs,
                                   final ApplicationInfoManager applicationInfoManager,
                                   final ReplicationClientAdditionalFilters replicationClientAdditionalFilters) {
            super(registry, serverConfig, clientConfig, serverCodecs,
                applicationInfoManager);
            this.replicationClientAdditionalFilters = replicationClientAdditionalFilters;
        }

        @Override
        public PeerEurekaNode createPeerEurekaNode(String peerEurekaNodeUrl) {
            JerseyReplicationClient replicationClient = JerseyReplicationClient
                .createReplicationClient(serverConfig, serverCodecs,
                    peerEurekaNodeUrl);

            this.replicationClientAdditionalFilters.getFilters()
                .forEach(replicationClient::addReplicationClientFilter);

            String targetHost = hostFromUrl(peerEurekaNodeUrl);
            if (targetHost == null) {
                targetHost = "host";
            }
            return new ApimlPeerEurekaNode(registry, targetHost, peerEurekaNodeUrl,
                replicationClient, serverConfig);
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

    public static class Tuple {

        boolean valid;
        String oldPrefix;
        String newPrefix;

        public Tuple(String tuple) {
            if (isValidTuple(tuple)) {
                String[] prefixes = tuple.split(",");
                this.oldPrefix = prefixes[0];
                this.newPrefix = prefixes[1];
                this.valid = true;
            }
        }

        public boolean isValid() {
            return valid;
        }

        public String getOldPrefix() {
            return oldPrefix;
        }

        public String getNewPrefix() {
            return newPrefix;
        }

        public static boolean isValidTuple(String tuple) {
            if (StringUtils.isNotEmpty(tuple)) {
                String[] replacer = tuple.split(",");
                return replacer.length > 1 &&
                    StringUtils.isNotEmpty(replacer[0]) &&
                    StringUtils.isNotEmpty(replacer[1]) &&
                    !replacer[0].equals(replacer[1]);
            }
            return false;
        }

    }

}
