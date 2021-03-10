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

import org.junit.jupiter.api.*;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.gateway.security.config.CompoundAuthProvider;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfConfiguration;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.gateway.security.login.zosmf.ZosmfConfiguration.JWT_AUTOCONFIGURATION_MODE.*;

class ProvidersTest {
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discovery;
    private Providers underTest;
    private CompoundAuthProvider compoundAuthProvider;
    private ZosmfService zosmfService;
    private static final String ZOSMF_ID = "zosmf";
    private ZosmfConfiguration zosmfConfiguration = ZosmfConfiguration.of(AUTO);

    @BeforeEach
    void setUp() {
        authConfigurationProperties = mock(AuthConfigurationProperties.class);
        compoundAuthProvider = mock(CompoundAuthProvider.class);
        discovery = mock(DiscoveryClient.class);
        zosmfService = mock(ZosmfService.class);
        zosmfConfiguration = ZosmfConfiguration.of(AUTO);
        underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService, zosmfConfiguration);
    }

    @Nested
    class whenInUseIsRequested {
        @Test
        void givenZosmfAsAuthentication_thenReturnTrue() {
            when(compoundAuthProvider.getLoginAuthProviderName()).thenReturn(LoginProvider.ZOSMF.getValue());

            assertThat(underTest.isZosfmUsed(), is(true));
        }

        @Test
        void givenSafIsUsedAsAuthentication_thenReturnFalse() {
            when(compoundAuthProvider.getLoginAuthProviderName()).thenReturn(LoginProvider.SAF.getValue());

            assertThat(underTest.isZosfmUsed(), is(false));
        }
    }

    @Nested
    class whenAvailabilityIsRequested {
        @Test
        void givenZosmfIsKnownByDiscovery_thenReturnTrue() {
            prepareValidZosmfInDiscovery();

            assertThat(underTest.isZosmfAvailable(), is(true));
        }

        @Test
        void givenZosmfIsUnknownByDiscovery_thenReturnFalse() {
            when(discovery.getInstances(ZOSMF_ID)).thenReturn(Collections.emptyList());

            assertThat(underTest.isZosmfAvailable(), is(false));
        }
    }

    @Nested
    class whenAvailabilityAndAccessibilityOfZosmfIsRequested {
        @Test
        void givenZosmfIsAvailableAndAccessible_thenTrueIsReturned() {
            prepareValidZosmfInDiscovery();
            when(zosmfService.isAccessible()).thenReturn(true);

            assertThat(underTest.isZosmfAvailableAndOnline(), is(true));
        }

        @Nested
        class thenFalseIsReturned {
            @Test
            void givenZosmfIsntRegisteredToTheGatewayYet() {
                when(discovery.getInstances(ZOSMF_ID)).thenReturn(Collections.emptyList());
                when(zosmfService.isAccessible()).thenThrow(ServiceNotAccessibleException.class);

                assertThat(underTest.isZosmfAvailableAndOnline(), is(false));
            }

            @Test
            void givenSafIsUsed() {
                when(discovery.getInstances(ZOSMF_ID)).thenReturn(Collections.emptyList());

                assertThat(underTest.isZosmfAvailableAndOnline(), is(false));
            }

            @Test
            void givenZosmfIsRegisteredButUnavailable() {
                prepareValidZosmfInDiscovery();
                when(zosmfService.isAccessible()).thenReturn(false);

                assertThat(underTest.isZosmfAvailableAndOnline(), is(false));
            }
        }
    }

    @Nested
    class whenJwtSupportIsVerified {
        @Test
        void givenLoginEndpointAndJwtBuilderEndpointExist_thenSupportJwtReturnsTrue() {
            when(zosmfService.loginEndpointExists()).thenReturn(true);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(true);

            assertThat(underTest.zosmfSupportsJwt(), is(true));
        }

        @Test
        void givenLoginEndpointDoesntExist_thenSupportJwtReturnsFalse() {
            when(zosmfService.loginEndpointExists()).thenReturn(false);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(true);

            assertThat(underTest.zosmfSupportsJwt(), is(false));
        }

        @Test
        void givenJwtBuilderEndpointDoesntExist_thenSupportJwtReturnsFalse() {
            when(zosmfService.loginEndpointExists()).thenReturn(true);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(false);

            assertThat(underTest.zosmfSupportsJwt(), is(false));
        }

        @Test
        void givenEndpointsExistAndLtpaOverrideSet_thenSupportJwtRetundsFalse() {
            zosmfConfiguration.jwtAutoconfigurationMode = LTPA;
            underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService, zosmfConfiguration);
            when(zosmfService.loginEndpointExists()).thenReturn(true);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(true);
            assertThat(underTest.zosmfSupportsJwt(), is(false));
        }

        @Test
        void givenEndpointsDontExistAndJwtOverrideSet_thenSupportJwtRetundsTrue() {
            zosmfConfiguration.jwtAutoconfigurationMode = JWT;
            underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService, zosmfConfiguration);
            when(zosmfService.loginEndpointExists()).thenReturn(false);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(false);
            assertThat(underTest.zosmfSupportsJwt(), is(true));
        }
    }

    private void prepareValidZosmfInDiscovery() {
        when(discovery.getInstances(ZOSMF_ID)).thenReturn(
            Collections.singletonList(mock(ServiceInstance.class))
        );
        when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);
    }
}
