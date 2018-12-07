package com.ca.mfaas.gatewayservice;

import io.apiml.security.gateway.login.GatewayLoginRequest;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecurityUtils {
    private final static String TOKEN = "apimlAuthenticationToken";
    private final static String LOGIN_ENDPOINT = "/auth/login";


    public static String gatewayToken(String username, String password) {
        GatewayLoginRequest loginRequest = new GatewayLoginRequest(username, password);

        String token = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(LOGIN_ENDPOINT)
            .then()
            .statusCode(is(SC_OK))
            .cookie(TOKEN, not(isEmptyString()))
            .body(
                TOKEN, not(isEmptyString())
            )
            .extract().
                path(TOKEN);

        return token;
    }
}
