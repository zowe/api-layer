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

import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.EurekaClientConfiguration;

/**
 * Trivial EurekaClientConfigProvider implementation.
 * Extended EurekaClientConfigProvider implementations can enhance the config metadata or provide a different client
 * configuration implementation which holds additional data or fetches certain configuration parameters differently.
 */
public class ApiMlEurekaClientConfigProvider implements EurekaClientConfigProvider {

    private EurekaClientConfiguration clientConfig;

    /**
     * Wraps the ApiMediationServiceConfig argument by a default implementation of {@link EurekaClientConfiguration}
     *
     * @param config
     * @return
     */
    @Override
    public EurekaClientConfiguration config(ApiMediationServiceConfig config) {
        clientConfig = new EurekaClientConfiguration(config);
        return clientConfig;
    }


    @Override
    public EurekaClientConfiguration get() {
        return clientConfig;
    }
}
