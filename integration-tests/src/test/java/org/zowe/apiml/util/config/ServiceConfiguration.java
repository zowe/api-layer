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
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class ServiceConfiguration {

    private String scheme;
    private String url;
    private String host;
    // holds comma separated list of gw ports, hence String
    private String port;
    private int instances;

    public abstract String getServiceId();

    public boolean isStaticallyRegistred() {
        return false;
    }

    public String getServletContext() {
        return "/";
    }

    public boolean isBasicAuthenticationSupported() {
        return true;
    }

    /**
     * Returns the first host in the list
     */
    public String getFirstHost() {
        return getHosts().get(0);
    }

    /**
     * Returns the first host in the list
     */
    public List<String> getHosts() {
        if (StringUtils.isBlank(this.host)) {
            return Collections.emptyList();
        }
        return Arrays.asList(host.split(","));
    }

//    public int getPort() {
//        if (port.split(",").length == 1) {
//            return Integer.parseInt(port);
//        }
//        throw new IllegalArgumentException("Multiple hosts defined, use getPortForHost(String host) instead");
//    }

    /**
     * Resolve the port for a specific hostname taken from host's comma-separated list,
     * pairing it positionally with ports defined.
     */
    public int getPortForHost(String hostToMatch) {
        var hosts = getHosts();
        var ports = Arrays.asList(this.port.split(","));

        if (hosts.size() != ports.size()) {
            throw new IllegalArgumentException("Host and port must have same length");
        }

        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).trim().equalsIgnoreCase(hostToMatch.trim())) {
                return Integer.parseInt(ports.get(i).trim());
            }
        }

        throw new IllegalArgumentException("Hostname %s not found in service configuration".formatted(hostToMatch));
    }

}
