/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.util.config.ConfigReader;

import java.util.*;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

public class ApiCatalogHttpHeadersIntegrationTest {

    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/ui/v1/apicatalog/#";
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private final static String COOKIE = "apimlAuthenticationToken";

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void verifyHttpHeaders() {
        String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Content-Type-Options","nosniff");
        expectedHeaders.put("X-XSS-Protection","1; mode=block");
        expectedHeaders.put("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
        expectedHeaders.put("Pragma","no-cache");
        expectedHeaders.put("Content-Type","text/html;charset=UTF-8");
        expectedHeaders.put("Transfer-Encoding","chunked");

        List<String> forbiddenHeaders = new ArrayList<>();
        forbiddenHeaders.add("X-Frame-Options");
        forbiddenHeaders.add("Strict-Transport-Security");

        Response response =  RestAssured.given().cookie(COOKIE, token)
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, GET_ALL_CONTAINERS_ENDPOINT));
        Map<String,String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(h -> responseHeaders.put(h.getName(),h.getValue()));

        expectedHeaders.entrySet().forEach(h -> assertThat(responseHeaders, hasEntry(h.getKey(),h.getValue())));
        forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));

    }
}
