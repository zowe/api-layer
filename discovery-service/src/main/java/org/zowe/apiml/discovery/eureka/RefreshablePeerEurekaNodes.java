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
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.HttpReplicationClient;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.Jersey3ReplicationClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters;
import org.springframework.context.ApplicationListener;
import org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode;

import java.util.Set;

public class RefreshablePeerEurekaNodes extends PeerEurekaNodes
    implements ApplicationListener<EnvironmentChangeEvent> {

    private ReplicationClientAdditionalFilters replicationClientAdditionalFilters;
    private int maxPeerRetries;

    public RefreshablePeerEurekaNodes(final PeerAwareInstanceRegistry registry,
                                      final EurekaServerConfig serverConfig,
                                      final EurekaClientConfig clientConfig, final ServerCodecs serverCodecs,
                                      final ApplicationInfoManager applicationInfoManager,
                                      final ReplicationClientAdditionalFilters replicationClientAdditionalFilters,
                                      final int maxPeerRetries) {
        super(registry, serverConfig, clientConfig, serverCodecs,
            applicationInfoManager);
        this.replicationClientAdditionalFilters = replicationClientAdditionalFilters;
        this.maxPeerRetries = maxPeerRetries;
    }

    @Override
    public PeerEurekaNode createPeerEurekaNode(String peerEurekaNodeUrl) {
        HttpReplicationClient replicationClient = Jersey3ReplicationClient.createReplicationClient(serverConfig, serverCodecs, peerEurekaNodeUrl);
        String targetHost = hostFromUrl(peerEurekaNodeUrl);
        if (targetHost == null) {
            targetHost = "host";
        }
        return new ApimlPeerEurekaNode(registry, targetHost, peerEurekaNodeUrl, replicationClient, serverConfig, maxPeerRetries);
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
