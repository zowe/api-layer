/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.Cookie;
import io.restassured.response.ValidatableResponse;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.categories.AuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;

import javax.net.ssl.SSLContext;
import java.net.Socket;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.zowe.apiml.gatewayservice.SecurityUtils.logoutItUserGatewayZosmf;

@AuthenticationTest
abstract class Login {
    protected final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    protected final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    protected final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    protected final static String BASE_PATH = "/api/v1/gateway";
    protected static String authenticationEndpointPath = String.format("%s://%s:%d%s/authentication", SCHEME, HOST, PORT, BASE_PATH);
    protected static AuthenticationProviders providers = new AuthenticationProviders(authenticationEndpointPath);
    protected final static String LOGIN_ENDPOINT = "/auth/login";
    public static final String LOGIN_ENDPOINT_URL = String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT);
    protected final static String COOKIE_NAME = "apimlAuthenticationToken";

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    public static final char[] KEYSTORE_PASSWORD = ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStorePassword();
    public static final String KEYSTORE_LOCALHOST_TEST_JKS = ConfigReader.environmentConfiguration().getTlsConfiguration().getClientKeystore();


    protected String getUsername() {
        return USERNAME;
    }

    protected String getPassword() {
        return PASSWORD;
    }

    protected static RestAssuredConfig clientCertValid;
    protected static RestAssuredConfig clientCertApiml;
    protected static RestAssuredConfig tlsWithoutCert;

    @BeforeAll
    static void prepareSslAuthentication() throws Exception {
        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "apimtst")
            .loadTrustMaterial(null, trustStrategy)
            .build();
        clientCertValid = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext)));

        SSLContext sslContext2 = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStore()),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD)
            .loadTrustMaterial(null, trustStrategy)
            .build();
        clientCertApiml = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext2)));

        SSLContext sslContext3 = SSLContextBuilder
            .create()
            .loadTrustMaterial(null, trustStrategy)
            .build();
        tlsWithoutCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext3)));
    }

    @AfterAll
    static void switchToOriginalProvider() {
        providers.switchProvider(null);
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    void givenValidCredentialsInBody_whenUserAuthenticates_thenTheValidTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(LOGIN_ENDPOINT_URL)
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie);

        logout(cookie.getValue());
    }

    protected void assertValidAuthToken(Cookie cookie) {
        assertValidAuthToken(cookie, Optional.empty());
    }

    protected void assertValidAuthToken(Cookie cookie, Optional<String> username) {
        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims, username);
    }

    @Test
    void givenValidCredentialsInHeader_whenUserAuthenticates_thenTheValidTokenIsProduced() {
        String token = given()
            .auth().preemptive().basic(getUsername(), getPassword())
            .contentType(JSON)
            .when()
            .post(LOGIN_ENDPOINT_URL)
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);

        int i = token.lastIndexOf('.');
        String untrustedJwtString = token.substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims);

        logout(token);
    }

    protected void assertThatTokenIsValid(Claims claims) {
        assertThatTokenIsValid(claims, Optional.empty());
    }

    protected void assertThatTokenIsValid(Claims claims, Optional<String> username) {
        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(username.orElseGet(this::getUsername)));
    }

    protected Claims parseJwtString(String untrustedJwtString) {
        return Jwts.parserBuilder().build()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();
    }

    @Test
    void givenInvalidCredentialsInBody_whenUserAuthenticates_thenUnauthorizedIsReturned() {
        String expectedMessage = "Invalid username or password for URL '" + BASE_PATH + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(LOGIN_ENDPOINT_URL)
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenInvalidCredentialsInHeader_whenUserAuthenticates_thenUnauthorizedIsReturned() {
        String expectedMessage = "Invalid username or password for URL '" + BASE_PATH + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        ValidatableResponse response = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(LOGIN_ENDPOINT_URL)
            .then();
        response.statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenNoCredentials_whenUserAuthenticates_then400IsReturned() {
        String expectedMessage = "Authorization header is missing, or the request body is missing or invalid for URL '" +
            BASE_PATH + LOGIN_ENDPOINT + "'";

        given()
            .when()
            .post(LOGIN_ENDPOINT_URL)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenCredentialsInTheWrongJsonFormat_whenUserAuthenticates_then400IsReturned() {
        String expectedMessage = "Authorization header is missing, or the request body is missing or invalid for URL '" +
            BASE_PATH + LOGIN_ENDPOINT + "'";

        JSONObject loginRequest = new JSONObject()
            .put("user", getUsername())
            .put("pass", getPassword());

        given()
            .contentType(JSON)
            .body(loginRequest.toString())
            .when()
            .post(LOGIN_ENDPOINT_URL)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenValidCredentialsInJsonBody_whenUserAuthenticatesViaGetMethod_then405IsReturned() {
        String expectedMessage = "Authentication method 'GET' is not supported for URL '" +
            BASE_PATH + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .get(LOGIN_ENDPOINT_URL)
            .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
            );
    }

    protected String authenticateAndVerify(LoginRequest loginRequest) {
        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims);

        return cookie.getValue();
    }

    @Test
    void givenClientX509Cert_whenUserAuthenticates_thenTheValidTokenIsProduced() throws Exception {

        Cookie cookie = given().config(clientCertValid)
            .post(new URI(LOGIN_ENDPOINT_URL))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie, Optional.of("APIMTST"));

        logout(cookie.getValue());
    }

    @Test
    void givenValidClientCertAndInvalidBasic_whenAuth_thenCertShouldTakePrecedenceAndTokenIsProduced() throws Exception {
        Cookie cookie = given().config(clientCertValid)
            .auth().basic("Bob", "The Builder")
            .post(new URI(LOGIN_ENDPOINT_URL))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie, Optional.of("APIMTST"));

        logout(cookie.getValue());
    }

    @Test
    void givenApimlsCert_whenAuth_thenUnauthorized() throws Exception {
        given().config(clientCertApiml)
            .post(new URI(LOGIN_ENDPOINT_URL))
            .then()
            .statusCode(is(SC_BAD_REQUEST));
    }

    protected void logout(String jwtToken) {
        logoutItUserGatewayZosmf(jwtToken);
    }
    //@formatter:on
}
