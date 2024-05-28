/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config.service.security;

import com.netflix.discovery.EurekaClient;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

//@TestConfiguration TODO: verify if this bean is really used
public class MockedSecurityContext {

    @Bean
    public EurekaClient getDiscoveryClient() {
        return mock(EurekaClient.class);
    }

    @Bean
    public EurekaMetadataParser getEurekaMetadataParser() {
        return spy(new EurekaMetadataParser());
    }

    @Bean
    public AuthenticationService getAuthenticationService() {
        return mock(AuthenticationService.class);
    }

    @Bean
    public AuthSourceService getAuthSourceService() {
        return mock(AuthSourceService.class);
    }

}
