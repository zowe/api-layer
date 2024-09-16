/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.discovery.StatusChangeEvent;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.zaas.security.login.Providers;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class JwtSecurityTest {
    public static final String KEY_ALIAS = "localhost";
    private JwtSecurity underTest;
    private Providers providers;

    @Mock
    private EurekaClient eurekaClient;

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
            underTest = new JwtSecurity(providers, KEY_ALIAS, "../keystore/localhost/localhost.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
        }

        @Test
        void givenSafIsUsed_thenProperKeysAreInitialized() {
            when(providers.isZosfmUsed()).thenReturn(false);

            underTest.loadAppropriateJwtKeyOrFail();
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }

        @Test
        void givenZosmfIsUsedWithoutJwt_thenProperKeysAreInitialized() {
            when(providers.zosmfSupportsJwt()).thenReturn(false);

            underTest.loadAppropriateJwtKeyOrFail();
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }

        @Test
        void givenZosmfConfiguredWithLtpa_thenProperKeysAreInitialized() {
            when(providers.isZosmfConfigurationSetToLtpa()).thenReturn(true);

            underTest.loadAppropriateJwtKeyOrFail();
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }
    }

    @Nested
    class WhenInitializedWithoutValidJWT {
        @BeforeEach
        void setUp() {
            underTest = new JwtSecurity(providers, null, "../keystore/localhost/localhost.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
        }

        @Test
        void givenZosmfIsUsedWithValidJwt_thenMissingJwtIsIgnored() {
            when(providers.zosmfSupportsJwt()).thenReturn(true);

            underTest.loadAppropriateJwtKeyOrFail();
            assertThat(underTest.getJwtSecret(), is(nullValue()));
        }

        @Test
        void givenSafIsUsed_exceptionIsThrown() {
            when(providers.isZosfmUsed()).thenReturn(false);

            assertThrows(HttpsConfigError.class, () -> underTest.loadAppropriateJwtKeyOrFail());
        }

        @Test
        void givenZosmfIsUsedWithoutJwt_exceptionIsThrown() {
            when(providers.zosmfSupportsJwt()).thenReturn(false);

            assertThrows(HttpsConfigError.class, () -> underTest.loadAppropriateJwtKeyOrFail());
        }

        @Test
        void givenZosmfConfiguredWithLtpa_thenExceptionIsThrown() {
            when(providers.isZosmfConfigurationSetToLtpa()).thenReturn(true);

            assertThrows(HttpsConfigError.class, () -> underTest.loadAppropriateJwtKeyOrFail());
        }
    }

    @Nested
    class WhenZosmfNotOnlineAndAvailableAtStart {

        @BeforeEach
        void setUp() {
            underTest = new JwtSecurity(providers, KEY_ALIAS, "../keystore/localhost/localhost.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
        }

        @Test
        void givenZosmfIsntRegisteredAtTheStartupButRegistersLater_thenProperKeysAreInitialized() {
            when(providers.isZosmfAvailableAndOnline())
                .thenReturn(false)
                .thenReturn(true);
            when(providers.zosmfSupportsJwt()).thenReturn(true);
            underTest.loadAppropriateJwtKeyOrFail();
            verify(eurekaClient, times(1)).registerEventListener(any());
            assertFalse(underTest.getZosmfListener().isZosmfReady());

            EurekaEventListener zosmfEventListener = underTest.getZosmfListener().getZosmfRegisteredListener();
            zosmfEventListener.onEvent(new CacheRefreshedEvent());

            assertTrue(underTest.getZosmfListener().isZosmfReady());
            verify(providers, times(2)).isZosmfAvailableAndOnline();
            verify(eurekaClient, times(1)).unregisterEventListener(any());
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }

        @Test
        void givenMultipleEurekaEvents_thenCheckZosmfWhenCacheRefreshedEvent() {
            when(providers.isZosmfAvailableAndOnline())
                .thenReturn(false)
                .thenReturn(true);
            when(providers.zosmfSupportsJwt()).thenReturn(true);

            underTest.loadAppropriateJwtKeyOrFail();
            verify(eurekaClient, times(1)).registerEventListener(any());
            assertFalse(underTest.getZosmfListener().isZosmfReady());

            EurekaEventListener zosmfEventListener = underTest.getZosmfListener().getZosmfRegisteredListener();
            zosmfEventListener.onEvent(new CacheRefreshedEvent());
            zosmfEventListener.onEvent(new StatusChangeEvent(null, null));

            assertTrue(underTest.getZosmfListener().isZosmfReady());
            verify(eurekaClient, times(1)).unregisterEventListener(any());
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }

        @Test
        void givenCacheRefreshedEvents_thenCheckZosmfForEach() {
            when(providers.isZosmfAvailableAndOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);
            when(providers.zosmfSupportsJwt()).thenReturn(true);
            underTest.loadAppropriateJwtKeyOrFail();
            verify(eurekaClient, times(1)).registerEventListener(any());
            assertFalse(underTest.getZosmfListener().isZosmfReady());

            EurekaEventListener zosmfEventListener = underTest.getZosmfListener().getZosmfRegisteredListener();
            zosmfEventListener.onEvent(new CacheRefreshedEvent());
            zosmfEventListener.onEvent(new CacheRefreshedEvent());

            assertTrue(underTest.getZosmfListener().isZosmfReady());
            verify(providers, times(3)).isZosmfAvailableAndOnline();
            verify(eurekaClient, times(1)).unregisterEventListener(any());
            assertThat(underTest.getJwtSecret(), is(not(nullValue())));
        }
    }

    @Nested
    class GetJwkPublicKey {
        @BeforeEach
        void setUp() {
            underTest = new JwtSecurity(providers, KEY_ALIAS, "../keystore/localhost/localhost.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);

            when(providers.isZosfmUsed()).thenReturn(false);
        }

        @Test
        void asSet() {
            underTest.loadAppropriateJwtKeyOrFail();
            JWKSet result = underTest.getPublicKeyInSet();

            assertThat(result.getKeys().size(), is(1));
        }

        @Test
        void whenOnePresent_asOneKey() {
            underTest.loadAppropriateJwtKeyOrFail();
            Optional<JWK> result = underTest.getJwkPublicKey();

            assertThat(result.isPresent(), is(true));
        }

        @Test
        void whenKeyNotLoaded_Empty() {
            Optional<JWK> result = underTest.getJwkPublicKey();

            assertThat(result.isPresent(), is(false));
        }
    }
}

