/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProvidersTest {
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discovery;
    private Providers underTest;

    private static final String ZOSMF_ID = "zosmf";

    @BeforeEach
    void setUp() {
        authConfigurationProperties = mock(AuthConfigurationProperties.class);
        discovery = mock(DiscoveryClient.class);

        underTest = new Providers(discovery, authConfigurationProperties);
    }


    @Test
    void givenZosmfAsAuthentication_whenInUseIsRequested_thenReturnTrue() {
        when(authConfigurationProperties.getProvider()).thenReturn(AuthenticationScheme.ZOSMF.getScheme());

        assertThat(underTest.isZosfmUsed(), is(true));
    }

    @Test
    void givenSafIsUsedAsAuthentication_whenInUseIsRequested_thenReturnFalse() {
        when(authConfigurationProperties.getProvider()).thenReturn("saf");

        assertThat(underTest.isZosfmUsed(), is(false));
    }

    @Test
    void givenZosmfIsKnownByDiscovery_whenAvailabilityIsRequested_thenReturnTrue() {
        when(discovery.getInstances(ZOSMF_ID)).thenReturn(
            Collections.singletonList(mock(ServiceInstance.class))
        );
        when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);

        assertThat(underTest.isZosmfAvailable(), is(true));
    }

    @Test
    void givenZosmfIsUnknownByDiscovery_whenAvailabilityIsRequested_thenReturnFalse() {
        when(discovery.getInstances(ZOSMF_ID)).thenReturn(Collections.emptyList());

        assertThat(underTest.isZosmfAvailable(), is(false));
    }
}
