/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.config.ribbon;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class IClientConfiguration {
    @Bean
    public IClientConfig config() {
        return  IClientConfig.Builder.newBuilder(DefaultClientConfigImpl.class, "apicatalog")
            .withSecure(false)
            .withFollowRedirects(false)
            .withDeploymentContextBasedVipAddresses("apicatalog")
            .withLoadBalancerEnabled(false)
            .build();
    }
}
