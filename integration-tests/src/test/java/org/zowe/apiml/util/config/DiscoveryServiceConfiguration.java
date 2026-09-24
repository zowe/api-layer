/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

import lombok.*;
import org.zowe.apiml.product.constants.CoreService;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DiscoveryServiceConfiguration extends ServiceConfiguration {
    private String user;
    private String password;
    private String additionalHost;
    private String additionalPort;

    DiscoveryServiceConfiguration(String scheme, String user, String password, String host, String additionalHost, String port, String additionalPort, int instances) {
        super(scheme, null, host, port, instances);
        this.user = user;
        this.password = password;
        this.additionalHost = additionalHost;
        this.additionalPort = additionalPort;
    }

    @Override
    public String getServiceId() {
        return CoreService.DISCOVERY.getServiceId();
    }

    //TODO - make additional host single
    /**
     * additionalHost is a second host list, separate from getHost(), so it needs its own
     * lookup against additionalConnectPorts (comma-list, for more than one additional instance,
     * e.g. apiml-2 AND apiml-3) / additionalPort (single value, the common 2-instance case),
     * falling through to the primary host/connectPorts lookup for anything else.
     */
    @Override
    public int getPortForHost(String host) {
        String[] hostArr = getAdditionalHost().split(",");
        String[] portArr = getAdditionalPort().split(",");

        if (hostArr.length != portArr.length) { throw new IllegalArgumentException("Host and port must have same length"); }

        for (int i = 0; i < hostArr.length; i++) {
            if (hostArr[i].trim().equalsIgnoreCase(host.trim())) {
                return Integer.parseInt(portArr[i].trim());
            }
        }

        return super.getPortForHost(host);
    }

}

