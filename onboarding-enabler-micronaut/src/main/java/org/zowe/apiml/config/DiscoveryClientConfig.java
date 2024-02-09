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

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.annotation.PostConstruct;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.zowe.apiml.exception.ServiceDefinitionException;

@ConfigurationProperties(
    value = "apiml.service"
)
public class DiscoveryClientConfig extends ApiMediationServiceConfig {

    @PostConstruct
    @Override
    public void setIpAddressIfNotPresents() throws ServiceDefinitionException {
        ApiMediationServiceConfigReader.setServiceIpAddress(this);
    }

}
