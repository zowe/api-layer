/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hwsjersey.listener;

import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.zowe.apiml.exception.ServiceDefinitionException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

@Slf4j
public class ApiDiscoveryListener implements ServletContextListener {
    private ApiMediationClient apiMediationClient;

    public ApiDiscoveryListener() {
        this.apiMediationClient = new ApiMediationClientImpl();
    }

    public ApiDiscoveryListener(ApiMediationClient apiMediationClient) {
        this.apiMediationClient = apiMediationClient;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {
            ApiMediationServiceConfig config = new ApiMediationServiceConfigReader().loadConfiguration(sce.getServletContext());
            if (config != null) {
                apiMediationClient.register(config);
            }
        } catch (ServiceDefinitionException e) {
            log.error("Service registration failed. Check log for previous errors: ", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        if (apiMediationClient != null) {
            apiMediationClient.unregister();
        }
        apiMediationClient = null;
    }

    public ApiMediationClient getApiMediationClient() {
        return apiMediationClient;
    }

}
