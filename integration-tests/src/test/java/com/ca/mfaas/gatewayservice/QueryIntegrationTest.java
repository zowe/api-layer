package com.ca.mfaas.gatewayservice;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

public class QueryIntegrationTest {
    private final static String USERNAME = "apimtst";
    private final static String PASSWORD = "password";
    private final static String QUERY_ENDPOINT = "/auth/query";
    private final static String DOMAIN = "gateway";
    private String token;

    @Before
    public void setUp() {
        RestAssured.port = 10010;
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @Test
    public void doQueryWithValidToken() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get(QUERY_ENDPOINT)
        .then()
            .statusCode(is(SC_OK))
            .body(
                "username", equalTo(USERNAME),
                "domain", equalTo(DOMAIN)
            );
    }

    @Test
    public void doQueryWithInvalidToken() {
        String invalidToken = "1234";
        String expectedMessage = "Token is not valid";

        given()
            .header("Authorization", "Bearer " + invalidToken)
        .when()
            .get(QUERY_ENDPOINT)
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_UNAUTHORIZED),
                "error.code", equalTo("SEC0004")
            );
    }

    @Test
    public void doQueryWithoutHeader() {
        String expectedMessage = "Token is blank or missing";

        given()
            .when()
            .get(QUERY_ENDPOINT)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_BAD_REQUEST),
                "error.code", equalTo("SEC0004")
            );
    }

    @Test
    public void doQueryWithWrongAuthType() {
        String expectedMessage = "Token format is wrong";

        given()
            .header("Authorization", "Basic " + token)
            .when()
            .get(QUERY_ENDPOINT)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_BAD_REQUEST),
                "error.code", equalTo("SEC0004")
            );
    }

    @Test
    public void doQueryWithEmptyHeader() {
        String emptyToken = " ";
        String expectedMessage = "Token format is wrong";

        given()
            .header("Authorization", "Bearer " + emptyToken)
            .when()
            .get(QUERY_ENDPOINT)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_BAD_REQUEST),
                "error.code", equalTo("SEC0004")
            );
    }
}
