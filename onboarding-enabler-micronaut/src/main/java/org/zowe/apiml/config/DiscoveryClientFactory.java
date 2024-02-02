/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;

import jakarta.inject.Singleton;

@Factory
public class DiscoveryClientFactory {

    @Requires(missingBeans = EurekaClientProvider.class)
    @Singleton
    public ApiMediationClient defaultApiMlClient() {
        return new ApiMediationClientImpl();
    }

    @Requires(beans = EurekaClientProvider.class)
    @Singleton
    public ApiMediationClient apiMlClient(EurekaClientProvider eurekaClientProvider) {
        return new ApiMediationClientImpl(eurekaClientProvider);
    }

    @Requires(beans = {EurekaClientProvider.class, EurekaClientConfigProvider.class})
    @Singleton
    public ApiMediationClient apiMlClient(EurekaClientProvider eurekaClientProvider, EurekaClientConfigProvider eurekaClientConfigProvider) {
        return new ApiMediationClientImpl(eurekaClientProvider, eurekaClientConfigProvider);
    }

}
