/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.springframework.cloud.commons.util.InetUtils;

/**
 * Fills in the parts of a registration that are not configured.
 * <p>
 * Netflix did this in {@code EurekaInstanceConfigBean}'s constructor and in Spring Cloud's
 * {@code EurekaClientAutoConfiguration}, which is why the defaults were invisible: a service that never set
 * {@code eureka.instance.hostname} still registered a host name and an IP address, resolved from the network
 * interfaces. Reproduced here, in one place both the autoconfiguration and its tests can call.
 */
public final class RegistryInstanceDefaults {

    private RegistryInstanceDefaults() {
    }

    /**
     * @param applicationName {@code spring.application.name}, which is where the service id comes from when
     *                        {@code eureka.instance.appname} is not set - as it never is in API ML
     * @param inetUtils       resolves the local host name and address; null skips that defaulting, for tests
     *                        that must not depend on the machine they run on
     */
    public static void apply(RegistryInstanceProperties config, String applicationName, InetUtils inetUtils) {
        if (RegistryInstanceProperties.UNKNOWN.equals(config.getAppname()) && applicationName != null) {
            config.setAppname(applicationName);
        }
        if (RegistryInstanceProperties.UNKNOWN.equals(config.getVirtualHostName())) {
            config.setVirtualHostName(config.getAppname());
        }
        if (RegistryInstanceProperties.UNKNOWN.equals(config.getSecureVirtualHostName())) {
            config.setSecureVirtualHostName(config.getAppname());
        }

        if (inetUtils != null && (config.getHostname() == null || config.getIpAddress() == null)) {
            InetUtils.HostInfo hostInfo = inetUtils.findFirstNonLoopbackHostInfo();
            if (config.getHostname() == null) {
                config.setHostname(hostInfo.getHostname());
            }
            if (config.getIpAddress() == null) {
                // Registered even when the host name is configured: the Discovery Service puts it on the wire
                // as ipAddr, and a registration without one is rejected by the schema.
                config.setIpAddress(hostInfo.getIpAddress());
            }
        }
    }

}
