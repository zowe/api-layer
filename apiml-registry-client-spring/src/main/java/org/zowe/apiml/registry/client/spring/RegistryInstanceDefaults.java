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

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.core.env.Environment;

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

    /**
     * The ports this service advertises, applied <em>before</em> {@code eureka.instance.*} is bound over them.
     * <p>
     * This is the order {@code EurekaClientAutoConfiguration} used: it set both ports from {@code server.port}
     * on the instance config bean, and Spring bound the configured {@code eureka.instance.*} values afterwards.
     * So an explicit port always won, and a service that never configured one still registered the port it
     * actually listens on. That is why several services' YAML says the ports are computed in code - ZAAS's says
     * exactly that - and why the field defaults of 80 and 443 are not what Netflix ever put on the wire.
     * <p>
     * Deliberately not "set it only if the property looks unset". Asking the environment whether a port was
     * configured needs Spring Boot's relaxed property source to answer correctly, and getting a wrong answer
     * would overwrite a port a service did configure. Setting it first and letting binding win needs no such
     * question and cannot get it wrong.
     * <p>
     * This is not cosmetic. ZAAS listened on 10023 and registered itself on 80 and 443, so the Gateway, the
     * Discovery Service and every other service that resolved it from the registry called a port with nothing
     * behind it. The Discovery Service authenticates {@code /application/**} through ZAAS, so its own
     * {@code eurekaversion} endpoint answered 401, and the integration tests' startup check never got past it.
     *
     * @param environment where {@code server.port} lives; null leaves the configured values alone
     */
    public static void applyPorts(RegistryInstanceProperties config, Environment environment) {
        if (environment == null) {
            return;
        }

        Integer serverPort = environment.getProperty("server.port", Integer.class);
        if (serverPort == null) {
            // The same fallback chain EurekaClientAutoConfiguration used.
            serverPort = environment.getProperty("port", Integer.class, 8080);
        }

        config.setNonSecurePort(serverPort);
        // Read through the Binder rather than the environment: a service writes securePortEnabled and another
        // writes secure-port-enabled, and only the Binder resolves both without depending on Spring Boot having
        // attached its relaxed property source. Read here rather than from the config because at this point
        // eureka.instance.* has not been bound yet.
        boolean securePortEnabled = Binder.get(environment)
            .bind("eureka.instance.secure-port-enabled", Boolean.class)
            .orElse(false);
        if (securePortEnabled) {
            config.setSecurePort(serverPort);
        }
    }

}
