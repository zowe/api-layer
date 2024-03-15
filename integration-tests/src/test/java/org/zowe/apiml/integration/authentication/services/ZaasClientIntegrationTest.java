/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ConfigReaderZaasClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.*;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;
import org.zowe.apiml.zaasclient.service.internal.ZaasClientImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the integration of the ZAAS Client with the ZAAS provider living in the Gateway.
 */
@GeneralAuthenticationTest
class ZaasClientIntegrationTest implements TestWithStartedInstances {

    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();

    private static final String KEY_ALIAS = ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyAlias();

    private static final String APPLID = ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration().getApplId();

    private static final String INVALID_USER = "usr";
    private static final String INVALID_PASS = "usr";
    private static final String NULL_USER = null;
    private static final String NULL_PASS = null;
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASS = "";
    private static final String NULL_AUTH_HEADER = null;
    private static final String EMPTY_AUTH_HEADER = "";
    private static final String EMPTY_STRING = "";

    private final long now = System.currentTimeMillis();
    private final long expirationForExpiredToken = now - 1000;

    ConfigProperties configProperties;
    ZaasClient tokenService;

    private static String getAuthHeader(String userName, String password) {
        String auth = userName + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
            auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    private String getToken(long now, long expiration, Key jwtSecretKey) {
        return Jwts.builder()
            .setSubject("user")
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer("APIML")
            .setId(UUID.randomUUID().toString())
            .signWith(jwtSecretKey, SignatureAlgorithm.RS256)
            .compact();
    }

    private Key getDummyKey(ConfigProperties configProperties) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        InputStream inputStream;

        KeyStore ks = KeyStore.getInstance(configProperties.getKeyStoreType());

        File keyStoreFile = new File(configProperties.getKeyStorePath());
        inputStream = new FileInputStream(keyStoreFile);
        ks.load(inputStream, configProperties.getKeyStorePassword());

        return ks.getKey(KEY_ALIAS, configProperties.getKeyStorePassword());
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        ZaasClientErrorCodes producedErrorCode = zce.getErrorCode();
        assertThat(producedErrorCode.getId(), Is.is(code.getId()));
        assertThat(producedErrorCode.getMessage(), Is.is(code.getMessage()));
        assertThat(producedErrorCode.getReturnCode(), Is.is(code.getReturnCode()));
    }

    @BeforeEach
    void setUp() throws Exception {
        configProperties = ConfigReaderZaasClient.getConfigProperties();
        tokenService = new ZaasClientImpl(configProperties);
    }

    static Stream<Arguments> provideInvalidUsernamePassword() {
        return Stream.of(
            Arguments.of(INVALID_USER, PASSWORD, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(NULL_USER, PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(EMPTY_USER, PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD)
        );
    }

    static Stream<Arguments> provideInvalidPassword() {
        return Stream.of(
            Arguments.of(USERNAME, INVALID_PASS, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(USERNAME, NULL_PASS, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD)
        );
    }

    static Stream<Arguments> provideInvalidAuthHeaders() {
        return Stream.of(
            Arguments.of(getAuthHeader(INVALID_USER, PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(NULL_USER, PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(EMPTY_USER, PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(NULL_AUTH_HEADER, ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER),
            Arguments.of(EMPTY_AUTH_HEADER, ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER)
        );
    }

    static Stream<Arguments> provideInvalidPasswordAuthHeaders() {
        return Stream.of(
            Arguments.of(getAuthHeader(USERNAME, INVALID_PASS), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(USERNAME, NULL_PASS), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(USERNAME, EMPTY_PASS), ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD)
        );
    }

    @Nested
    class WhenLoggingIn {

        @Nested
        class ValidTokenIsReturned {
            @Test
            void givenValidCredentials() throws ZaasClientException {
                String token = tokenService.login(USERNAME, PASSWORD.toCharArray());
                assertNotNull(token);
                assertThat(token, is(not(EMPTY_STRING)));
            }

            @Test
            void givenValidCredentialsInHeader() throws ZaasClientException {
                String token = tokenService.login(getAuthHeader(USERNAME, PASSWORD));
                assertNotNull(token);
                assertThat(token, is(not(EMPTY_STRING)));
            }
        }

        @Nested
        @TestsNotMeantForZowe
        class ProperExceptionIsRaised {
            @ParameterizedTest(name = "givenInvalidCredentials {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.services.ZaasClientIntegrationTest#provideInvalidUsernamePassword")
            void givenInvalidCredentials(String username, String password, ZaasClientErrorCodes expectedCode) {
                ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(username, password.toCharArray()));

                assertThatExceptionContainValidCode(exception, expectedCode);
            }

            @NotForMainframeTest
            @ParameterizedTest(name = "givenInvalidPassword {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.services.ZaasClientIntegrationTest#provideInvalidPassword")
            void givenInvalidPassword(String username, String password, ZaasClientErrorCodes expectedCode) {
                ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(username, password == null ? null : password.toCharArray()));

                assertThatExceptionContainValidCode(exception, expectedCode);
            }

            @ParameterizedTest(name = "givenInvalidHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.services.ZaasClientIntegrationTest#provideInvalidAuthHeaders")
            void givenInvalidHeader(String authHeader, ZaasClientErrorCodes expectedCode) {
                ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(authHeader));

                assertThatExceptionContainValidCode(exception, expectedCode);
            }

            @NotForMainframeTest
            @ParameterizedTest(name = "givenInvalidPasswordInHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.services.ZaasClientIntegrationTest#provideInvalidPasswordAuthHeaders")
            void givenInvalidPasswordInHeader(String authHeader, ZaasClientErrorCodes expectedCode) {
                ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(authHeader));

                assertThatExceptionContainValidCode(exception, expectedCode);
            }
        }
    }

    @Nested
    class WhenQueriedForDetails {

        @Test
        void givenValidToken_thenValidDetailsAreProvided() throws ZaasClientException {
            String token = tokenService.login(USERNAME, PASSWORD.toCharArray());
            ZaasToken zaasToken = tokenService.query(token);
            assertNotNull(zaasToken);
            assertThat(zaasToken.getUserId(), is(USERNAME));
            assertThat(zaasToken.isExpired(), is(Boolean.FALSE));
        }

        @Nested
        class ProperExceptionIsRaised {

            @Test
            void givenInvalidToken() {
                assertThrows(ZaasClientException.class, () -> {
                    String invalidToken = "INVALID_TOKEN";
                    tokenService.query(invalidToken);
                });
            }

            @Test
            void givenExpiredToken() {
                assertThrows(ZaasClientException.class, () -> {
                    String expiredToken = getToken(now, expirationForExpiredToken, getDummyKey(configProperties));
                    tokenService.query(expiredToken);
                });
            }

            @Test
            void givenEmptyToken() {
                assertThrows(ZaasClientException.class, () -> {
                    String emptyToken = "";
                    tokenService.query(emptyToken);
                });
            }

        }
    }

    @Nested
    class WhenPassTicketRequested {
        @Test
        void givenValidToken_thenValidPassTicketIsReturned() throws ZaasClientException, ZaasConfigurationException {
            String token = tokenService.login(USERNAME, PASSWORD.toCharArray());
            String passTicket = tokenService.passTicket(token, APPLID);
            assertNotNull(passTicket);
            assertThat(token, is(not(EMPTY_STRING)));
        }

        @Nested
        class ProperExceptionIsRaised {

            @Test
            void givenInvalidToken() {
                assertThrows(ZaasClientException.class, () -> {
                    String invalidToken = "INVALID_TOKEN";
                    tokenService.passTicket(invalidToken, APPLID);
                });
            }

            @Test
            void givenEmptyToken() {
                assertThrows(ZaasClientException.class, () -> {
                    String emptyToken = "";
                    tokenService.passTicket(emptyToken, APPLID);
                });
            }

            @Test
            void givenValidTokenButInvalidApplicationId() throws ZaasClientException {
                String token = tokenService.login(USERNAME, PASSWORD.toCharArray());
                assertThrows(ZaasClientException.class, () -> {
                    String emptyApplicationId = "";
                    tokenService.passTicket(token, emptyApplicationId);
                });
            }

        }
    }

    @Nested
    class WhenLoggingOut {

        @Test
        void givenValidTokenBut_thenSuccess() throws ZaasClientException {
            String token = tokenService.login(USERNAME, PASSWORD.toCharArray());
            assertDoesNotThrow(() -> tokenService.logout(token));
        }

        @Test
        void givenInvalidTokenBut_thenExceptionIsThrown() {
            String token = "";
            assertThrows(ZaasClientException.class, () ->
                tokenService.logout(token));
        }
    }
}
