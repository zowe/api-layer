/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client.impl;

import com.netflix.discovery.EurekaClientConfig;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.EurekaClientConfiguration;

/**
 * Trivial EurekaClientConfigProvider implementation.
 * Extended EurekaClientConfigProvider implementations can enhance the config metaData or provide different EurekaClientConfig implementation
 * which hold additional data or fetches certain configuration parameters differently.
 *
 * Additionally Netflix implementation DefaultEurekaClientConfig provides some parameters by dynamically fetching them from Archaius1, which in some situations is good,
 * but sometimes can imply incorrect behavior, e.g. if we want to configure certain parameter but want to make sure it won't change in runtime.
 * Another usage is to pass the parameter in different way than Archaius1 is able to access it, for example store it in metadata.
  *
 * See API ML EurekaClientConfiguration for example how some config parameters are hard coded.
 */
public class ApiMlEurekaClientConfigProvider implements EurekaClientConfigProvider {

    private EurekaClientConfig clientConfig;

    /**
     *  Wrapps the ApiMediationServiceConfig argument by a default implementation of {@link EurekaClientConfiguration}
     *
     * @param config
     * @return
     */
    @Override
    public EurekaClientConfig config(ApiMediationServiceConfig config) {
        clientConfig = new EurekaClientConfiguration(config);
        return clientConfig;
    }


    @Override
    public EurekaClientConfig get() {
        return clientConfig;
    }
}
