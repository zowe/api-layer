/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.main;

import org.apache.commons.codec.binary.Base64;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.token.TokenServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MainClass {

    private static final String VALID_USER = "user";
    private static final String VALID_PASS = "user";
    private static final String INAVLID_USER = "use";
    private static final String INVALID_PASS = "uer";
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASS = "";

    public static void main(String[] args) throws IOException {
        ConfigProperties configProperties = new ConfigProperties();
        TokenServiceImpl tokenService = new TokenServiceImpl(configProperties);

        testLoginWithCredentials(tokenService);
        testLoginWithAuthHeader(tokenService);
    }

    private static void testLoginWithCredentials(TokenServiceImpl tokenService) {
        System.out.println("$$$$$$$$$$$$ Test of LOGIN with CREDENTIALS $$$$$$$$$$$$\n\n" +
            "Please start the server, then type a character & press Enter to continue");
        Scanner sc = new Scanner(System.in);
        sc.next();

        try {
            System.out.println("##### START OF POSITIVE CASE #####");
            System.out.println(tokenService.login(null, null));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF POSITIVE CASE #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 1 - Invalid Username #####");
            System.out.println(tokenService.login(INAVLID_USER, VALID_PASS));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 1 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 2 - Invalid Password #####");
            System.out.println(tokenService.login(VALID_USER, INVALID_PASS));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 2 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 3 - Empty Username #####");
            System.out.println(tokenService.login(EMPTY_USER, VALID_PASS));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 3 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 4 - Empty Password #####");
            System.out.println(tokenService.login(VALID_USER, EMPTY_PASS));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 4 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 5 - Empty Password #####");
            System.out.println("Please stop the server, then type a character & press Enter to continue");
            sc.next();
            System.out.println(tokenService.login(VALID_USER, VALID_PASS));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 5 #####\n\n");
        }
    }

    private static void testLoginWithAuthHeader(TokenServiceImpl tokenService) {
        System.out.println("$$$$$$$$$$$$ Test of LOGIN with AUTHORIZATION HEADER $$$$$$$$$$$$\n\n" +
            "Please start the server, then type a character & press Enter to continue");
        Scanner sc = new Scanner(System.in);
        sc.next();

        try {
            System.out.println("##### START OF POSITIVE CASE #####");
            String auth = VALID_USER + ":" + VALID_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println(tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF POSITIVE CASE #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 1 - Invalid Username #####");
            String auth = INAVLID_USER + ":" + VALID_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println(tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 1 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 2 - Invalid Password #####");
            String auth = VALID_USER + ":" + INVALID_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println(tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 2 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 3 - Empty Username #####");
            String auth = EMPTY_USER + ":" + VALID_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println(tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 3 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 4 - Empty Password #####");
            String auth = VALID_USER + ":" + EMPTY_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println(tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 4 #####\n\n");
        }

        try {
            System.out.println("##### NEGATIVE CASE 5 - Empty Password #####");
            System.out.println("Please stop the server, then type a character & press Enter to continue");
            sc.next();
            String auth = VALID_USER + ":" + VALID_PASS;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println(tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println("##### END OF NEGATIVE CASE 5 #####\n\n");
        }
    }
}
