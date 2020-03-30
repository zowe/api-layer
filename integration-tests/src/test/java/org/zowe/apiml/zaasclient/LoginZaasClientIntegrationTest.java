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
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.token.TokenService;
import org.zowe.apiml.zaasclient.token.TokenServiceImpl;

import org.junit.Before;
import org.junit.Test;
import org.zowe.apiml.util.config.ConfigReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    TokenService tokenService;

    @Before
    public void setUp() throws IOException {
        ConfigProperties configProperties = ConfigReaderZaasClient.getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);
    }

    //@formatter:off
    @Test
    public void doLoginWithValidCredentials() {
        try {
            String token = tokenService.login(USERNAME, PASSWORD);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithInValidPassword() {
        try {
            String token = tokenService.login(INVALID_USER, PASSWORD);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithInValidUsername() {
        try {
            String token = tokenService.login(USERNAME, INVALID_PASS);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithNullUser() {
        try {
            String token = tokenService.login(NULL_USER, PASSWORD);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithNullPassword() {
        try {
            String token = tokenService.login(USERNAME, NULL_PASS);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithEmptyUser() {
        try {
            String token = tokenService.login(EMPTY_USER, PASSWORD);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithEmptyPassword() {
        try {
            String token = tokenService.login(USERNAME, EMPTY_PASS);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderValidCredentials() {
        try {
            String auth = USERNAME + ":" + PASSWORD;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderValidUsername() {
        try {
            String auth = USERNAME + ":" + INVALID_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }


    @Test
    public void doLoginWithAuthHeaderValidPassWord() {
        try {
            String auth = INVALID_USER + ":" + PASSWORD;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderNullUserName() {
        try {
            String auth = NULL_USER + ":" + PASSWORD;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }


    @Test
    public void doLoginWithAuthHeaderNullPassword() {
        try {
            String auth = USERNAME + ":" + NULL_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderEmptyUsername() {
        try {
            String auth = EMPTY_USER + ":" + PASSWORD;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthHeaderEmptyPassword() {
        try {
            String auth = USERNAME + ":" + EMPTY_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            String token = tokenService.login(authHeader);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthNullHeader() {
        try {
            System.out.println("Token obtained: " + tokenService.login(NULL_AUTH_HEADER));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithAuthEmptyHeader() {
        try {
            System.out.println("Token obtained: " + tokenService.login(EMPTY_AUTH_HEADER));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }
}


