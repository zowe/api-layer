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
import org.zowe.apiml.util.config.ConfigReaderZaasClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.token.TokenService;
import org.zowe.apiml.zaasclient.token.TokenServiceImpl;

import org.junit.Before;
import org.junit.Test;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.zaasclient.token.ZaasToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

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
    TokenService tokenService;

    private String getAuthHeader(String userName, String password) {
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

    @Before
    public void setUp() throws IOException {
        configProperties = ConfigReaderZaasClient.getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);
    }

    @Test
    public void doLoginWithValidCredentials() {
        try {
            String token = tokenService.login(USERNAME, PASSWORD);
            assertNotNull("null Token obtained", token);
            assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
        } catch (ZaasClientException zce) {
            fail("Test case failed as it threw an exception");
        }
    }

    @Test
    public void doLoginWithInValidUsername() {
        try {
            tokenService.login(INVALID_USER, PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithInValidPassword() {
        try {
            tokenService.login(USERNAME, INVALID_PASS);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithNullUser() {
        try {
            tokenService.login(NULL_USER, PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithNullPassword() {
        try {
            tokenService.login(USERNAME, NULL_PASS);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithEmptyUser() {
        try {
            tokenService.login(EMPTY_USER, PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithEmptyPassword() {
        try {
            tokenService.login(USERNAME, EMPTY_PASS);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderValidCredentials() {
        try {
            String token = tokenService.login(getAuthHeader(USERNAME, PASSWORD));
            assertNotNull("null Token obtained", token);
            assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
        } catch (ZaasClientException zce) {
            fail("Test case failed as it threw an exception");
        }
    }

    @Test
    public void doLoginWithAuthHeaderValidUsername() {
        try {
            String token = tokenService.login(getAuthHeader(USERNAME, INVALID_PASS));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }


    @Test
    public void doLoginWithAuthHeaderValidPassWord() {
        try {
            String token = tokenService.login(getAuthHeader(INVALID_USER, PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderNullUserName() {
        try {
            String token = tokenService.login(getAuthHeader(NULL_USER, PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }


    @Test
    public void doLoginWithAuthHeaderNullPassword() {
        try {
            tokenService.login(getAuthHeader(USERNAME, NULL_PASS));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderEmptyUsername() {
        try {
            tokenService.login(getAuthHeader(EMPTY_USER, PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderEmptyPassword() {
        try {
            tokenService.login(getAuthHeader(USERNAME, EMPTY_PASS));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthNullHeader() {
        try {
            tokenService.login(NULL_AUTH_HEADER);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthEmptyHeader() {
        try {
            tokenService.login(EMPTY_AUTH_HEADER);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testQueryWithValidToken() {
        try {
            String token = tokenService.login(USERNAME, PASSWORD);
            ZaasToken zaasToken = tokenService.query(token);
            assertNotNull(zaasToken);
            assertEquals("Username Mismatch", USERNAME, zaasToken.getUserId());
            assertEquals("Token Expired", Boolean.FALSE, zaasToken.isExpired());
        } catch (ZaasClientException e) {
            fail("Test case failed as it threw an exception");
        }
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithInvalidToken() throws ZaasClientException {
            String token = tokenService.login(USERNAME, PASSWORD);
            String invalidToken = token + "INVALID";
            ZaasToken zaasToken = tokenService.query(invalidToken);
            fail("Test case failed as it didn't throw an exception");
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithExpiredToken() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ZaasClientException {
            String expiredToken = getToken(now, expirationForExpiredToken, getDummyKey(configProperties));
            ZaasToken zaasToken = tokenService.query(expiredToken);
            fail("Test case failed as it didn't throw an exception");
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithEmptyToken() throws ZaasClientException {
        String emptyToken = "";
        ZaasToken zaasToken = tokenService.query(emptyToken);
        fail("Test case failed as it didn't throw an exception");
    }

    @Test
    public void testPassTicketWithValidToken() {
        try {
            String token = tokenService.login(USERNAME, PASSWORD);
            String passTicket = tokenService.passTicket(token, "ZOWEAPPL");
            assertNotNull(passTicket);
            assertNotEquals("Empty PassTicket obtained", EMPTY_STRING, token);
        } catch (ZaasClientException e) {
            fail("Test case failed as it threw an exception");
        }
    }

    @Test(expected = ZaasClientException.class)
    public void testPassTicketWithInvalidToken() throws ZaasClientException {
        String token = tokenService.login(USERNAME, PASSWORD);
        String invalidToken = token + "INVALID";
        tokenService.passTicket(invalidToken, "ZOWEAPPL");
    }

    @Test(expected = ZaasClientException.class)
    public void testPassTicketWithEmptyToken() throws ZaasClientException {
        String emptyToken = "";
        tokenService.passTicket(emptyToken, "ZOWEAPPL");
    }

    @Test(expected = ZaasClientException.class)
    public void testPassTicketWithEmptyApplicationID() throws ZaasClientException {
        String token = tokenService.login(USERNAME, PASSWORD);
        String emptyApplicationId = "";
        tokenService.passTicket(token, emptyApplicationId);
    }
}


