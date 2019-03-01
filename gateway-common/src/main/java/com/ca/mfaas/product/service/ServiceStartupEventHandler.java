/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

public class ServiceStartupEventHandler {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ServiceStartupEventHandler.class);
    public static int DEFAULT_DELAY_FACTOR = 5;

    public void onServiceStartup(String serviceName, int delayFactor) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("{} has been started in {} seconds", serviceName, uptime / 1000.0);

        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                String[] names = new String[]{"com.netflix.discovery.DiscoveryClient",
                    "com.netflix.discovery.shared.transport.decorator.RedirectingEurekaHttpClient"};
                for (String name : names) {
                    Logger logger = loggerContext.getLogger(name);
                    logger.setLevel(Level.ERROR);
                }
            }
        }, uptime * 5);
    }
}
