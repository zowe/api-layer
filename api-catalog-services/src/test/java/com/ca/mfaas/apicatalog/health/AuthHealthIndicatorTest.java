/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.health;

import com.ca.mfaas.security.config.SecurityConfigurationProperties;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthHealthIndicatorTest {

    private static final String ZOSMF = "zosmf";

    @Test
    public void testStatusIsUpWhenZosmfIsAvailable() throws Exception {
        SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(ZOSMF))
            .thenReturn(Arrays.asList(new DefaultServiceInstance(ZOSMF, "host", 443, true)));

        AuthHealthIndicator authHealthIndicator = new AuthHealthIndicator(discoveryClient,
            securityConfigurationProperties);
        Health.Builder builder = new Health.Builder();
        authHealthIndicator.doHealthCheck(builder);
        assertEquals(builder.build().getStatus(), Status.UP);
    }

    @Test
    public void testStatusIsDownWhenNoZosmfIsAvailable() throws Exception {
        SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(Collections.emptyList());

        AuthHealthIndicator authHealthIndicator = new AuthHealthIndicator(discoveryClient,
            securityConfigurationProperties);
        Health.Builder builder = new Health.Builder();
        authHealthIndicator.doHealthCheck(builder);
        assertEquals(builder.build().getStatus(), Status.DOWN);
    }
}
