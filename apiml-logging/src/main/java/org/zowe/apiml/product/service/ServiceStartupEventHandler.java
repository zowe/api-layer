/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

public class ServiceStartupEventHandler {
    public static final int DEFAULT_DELAY_FACTOR = 5;

    private final ApimlLogger apimlLog = ApimlLogger.of(ServiceStartupEventHandler.class,
            YamlMessageServiceInstance.getInstance());

    @SuppressWarnings("squid:S1172")
    public void onServiceStartup(String serviceName, int delayFactor) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        apimlLog.log("apiml.common.serviceStarted", serviceName, uptime / 1000.0);

        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                String[] names = new String[] { "com.netflix.discovery.DiscoveryClient",
                        "com.netflix.discovery.shared.transport.decorator.RedirectingEurekaHttpClient",
                        "com.ca.mfaas.discovery.GatewayNotifier" };
                for (String name : names) {
                    Logger logger = loggerContext.getLogger(name);
                    logger.setLevel(Level.ERROR);
                }
            }
        }, uptime * delayFactor);
    }
}
