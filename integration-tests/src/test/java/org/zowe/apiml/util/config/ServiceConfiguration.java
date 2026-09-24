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

import java.util.Locale;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class ServiceConfiguration {

    private String scheme;
    private String url;
    private String host;
    @Getter(AccessLevel.NONE)
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

    public int getPort() {
        if (port.split(",").length == 1) {
            return Integer.parseInt(port);
        }
        throw new IllegalArgumentException("Multiple hosts defined, use getPortForHost(String host) instead");
    }

    /**
     * Resolve the port for a specific hostname taken from host's comma-separated list,
     * pairing it positionally with ports defined.
     */
    public int getPortForHost(String host) {
        String[] hostArr = host.split(",");
        String[] portArr = port.split(",");

        if (hostArr.length != portArr.length) { throw new IllegalArgumentException("Host and port must have same length"); }

        for (int i = 0; i < hostArr.length; i++) {
            if (hostArr[i].trim().equalsIgnoreCase(host.trim())) {
                return Integer.parseInt(portArr[i].trim());
            }
        }

        throw new IllegalArgumentException("Hostname %s not found in service configuration".toLowerCase(Locale.ROOT));
    }

}
