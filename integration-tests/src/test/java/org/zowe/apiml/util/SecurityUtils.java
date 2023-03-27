/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;


import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import com.nimbusds.jose.util.Base64;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.gateway.security.login.SuccessfulAccessTokenHandler;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.TlsConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.zowe.apiml.util.requests.Endpoints.*;

public class SecurityUtils {
    public final static String GATEWAY_TOKEN_COOKIE_NAME = "apimlAuthenticationToken";

    private final static GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

    private final static String gatewayScheme = serviceConfiguration.getScheme();
    private final static String gatewayHost = serviceConfiguration.getHost();
    private final static int gatewayPort = serviceConfiguration.getPort();

    public final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    public final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();

    public final static String OKTA_HOSTNAME = ConfigReader.environmentConfiguration().getIdpConfiguration().getHost();
    public final static String OKTA_CLIENT_ID = ConfigReader.environmentConfiguration().getIdpConfiguration().getClientId();
    public final static String OKTA_USER = ConfigReader.environmentConfiguration().getIdpConfiguration().getUser();
    public final static String OKTA_PASSWORD = ConfigReader.environmentConfiguration().getIdpConfiguration().getPassword();

    public final static String COOKIE_NAME = "apimlAuthenticationToken";
    public static final String PAT_COOKIE_AUTH_NAME = "personalAccessToken";

    protected static String getUsername() {
        return USERNAME;
    }


    //@formatter:off

    public static String getGatewayUrl(String path) {
        return getGatewayUrl(path, gatewayPort);
    }

    public static String getGatewayUrl(String path, int port) {
        return String.format("%s://%s:%d%s", gatewayScheme, gatewayHost, port, path);
    }

    public static String getGatewayLogoutUrl(String path) {
        return getGatewayUrl(path);
    }

    public static String gatewayToken() {
        return gatewayToken(USERNAME, PASSWORD);
    }

    public static String gatewayToken(URI gatewayLoginEndpoint) {
        return gatewayToken(gatewayLoginEndpoint, USERNAME, PASSWORD);
    }

    public static String gatewayToken(String username, String password) {
        return gatewayToken(HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN), username, password);
    }

    public static String gatewayToken(URI gatewayLoginEndpoint, String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        SSLConfig originalConfig = RestAssured.config().getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        String cookie = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(gatewayLoginEndpoint)
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(GATEWAY_TOKEN_COOKIE_NAME);

        RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        return cookie;
    }

    public static String personalAccessToken(Set<String> scopes) {
        URI gatewayGenerateAccessTokenEndpoint = HttpRequestUtils.getUriFromGateway(GENERATE_ACCESS_TOKEN);
        SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = new SuccessfulAccessTokenHandler.AccessTokenRequest(60, scopes);

        SSLConfig originalConfig = RestAssured.config().getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        String token = given()
            .contentType(JSON).header("Authorization", "Basic " + Base64.encode(USERNAME + ":" + PASSWORD))
            .body(accessTokenRequest)
            .when()
            .post(gatewayGenerateAccessTokenEndpoint)
            .then()
            .statusCode(is(SC_OK))
            .extract().body().asString();

        RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        return token;
    }

    public static String personalAccessTokenWithClientCert(RestAssuredConfig sslConfig) {
        URI gatewayGenerateAccessTokenEndpoint = HttpRequestUtils.getUriFromGateway(GENERATE_ACCESS_TOKEN);
        Set<String> scopes = new HashSet<>();
        scopes.add("service");

        SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = new SuccessfulAccessTokenHandler.AccessTokenRequest(60, scopes);
        SSLConfig originalConfig = RestAssured.config().getSSLConfig();

        String token = given().config(sslConfig)
            .body(accessTokenRequest)
            .when()
            .post(gatewayGenerateAccessTokenEndpoint)
            .then()
            .statusCode(is(SC_OK))
            .extract().body().asString();

        RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        return token;
    }

    public static String validOktaAccessToken() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("username", OKTA_USER);
        requestBody.put("password", OKTA_PASSWORD);
        String sessionToken = given()
            .contentType(JSON)
            .body(requestBody.toString())
            .when()
            .post(OKTA_HOSTNAME + "/api/v1/authn")
            .then()
            .statusCode(200)
            .extract().path("sessionToken");

        assertNotNull(sessionToken, "Failed to get session token from Okta authentication.");

        // retrieve the access token from Okta using session token
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("client_id", OKTA_CLIENT_ID);
        queryParams.put("redirect_uri", "https://localhost:10010/login/oauth2/code/okta");
        queryParams.put("response_type", "token");
        queryParams.put("response_mode", "form_post");
        queryParams.put("sessionToken", sessionToken);
        queryParams.put("scope", "openid");
        queryParams.put("state", "TEST");
        queryParams.put("nonce", "TEST");
        Response authResponse = given()
            .queryParams(queryParams)
            .when()
            .get(OKTA_HOSTNAME + "/oauth2/default/v1/authorize")
            .then()
            .statusCode(200)
            .extract().response();

        // The response is HTML form where access token is hidden input field (this is controlled by response_mode = form_post)

        String body = authResponse.getBody().asString();
        String accessToken = StringUtils.substringBetween(body, "name=\"access_token\" value=\"", "\"/>");
        assertNotNull(accessToken, "Failed to locate access token in the Okta /authorize response.");
        return accessToken;
    }

    public static String expiredOktaAccessToken() {
        return "eyJraWQiOiJGTUM5UndncFVJMUt0V25QWkdmVmFKYzZUZGlTTElZU29jeWs4aHlEbE44IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULkVzZ051RGxkcm5FN0VDYlhnNUhEdUY4MW9BV3k1UDF4WUZLT1psTmVJcmMiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2Njc5MTc5MjcsImV4cCI6MTY2NzkyMTUyNywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU3NmVvZjB6bnNNYkY3NDVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjY3OTE3ODg2LCJzdWIiOiJpdF90ZXN0QGFjbWUuY29tIiwiZ3JvdXBzIjpbIkV2ZXJ5b25lIl19.KiPa0c1U5IClozwZI5aDRSwjoi-hYtIkQZWpizGF8PPsgzvfMaivUzMoPi5GfEUZF6Bjlg_fQFUK7kJQ8NWjL6gY_5QQMfONw0U9dzQy2HLHb5gU55IKt6mBIutBSPk2FmCTd4SaPmllMb6nAyhIZf0DI7xuAXqRgt5JnasnmCKSIM3HJMlTeXDzHQ5BvMr7tVHWmwQ-8W3nef5nsKi2Sw05rds9RgkcckGUzhA2tMeF_rVTitufeG7h2oXYICtv60wfK6YSnmE78aoHf5NQD5517gnGrRxGMM6UAn3SV4GKOll6OlGDzpz87mq-AR2tigkDfVcOtJA9mkxFFv7HSg";
    }

    public static void logoutOnGateway(String url, String jwtToken) {
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwtToken)
            .when()
            .post(url)
            .then()
            .statusCode(is(SC_NO_CONTENT));
    }

    public static SSLConfig getConfiguredSslConfig() {
        TlsConfiguration tlsConfiguration = ConfigReader.environmentConfiguration().getTlsConfiguration();
        try {
            SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(
                    new File(tlsConfiguration.getKeyStore()),
                    tlsConfiguration.getKeyStorePassword(),
                    tlsConfiguration.getKeyPassword(),
                    (aliases, socket) -> tlsConfiguration.getKeyAlias())
                .loadTrustMaterial(
                    new File(tlsConfiguration.getTrustStore()),
                    tlsConfiguration.getTrustStorePassword())
                .build();
            HostnameVerifier hostnameVerifier = tlsConfiguration.isNonStrictVerifySslCertificatesOfServices() ? new NoopHostnameVerifier() : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
            SSLSocketFactoryAdapter sslSocketFactory = new SSLSocketFactoryAdapter(new SSLConnectionSocketFactory(sslContext, hostnameVerifier));
            return SSLConfig.sslConfig().with().sslSocketFactory(sslSocketFactory);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
            | CertificateException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void assertIfLogged(String jwt, boolean logged) {
        final HttpStatus status = logged ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .when()
            .get(HttpRequestUtils.getUriFromGateway(ROUTED_QUERY))
            .then()
            .statusCode(status.value());
    }

    public static void assertLogout(String url, String jwtToken, int expectedStatusCode) {
        given()
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(url)
            .then()
            .statusCode(is(expectedStatusCode));
    }

    public static void assertValidAuthToken(Cookie cookie) {
        assertValidAuthToken(cookie, Optional.empty());
    }

    public static void assertValidAuthToken(Cookie cookie, Optional<String> username) {
        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims, username);
    }

    public static void assertThatTokenIsValid(Claims claims) {
        assertThatTokenIsValid(claims, Optional.empty());
    }

    public static void assertThatTokenIsValid(Claims claims, Optional<String> username) {
        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(username.orElseGet(SecurityUtils::getUsername)));
    }

    public static Claims parseJwtString(String untrustedJwtString) {
        return Jwts.parserBuilder().build()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();
    }

    //@formatter:on
}
