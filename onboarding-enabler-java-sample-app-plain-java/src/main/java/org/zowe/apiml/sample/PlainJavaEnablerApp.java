/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.sample;

import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.zowe.apiml.exception.ServiceDefinitionException;

import java.util.Objects;
import java.util.logging.Logger;

public class PlainJavaEnablerApp {

    private static Logger log = Logger.getLogger(PlainJavaEnablerApp.class.getName());

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0 || Objects.isNull(args[0]) || args[0].isEmpty()) {
            throw new IllegalArgumentException("Please provide path to service configuration file as a first argument");
        }
        String configurationFile = args[0];
        try {
            ApiMediationServiceConfigReader reader = new ApiMediationServiceConfigReader();
            ApiMediationServiceConfig config = reader.loadConfiguration(configurationFile);
            ApiMediationClientImpl apiMediationClient = new ApiMediationClientImpl();
            apiMediationClient.register(config);
        } catch (ServiceDefinitionException sde) {
            log.severe("Service configuration failed. Check log for previous errors: " + sde.getMessage());
            sde.printStackTrace();
            throw new IllegalStateException("Configuration problem");
        }
        log.info("Configuration loaded, sleeping the main thread. Verify registration correctness in Discovery service");

        Thread.currentThread().join();
    }
}
