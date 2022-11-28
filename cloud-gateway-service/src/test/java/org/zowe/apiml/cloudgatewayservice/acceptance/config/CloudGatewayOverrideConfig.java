/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApplicationRegistry;

@TestConfiguration
public class CloudGatewayOverrideConfig {

    @Bean
    public ApplicationRegistry registry() {
        ApplicationRegistry applicationRegistry = new ApplicationRegistry();
        return applicationRegistry;
    }

}
