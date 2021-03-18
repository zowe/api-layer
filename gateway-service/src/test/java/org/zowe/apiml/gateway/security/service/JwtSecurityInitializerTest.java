/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.security.HttpsConfigError;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtSecurityInitializerTest {
    private JwtSecurityInitializer underTest;
    private Providers providers;

    @BeforeEach
    void setUp() {
        providers = mock(Providers.class);
        when(providers.isZosfmUsed()).thenReturn(true);
        when(providers.isZosmfConfigurationSetToLtpa()).thenReturn(false);
        when(providers.isZosmfAvailableAndOnline()).thenReturn(true);
    }

    @Nested
    class WhenInitializedWithValidJWT {
        @BeforeEach
        void setUp() {
            underTest = new JwtSecurityInitializer(providers, "jwtsecret", "../keystore/localhost/localhost.keystore.p12", "password".toCharArray(), "password".toCharArray(), new Duration(10, TimeUnit.MILLISECONDS));
        }

        @Test
        void givenZosmfIsUsedWithValidJwt_thenJwtSecretIsIgnored() {
            when(providers.zosmfSupportsJwt()).thenReturn(true);

            underTest.init();
            assertThat(underTest.getJwtSecret(), is(nullValue()));
        }

        @Test
        void givenSafIsUsed_thenProperKeysAreInitialized() {
            when(providers.isZosfmUsed()).thenReturn(false);

            underTest.init();
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }

        @Test
        void givenZosmfIsUsedWithoutJwt_thenProperKeysAreInitialized() {
            when(providers.zosmfSupportsJwt()).thenReturn(false);

            underTest.init();
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }

        @Test
        void givenZosmfIsntRegisteredAtTheStartupButRegistersLater_thenProperKeysAreInitialized() {
            when(providers.zosmfSupportsJwt()).thenReturn(false);
            when(providers.isZosmfAvailableAndOnline())
                .thenReturn(false)
                .thenReturn(true);

            underTest.init();

            await()
                .atMost(Duration.TEN_SECONDS)
            .with()
                .pollInterval(new Duration(20, TimeUnit.MILLISECONDS))
                .until(underTest::getJwtSecret, is(not(nullValue())));
        }

        @Test
        void givenZosmfConfiguredWithLtpa_thenProperKeysAreInitialized() {
            when(providers.isZosmfConfigurationSetToLtpa()).thenReturn(true);

            underTest.init();
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }
    }

    @Nested
    class WhenInitializedWithoutValidJWT {
        @BeforeEach
        void setUp() {
            underTest = new JwtSecurityInitializer(providers, null, "../keystore/localhost/localhost.keystore.p12", "password".toCharArray(), "password".toCharArray(), new Duration(10, TimeUnit.MILLISECONDS));
        }

        @Test
        void givenZosmfIsUsedWithValidJwt_thenMissingJwtIsIgnored() {
            when(providers.zosmfSupportsJwt()).thenReturn(true);

            underTest.init();
            assertThat(underTest.getJwtSecret(), is(nullValue()));
        }

        @Test
        void givenSafIsUsed_exceptionIsThrown() {
            when(providers.isZosfmUsed()).thenReturn(false);

            assertThrows(HttpsConfigError.class, () -> underTest.init());
        }

        @Test
        void givenZosmfIsUsedWithoutJwt_exceptionIsThrown() {
            when(providers.zosmfSupportsJwt()).thenReturn(false);

            assertThrows(HttpsConfigError.class, () -> underTest.init());
        }

        @Test
        void givenZosmfConfiguredWithLtpa_thenExceptionIsThrown() {
            when(providers.isZosmfConfigurationSetToLtpa()).thenReturn(true);

            assertThrows(HttpsConfigError.class, () -> underTest.init());
        }
    }
}

