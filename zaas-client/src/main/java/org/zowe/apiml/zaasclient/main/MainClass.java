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
import org.zowe.apiml.zaasclient.token.TokenService;
import org.zowe.apiml.zaasclient.token.TokenServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MainClass {

    private static final String VALID_USER = "user";
    private static final String VALID_PASS = "user";
    private static final String INAVLID_USER = "use";
    private static final String INVALID_PASS = "uer";
    private static final String NULL_USER = null;
    private static final String NULL_PASS = null;
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASS = "";
    private static final String EMPTY_AUTH_HEADER = "";
    private static final String NULL_AUTH_HEADER = null;

    public static void main(String[] args) throws IOException {
        ConfigProperties configProperties = new ConfigProperties();
        TokenServiceImpl tokenService = new TokenServiceImpl(configProperties);

        testLoginWithCredentials(tokenService);
        testLoginWithAuthHeader(tokenService);
    }

    private static void testLoginWithCredentials(TokenServiceImpl tokenService) {
        System.out.println("$$$$$$$$$$$$ START of Tests of LOGIN with CREDENTIALS $$$$$$$$$$$$\n\n");

        System.out.println("Please start the server, then type a character & press Enter to continue");
        Scanner sc = new Scanner(System.in);
        sc.next();

        executeCaseWithCredentials(tokenService, VALID_USER, VALID_PASS, "##### START OF POSITIVE CASE #####", "##### END OF POSITIVE CASE #####");
        executeCaseWithCredentials(tokenService, INAVLID_USER, VALID_PASS, "##### START OF NEGATIVE CASE 1 - Invalid Username #####", "##### END OF NEGATIVE CASE 1 #####");
        executeCaseWithCredentials(tokenService, VALID_USER, INVALID_PASS, "##### START OF NEGATIVE CASE 2 - Invalid Password #####", "##### END OF NEGATIVE CASE 2 #####");
        executeCaseWithCredentials(tokenService, EMPTY_USER, VALID_PASS, "##### START OF NEGATIVE CASE 3 - Empty Username #####", "##### END OF NEGATIVE CASE 3 #####");
        executeCaseWithCredentials(tokenService, VALID_USER, EMPTY_PASS, "##### START OF NEGATIVE CASE 4 - Empty Password #####", "##### END OF NEGATIVE CASE 4 #####");
        executeCaseWithCredentials(tokenService, NULL_USER, VALID_PASS, "##### START OF NEGATIVE CASE 5 - null Username #####", "##### END OF NEGATIVE CASE 5 #####");
        executeCaseWithCredentials(tokenService, VALID_USER, NULL_PASS, "##### START OF NEGATIVE CASE 6 - null Password #####", "##### END OF NEGATIVE CASE 6 #####");

        System.out.println("Please stop the server, then type a character & press Enter to continue");
        sc.next();

        executeCaseWithCredentials(tokenService, VALID_USER, VALID_PASS, "##### START OF NEGATIVE CASE 7 - Gateway Server Stopped/Unavailable #####", "##### END OF NEGATIVE CASE 7 #####");

        System.out.println("$$$$$$$$$$$$ END of Tests of LOGIN with CREDENTIALS $$$$$$$$$$$$\n\n");
    }

    private static void testLoginWithAuthHeader(TokenServiceImpl tokenService) {
        System.out.println("$$$$$$$$$$$$ START of Tests of LOGIN with AUTHORIZATION HEADER $$$$$$$$$$$$\n\n");

        System.out.println("Please start the server, then type a character & press Enter to continue");
        Scanner sc = new Scanner(System.in);
        sc.next();

        executeCaseWithAuthHeader(tokenService, VALID_USER, VALID_PASS, "##### START OF POSITIVE CASE #####", "##### END OF POSITIVE CASE #####");
        executeCaseWithAuthHeader(tokenService, INAVLID_USER, VALID_PASS, "##### START OF NEGATIVE CASE 1 - Invalid Username #####", "##### END OF NEGATIVE CASE 1 #####");
        executeCaseWithAuthHeader(tokenService, VALID_USER, INVALID_PASS, "##### START OF NEGATIVE CASE 2 - Invalid Password #####", "##### END OF NEGATIVE CASE 2 #####");
        executeCaseWithAuthHeader(tokenService, EMPTY_USER, VALID_PASS, "##### START OF NEGATIVE CASE 3 - Empty Username #####", "##### END OF NEGATIVE CASE 3 #####");
        executeCaseWithAuthHeader(tokenService, VALID_USER, EMPTY_PASS, "##### START OF NEGATIVE CASE 4 - Empty Password #####", "##### END OF NEGATIVE CASE 4 #####");
        executeCaseWithAuthHeader(tokenService, NULL_AUTH_HEADER, "##### START OF NEGATIVE CASE 5 - Null Auth Header #####", "##### END OF NEGATIVE CASE 5 #####");
        executeCaseWithAuthHeader(tokenService, NULL_AUTH_HEADER, "##### START OF NEGATIVE CASE 6 - Empty Auth Header #####", "##### END OF NEGATIVE CASE 6 #####");

        System.out.println("Please stop the server, then type a character & press Enter to continue");
        sc.next();

        executeCaseWithAuthHeader(tokenService, VALID_USER, VALID_PASS, "##### START OF NEGATIVE CASE 7 - Empty Password #####", "##### END OF NEGATIVE CASE 7 #####");

        System.out.println("$$$$$$$$$$$$ END of Tests of LOGIN with AUTHORIZATION HEADER $$$$$$$$$$$$\n\n");
    }

    private static void executeCaseWithCredentials(TokenService tokenService, String userName, String password, String caseStartMsg, String caseEndMsg) {
        try {
            System.out.println(caseStartMsg);
            System.out.println("Token obtained: " + tokenService.login(userName, password));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println(caseEndMsg + "\n\n");
        }
    }

    private static void executeCaseWithAuthHeader(TokenService tokenService, String userName, String password, String caseStartMsg, String caseEndMsg) {
        try {
            System.out.println(caseStartMsg);
            String auth = userName + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            System.out.println("Token obtained: " + tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println(caseEndMsg+"\n\n");
        }
    }

    private static void executeCaseWithAuthHeader(TokenService tokenService, String authHeader, String caseStartMsg, String caseEndMsg) {
        try {
            System.out.println(caseStartMsg);
            System.out.println("Token obtained: " + tokenService.login(authHeader));
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        } finally {
            System.out.println(caseEndMsg+"\n\n");
        }
    }
}
