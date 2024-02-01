/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Utility class to extract InstanceInfo object out of list of {@link Server} instances
 */
public class InstanceInfoExtractor {

    @Getter
    private Optional<InstanceInfo> info = Optional.empty();

    public InstanceInfoExtractor(List<Server> serverList) {
        if (serverList == null) {
            throw new IllegalArgumentException("The serverList argument cannot be null");
        }
        if (!serverList.isEmpty()) {
            Server server = serverList.get(new Random().nextInt(serverList.size())); //NOSONAR Random not used in cryptographic context
            if (server instanceof DiscoveryEnabledServer) {
                info = Optional.of(((DiscoveryEnabledServer)server).getInstanceInfo());
            } else {
                throw new IllegalArgumentException("The server list elements are not instances of DiscoveryEnabledServer class");
            }

        }


    }

    public Optional<InstanceInfo> getInstanceInfo() {
        return info;
    }
}
