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

public interface ServiceConfiguration {

    String getScheme();

    String getHost();

    int getPort();

    String getServiceId();

    default boolean isStaticallyRegistred() {
        return false;
    }

    default String getServletContext() {
        return "/";
    }

    default boolean isBasicAuthenticationSupported() {
        return true;
    }

    /**
     * Comma-separated ports, positionally paired with getHost()'s comma-separated hosts - each
     * secondary/tertiary instance binds its own distinct real port (see docker-compose.yml's
     * APIML_SERVICE_PORT), so a service with more than one host needs this to say which port
     * belongs to which. Null/blank (the default) means every host uses getPort(), which is
     * correct for every service that only ever has one instance/host.
     */
    default String getConnectPorts() {
        return null;
    }

    /**
     * Resolve the port for a specific hostname taken from getHost()'s comma-separated list,
     * pairing it positionally with getConnectPorts(). This is both the port a test should
     * connect on AND the port that hostname self-registers under as its eureka instance
     * identity - the two are the same port, since each instance now binds its own real port
     * directly rather than sharing one and being reached via a different published port. Falls
     * back to getPort() if connectPorts is blank, the host isn't found, or there's no
     * corresponding entry - so callers that only ever deal with the primary/default host are
     * unaffected.
     */
    default int getConnectPortForHost(String host) {
        return getConnectPortForHost(host, getPort());
    }

    default int getConnectPortForHost(String host, int fallbackPort) {
        String connectPorts = getConnectPorts();
        String hosts = getHost();
        if (org.apache.commons.lang3.StringUtils.isBlank(connectPorts) || org.apache.commons.lang3.StringUtils.isBlank(hosts) || host == null) {
            return fallbackPort;
        }
        String[] hostArr = hosts.split(",");
        String[] portArr = connectPorts.split("[,;]");
        for (int i = 0; i < hostArr.length; i++) {
            if (hostArr[i].trim().equalsIgnoreCase(host.trim())) {
                return i < portArr.length ? Integer.parseInt(portArr[i].trim()) : fallbackPort;
            }
        }
        return fallbackPort;
    }

}
