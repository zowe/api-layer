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

//import org.apache.commons.codec.binary.Base64;
import org.zowe.apiml.util.config.ConfigReaderZaasClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.token.TokenService;
import org.zowe.apiml.zaasclient.token.TokenServiceImpl;

import org.junit.Before;
import org.junit.Test;
import org.zowe.apiml.util.config.ConfigReader;

public class LoginZaasClientIntegrationTest {

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private static final String INVALID_USER = "usr";
    private static final String INVALID_PASS = "usr";
    TokenService tokenService;

    @Before
    public void setUp() {
        ConfigProperties configProperties = ConfigReaderZaasClient.getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);
    }

    //@formatter:off
    @Test
    public void doLoginWithValidCredentials()  {
        try {
            String token = tokenService.login(USERNAME, PASSWORD);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }

    @Test
    public void doLoginWithInValidUsername()  {
        try {
            String token = tokenService.login(INVALID_USER, PASSWORD);
            System.out.println("Token obtained: " + token);
            tokenService.query(token);
        } catch (ZaasClientException zce) {
            System.out.println(zce.getErrorCode() + "\n" + zce.getErrorMessage() + "\n" + zce.getHttpResponseCode());
        }
    }



}
