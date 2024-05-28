/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.util.Collections;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.security.common.config.AuthConfigurationProperties.JWT_AUTOCONFIGURATION_MODE.*;

class ProvidersTest {
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discovery;
    private Providers underTest;
    private CompoundAuthProvider compoundAuthProvider;
    private ZosmfService zosmfService;
    private static final String ZOSMF_ID = "zosmf";

    @BeforeEach
    void setUp() {
        authConfigurationProperties = mock(AuthConfigurationProperties.class);
        when(authConfigurationProperties.getZosmf()).thenReturn(mock(AuthConfigurationProperties.Zosmf.class));
        compoundAuthProvider = mock(CompoundAuthProvider.class);
        discovery = mock(DiscoveryClient.class);
        zosmfService = mock(ZosmfService.class);
        underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService);
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
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class whenJwtSupportIsVerified {

        @ParameterizedTest
        @MethodSource("provideZosmfConfiguration")
        void givenZosmfConfiguration_thenSupportJwtReturnsProperly(boolean loginEndpointExists, boolean jwtBuilderEndpointExists, boolean zosmfShouldSupportJwt) {
            when(zosmfService.loginEndpointExists()).thenReturn(loginEndpointExists);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(jwtBuilderEndpointExists);
            when(authConfigurationProperties.getZosmf().getJwtAutoconfiguration()).thenReturn(AUTO);

            assertThat(underTest.zosmfSupportsJwt(), is(zosmfShouldSupportJwt));
        }

        @Test
        void givenEndpointsExistAndLtpaOverrideSet_thenSupportJwtRetundsFalse() {
            underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService);
            when(zosmfService.loginEndpointExists()).thenReturn(true);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(true);
            when(authConfigurationProperties.getZosmf().getJwtAutoconfiguration()).thenReturn(LTPA);
            assertThat(underTest.zosmfSupportsJwt(), is(false));
        }

        @Test
        void givenEndpointsDontExistAndJwtOverrideSet_thenSupportJwtReturnsTrue() {
            underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService);
            when(zosmfService.loginEndpointExists()).thenReturn(false);
            when(zosmfService.jwtBuilderEndpointExists()).thenReturn(false);
            when(authConfigurationProperties.getZosmf().getJwtAutoconfiguration()).thenReturn(JWT);
            assertThat(underTest.zosmfSupportsJwt(), is(true));
        }

        private Stream<Arguments> provideZosmfConfiguration() {
            return Stream.of(
                Arguments.of(true, true, true),
                Arguments.of(false, true, false),
                Arguments.of(true, false, false)
            );
        }
    }

    @Nested
    class whenZosmfLtpaConfigurationIsTested {
        @BeforeEach
        void setUp() {
            underTest = new Providers(discovery, authConfigurationProperties, compoundAuthProvider, zosmfService);
        }

        @Test
        void givenZosmfAuthConfigurationLtpa_thenReturnTrue() {
            when(authConfigurationProperties.getZosmf().getJwtAutoconfiguration()).thenReturn(LTPA);
            assertThat(underTest.isZosmfConfigurationSetToLtpa(), is(true));
        }

        @Test
        void givenZosmfAuthConfigurationNotLtpa_thenReturnFalse() {
            when(authConfigurationProperties.getZosmf().getJwtAutoconfiguration()).thenReturn(JWT);
            assertThat(underTest.isZosmfConfigurationSetToLtpa(), is(false));
        }
    }

    private void prepareValidZosmfInDiscovery() {
        when(discovery.getInstances(ZOSMF_ID)).thenReturn(
            Collections.singletonList(mock(ServiceInstance.class))
        );
        when(discovery.getServices()).thenReturn(Collections.singletonList("serviceId")); // just verifies if there's at least one as a basic health
        doNothing().when(discovery).probe();
        when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);
    }
}
