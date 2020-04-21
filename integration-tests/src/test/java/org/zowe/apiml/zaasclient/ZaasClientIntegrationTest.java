/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ConfigReaderZaasClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.internal.ZaasClientHttps;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ZaasClientIntegrationTest {

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private static final String INVALID_USER = "usr";
    private static final String INVALID_PASS = "usr";
    private static final String NULL_USER = null;
    private static final String NULL_PASS = null;
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASS = "";
    private static final String NULL_AUTH_HEADER = null;
    private static final String EMPTY_AUTH_HEADER = "";
    private static final String EMPTY_STRING = "";

    private long now = System.currentTimeMillis();
    private long expirationForExpiredToken = now - 1000;

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
            .signWith(SignatureAlgorithm.RS256, jwtSecretKey)
            .compact();
    }

    private Key getDummyKey(ConfigProperties configProperties) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        InputStream inputStream;

        KeyStore ks = KeyStore.getInstance(configProperties.getKeyStoreType());

        File keyStoreFile = new File(configProperties.getKeyStorePath());
        inputStream = new FileInputStream(keyStoreFile);
        ks.load(inputStream, configProperties.getKeyStorePassword() == null ? null : configProperties.getKeyStorePassword().toCharArray());

        return ks.getKey("jwtsecret",
            configProperties.getKeyStorePassword() == null ? null : configProperties.getKeyStorePassword().toCharArray());
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        assertThat(code.getId(), is(zce.getErrorCode()));
        assertThat( code.getMessage(), is(zce.getErrorMessage()));
        assertThat(code.getReturnCode(), is(zce.getHttpResponseCode()));
    }

    @BeforeEach
    public void setUp() throws Exception {
        configProperties = ConfigReaderZaasClient.getConfigProperties();
        tokenService = new ZaasClientHttps(configProperties);
    }

    @Test
    public void givenValidCredentials_whenUserLogsIn_thenValidTokenIsObtained() throws ZaasClientException {
        String token = tokenService.login(USERNAME, PASSWORD);
        assertNotNull(token);
        assertThat(token, is(not(EMPTY_STRING)));
    }

    private static Stream<Arguments> provideInvalidUsernamePassword() {
        return Stream.of(
            Arguments.of(INVALID_USER, PASSWORD, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(USERNAME, INVALID_PASS, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(NULL_USER, PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(USERNAME, NULL_PASS, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(EMPTY_USER, PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsernamePassword")
    public void giveInvalidCredentials_whenLoginIsRequested_thenProperExceptionIsRaised(String username, String password, ZaasClientErrorCodes expectedCode) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(username, password));

        assertThatExceptionContainValidCode(exception, expectedCode);
    }

    private static Stream<Arguments> provideInvalidAuthHeaders() {
        return Stream.of(
            Arguments.of(getAuthHeader(INVALID_USER, PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(USERNAME, INVALID_PASS), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(NULL_USER, PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(USERNAME, NULL_PASS), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(getAuthHeader(EMPTY_USER, PASSWORD), ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(getAuthHeader(USERNAME, EMPTY_PASS), ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(NULL_AUTH_HEADER, ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER),
            Arguments.of(EMPTY_AUTH_HEADER, ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAuthHeaders")
    public void doLoginWithAuthHeaderInValidUsername(String authHeader, ZaasClientErrorCodes expectedCode) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(authHeader));

        assertThatExceptionContainValidCode(exception, expectedCode);
    }

    @Test
    public void givenValidCredentials_whenUserLogsIn_thenValidTokenIsReceived() throws ZaasClientException {
        String token = tokenService.login(getAuthHeader(USERNAME, PASSWORD));
        assertNotNull(token);
        assertThat(token, is(not(EMPTY_STRING)));
    }

    @Test
    public void givenValidToken_whenQueriedForDetails_thenValidDetailsAreProvided() throws ZaasClientException {
        String token = tokenService.login(USERNAME, PASSWORD);
        ZaasToken zaasToken = tokenService.query(token);
        assertNotNull(zaasToken);
        assertThat(zaasToken.getUserId(), is(USERNAME));
        assertThat(zaasToken.isExpired(), is(Boolean.FALSE));
    }

    @Test
    public void givenInvalidToken_whenQueriedForDetails_thenExceptionIsThrown() {
        assertThrows(ZaasClientException.class, () -> {
            String invalidToken = "INVALID_TOKEN";
            tokenService.query(invalidToken);
        });
    }

    @Test
    public void givenExpiredToken_whenQueriedForDetails_thenExceptionIsThrown() {
        assertThrows(ZaasClientException.class, () -> {
            String expiredToken = getToken(now, expirationForExpiredToken, getDummyKey(configProperties));
            tokenService.query(expiredToken);
        });
    }

    @Test
    public void givenEmptyToken_whenDetailsAboutTheTokenAreRequested_thenTheExceptionIsThrown() {
        assertThrows(ZaasClientException.class, () -> {
            String emptyToken = "";
            tokenService.query(emptyToken);
        });
    }

    @Test
    public void givenValidTicket_whenPassTicketIsRequested_thenValidPassTicketIsReturned() throws ZaasClientException, ZaasConfigurationException {
        String token = tokenService.login(USERNAME, PASSWORD);
        String passTicket = tokenService.passTicket(token, "ZOWEAPPL");
        assertNotNull(passTicket);
        assertThat(token, is(not(EMPTY_STRING)));
    }

    @Test
    public void givenInvalidToken_whenPassTicketIsRequested_thenExceptionIsThrown()  {
        assertThrows(ZaasClientException.class, () -> {
            String invalidToken = "INVALID_TOKEN";
            tokenService.passTicket(invalidToken, "ZOWEAPPL");
        });
    }

    @Test
    public void givenEmptyToken_whenPassTicketIsRequested_thenExceptionIsThrown() {
        assertThrows(ZaasClientException.class, () -> {
            String emptyToken = "";
            tokenService.passTicket(emptyToken, "ZOWEAPPL");
        });
    }

    @Test
    public void givenValidTokenButInvalidApplicationId_whenPassTicketIsRequested_thenExceptionIsThrown() {
        assertThrows(ZaasClientException.class, () -> {
            String token = tokenService.login(USERNAME, PASSWORD);
            String emptyApplicationId = "";
            tokenService.passTicket(token, emptyApplicationId);
        });
    }
}
