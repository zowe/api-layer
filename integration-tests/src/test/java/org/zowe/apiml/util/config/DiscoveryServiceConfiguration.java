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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.product.constants.CoreService;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscoveryServiceConfiguration implements ServiceConfiguration {
    private String scheme;
    private String user;
    private String password;
    private String host;
    private String additionalHost;
    private int port;
    private int additionalPort;
    private int instances;
    private String additionalConnectPorts;

    @Override
    public String getServiceId() {
        return CoreService.DISCOVERY.getServiceId();
    }

    /**
     * additionalHost is a second host list, separate from getHost(), so it needs its own
     * lookup against additionalConnectPorts (comma-list, for more than one additional instance,
     * e.g. apiml-2 AND apiml-3) / additionalPort (single value, the common 2-instance case),
     * falling through to the primary host/connectPorts lookup for anything else.
     */
    @Override
    public int getConnectPortForHost(String host) {
        if (host != null && StringUtils.isNotBlank(additionalHost)) {
            String[] hostArr = additionalHost.split("[,;]");
            for (int i = 0; i < hostArr.length; i++) {
                if (hostArr[i].trim().equalsIgnoreCase(host.trim())) {
                    int fallback = additionalPort > 0 ? additionalPort : getPort();
                    if (StringUtils.isNotBlank(additionalConnectPorts)) {
                        String[] portArr = additionalConnectPorts.split("[,;]");
                        if (i < portArr.length) {
                            return Integer.parseInt(portArr[i].trim());
                        }
                    }
                    return fallback;
                }
            }
        }
        return ServiceConfiguration.super.getConnectPortForHost(host);
    }

}

