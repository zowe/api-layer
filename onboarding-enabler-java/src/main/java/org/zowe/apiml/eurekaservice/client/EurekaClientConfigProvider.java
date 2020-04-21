/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client;

import com.netflix.discovery.EurekaClientConfig;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;

import javax.inject.Provider;

/**
 * Provides a EurekaClientConfig implementation based on the provided ApiMl service configuration.
 */
public interface EurekaClientConfigProvider extends Provider<EurekaClientConfig> {

    /**
     *  * @param config Configuration for the Eureka
     *  * @return Valid client for the Discovery service
     */
    EurekaClientConfig config(final ApiMediationServiceConfig config);
}
