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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class LoginZaasClientIntegrationTest {

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

    TokenService tokenService;

    private String getAuthHeader(String userName, String password) {
        String auth = userName + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
            auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    @Before
    public void setUp() throws IOException {
        ConfigProperties configProperties = ConfigReaderZaasClient.getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);
    }

    @Test
    public void doLoginWithValidCredentials() {
        try {
            String token = tokenService.login(USERNAME, PASSWORD);
            assertNotNull("null Token obtained", token);
            assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
            assertEquals("Token Mismatch","token", token);
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
            assertEquals("Token Mismatch","token", token);
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
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderEmptyPassword() {
        try {
            tokenService.login(getAuthHeader(USERNAME, EMPTY_PASS));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
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
    public void testLoginWithAuthHeader_ServerUnavailable() {
        try {
            tokenService.login(getAuthHeader(USERNAME, PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_GenericException() {
        try {
            tokenService.login(getAuthHeader(USERNAME, PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

}


