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
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.HashUtil;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.zaas.security.login.Providers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtSecurityTest {
    public static final String KEY_ALIAS = "localhost";
    private JwtSecurity underTest;
    private Providers providers;

    @Mock
    private EurekaClient eurekaClient;

    @BeforeEach
    void setUp() {
        providers = mock(Providers.class);
        lenient().when(providers.isZosfmUsed()).thenReturn(true);
        lenient().when(providers.isZosmfConfigurationSetToLtpa()).thenReturn(false);
        lenient().when(providers.isZosmfAvailableAndOnline()).thenReturn(true);
    }

    @Nested
    class WhenInitializedWithValidJWT {
        @BeforeEach
        void setUp() {
            underTest = new JwtSecurity(providers, KEY_ALIAS, "../keystore/service/service.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
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
            underTest = new JwtSecurity(providers, null, "../keystore/service/service.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
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
            underTest = new JwtSecurity(providers, KEY_ALIAS, "../keystore/service/service.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
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
    class BuildVerifier {

        private JwtSecurity jwtSecurity;

        @BeforeEach
        void setUp() {
            jwtSecurity = new JwtSecurity(providers, eurekaClient);
        }

        @Test
        void givenRsaPublicKey_thenReturnsRSASSAVerifier() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) gen.generateKeyPair().getPublic();

            JWSVerifier verifier = jwtSecurity.buildVerifier(rsaPublicKey);

            assertThat(verifier, is(instanceOf(RSASSAVerifier.class)));
        }

        @Test
        void givenEcPublicKey_thenReturnsECDSAVerifier() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(256);
            ECPublicKey ecPublicKey = (ECPublicKey) gen.generateKeyPair().getPublic();

            JWSVerifier verifier = jwtSecurity.buildVerifier(ecPublicKey);

            assertThat(verifier, is(instanceOf(ECDSAVerifier.class)));
        }

        @Test
        void givenUnsupportedKeyType_thenReturnsNull() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
            gen.initialize(1024);
            PublicKey dsaPublicKey = gen.generateKeyPair().getPublic();

            JWSVerifier verifier = jwtSecurity.buildVerifier(dsaPublicKey);

            assertNull(verifier);
        }

        @Test
        void givenNullKey_thenReturnsNull() {
            JWSVerifier verifier = jwtSecurity.buildVerifier(null);

            assertNull(verifier);
        }
    }

    @Nested
    class GetJwkPublicKey {
        @BeforeEach
        void setUp() {
            underTest = new JwtSecurity(providers, KEY_ALIAS, "../keystore/service/service.keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);

            lenient().when(providers.isZosfmUsed()).thenReturn(false);
        }

        @Test
        void asSet() {
            underTest.loadAppropriateJwtKeyOrFail();
            var result = underTest.getPublicKeyInSet();

            assertThat(result.getJsonWebKeys().size(), is(1));
        }

        @Test
        void whenOnePresent_asOneKey() {
            underTest.loadAppropriateJwtKeyOrFail();
            var result = underTest.getJwkPublicKey();

            assertThat(result.isPresent(), is(true));
        }

        @Test
        void whenKeyNotLoaded_Empty() {
            var result = underTest.getJwkPublicKey();

            assertThat(result.isPresent(), is(false));
        }

        @Test
        void whenKeyNotLoaded_noSigningJwks() {
            assertTrue(underTest.getAllSigningJwks().isEmpty());
        }

        @Test
        void whenKeyNotLoaded_noVerifierForAnyKeyId() {
            assertNull(underTest.getJwtVerifier(null));
            assertNull(underTest.getJwtVerifier("unknown"));
        }
    }

    @Nested
    class GivenMultipleSigningKeysInKeystore {
        private KeyPair signingKeyPair;
        private KeyPair rotatedKeyPair;
        private MockedStatic<SecurityUtils> securityUtils;

        @BeforeEach
        void setUp() throws NoSuchAlgorithmException {
            signingKeyPair = generateRsaKeyPair();
            rotatedKeyPair = generateRsaKeyPair();

            securityUtils = mockStatic(SecurityUtils.class);
            securityUtils.when(() -> SecurityUtils.loadKey(any())).thenReturn(signingKeyPair.getPrivate());
            securityUtils.when(() -> SecurityUtils.loadPublicKey(any())).thenReturn(signingKeyPair.getPublic());
            securityUtils.when(() -> SecurityUtils.loadSigningKeys(any(), any()))
                .thenReturn(List.of(rotatedKeyPair.getPublic(), signingKeyPair.getPublic()));

            when(providers.isZosfmUsed()).thenReturn(false);
            underTest = new JwtSecurity(providers, KEY_ALIAS, "keystore.p12", "password".toCharArray(), "password".toCharArray(), eurekaClient);
            underTest.loadAppropriateJwtKeyOrFail();
        }

        @AfterEach
        void tearDown() {
            securityUtils.close();
        }

        @Test
        void givenTokenWithoutKeyId_thenVerifierOfCurrentSigningKeyIsReturned() {
            var verifier = underTest.getJwtVerifier(null);

            assertThat(verifier, is(instanceOf(RSASSAVerifier.class)));
            assertThat(((RSASSAVerifier) verifier).getPublicKey(), is(signingKeyPair.getPublic()));
        }

        @Test
        void givenKeyIdOfCurrentSigningKey_thenVerifierOfCurrentSigningKeyIsReturned() throws JoseException {
            var verifier = underTest.getJwtVerifier(thumbprint(signingKeyPair.getPublic()));

            assertThat(((RSASSAVerifier) verifier).getPublicKey(), is(signingKeyPair.getPublic()));
        }

        @Test
        void givenKeyIdOfRotatedKey_thenVerifierOfRotatedKeyIsReturned() throws JoseException {
            var verifier = underTest.getJwtVerifier(thumbprint(rotatedKeyPair.getPublic()));

            assertThat(((RSASSAVerifier) verifier).getPublicKey(), is(rotatedKeyPair.getPublic()));
        }

        @Test
        void givenNoVerificationKeyAliases_thenAllPrivateKeyEntriesAreLoaded() {
            securityUtils.verify(() -> SecurityUtils.loadSigningKeys(any(), eq(List.of())));
        }

        @Test
        void givenVerificationKeyAliases_thenOnlyTheseAliasesAreLoaded() {
            ReflectionTestUtils.setField(underTest, "verificationKeyAliases", List.of(" oldsigner ", ""));

            underTest.loadAppropriateJwtKeyOrFail();

            securityUtils.verify(() -> SecurityUtils.loadSigningKeys(any(), eq(List.of("oldsigner"))));
        }

        @Test
        void givenUnknownKeyId_thenNoVerifierIsReturned() {
            assertNull(underTest.getJwtVerifier("unknown"));
        }

        @Test
        void thenAllSigningJwksAreReturnedWithCurrentSigningKeyFirst() throws JoseException {
            var kids = underTest.getAllSigningJwks().stream().map(JsonWebKey::getKeyId).toList();

            assertThat(kids, is(List.of(thumbprint(signingKeyPair.getPublic()), thumbprint(rotatedKeyPair.getPublic()))));
        }

        @Test
        void thenPublicKeyInSetContainsOnlyCurrentSigningKey() throws JoseException {
            var keys = underTest.getPublicKeyInSet().getJsonWebKeys();

            assertThat(keys.size(), is(1));
            assertThat(keys.get(0).getKeyId(), is(thumbprint(signingKeyPair.getPublic())));
        }

        private KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        }

        private String thumbprint(PublicKey publicKey) throws JoseException {
            return JsonWebKey.Factory.newJwk(publicKey).calculateBase64urlEncodedThumbprint(HashUtil.SHA_256);
        }
    }
}

