/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.oauth2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.integration.authentication.pat.ValidateRequestModel;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class OktaOauth2Test {

    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_OIDC_TOKEN);

    @Test
    @Tag("OktaOauth2Test")
    void givenValidAccessToken_thenValidate() {
        String username = System.getProperty("okta.client.id");
        String password = System.getProperty("okta.client.password");
        Assertions.assertNotNull(username);
        Assertions.assertNotNull(password);
        Map<String, String> headers = new HashMap<>();
        String creds = username + ":" + password;
        byte[] base64encoded = Base64.getEncoder().encode(creds.getBytes());
        headers.put("authorization", "Basic " + new String(base64encoded));
        headers.put("content-type", "application/x-www-form-urlencoded");
        headers.put("accepts", "application/json");
        RestAssured.useRelaxedHTTPSValidation();
        Object accessToken = given().port(443).headers(headers).when().post("https://dev-95727686.okta.com:443/oauth2/default/v1/token?grant_type=client_credentials&scope=customScope")
            .then().statusCode(200).extract().body().path("access_token");
        if (accessToken instanceof String) {
            String token = (String) accessToken;
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(token);
            given().contentType(ContentType.JSON).body(requestBody).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
        } else {
            throw new RuntimeException("Incorrect format of response from authorization server.");
        }
    }

}