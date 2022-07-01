/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication.pat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;

@InfinispanStorageTest
public class AccessTokenServiceTest {

    public static final URI REVOKE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.REVOKE_ACCESS_TOKEN);
    public static final URI REVOKE_FOR_USER_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.REVOKE_ACCESS_TOKENS_FOR_USER);
    public static final URI REVOKE_FOR_SCOPE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.REVOKE_ACCESS_TOKENS_FOR_SCOPE);
    public static final URI REVOKE_OWN_TOKENS_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.REVOKE_OWN_ACCESS_TOKENS);
    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_ACCESS_TOKEN);
    ValidateRequestModel bodyContent;

    @Nested
    class GivenUserCredentialsAsAuthTest {

        @BeforeEach
        void setup() throws Exception {
            SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
            RestAssured.useRelaxedHTTPSValidation();
            Set<String> scopes = new HashSet<>();
            scopes.add("service");
            String pat = SecurityUtils.personalAccessToken(scopes);
            bodyContent = new ValidateRequestModel();
            bodyContent.setServiceId("service");
            bodyContent.setToken(pat);
        }

        @Test
        void givenValidToken_invalidateTheToken() {
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .delete(REVOKE_ENDPOINT)
                .then().statusCode(200);
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(401);
        }

        @Test
        void givenTokenInvalidated_returnUnauthorized() {
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .delete(REVOKE_ENDPOINT)
                .then().statusCode(200);
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .delete(REVOKE_ENDPOINT)
                .then().statusCode(401);
        }

        @Test
        void givenMatchingScopes_validateTheToken() throws Exception {
            SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
            RestAssured.useRelaxedHTTPSValidation();
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
        }

        @Test
        void givenInvalidScopes_returnUnauthorized() throws Exception {
            SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
            RestAssured.useRelaxedHTTPSValidation();
            bodyContent.setServiceId("differentService");
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(401);
        }

    }

    @BeforeAll
    static void setupSsl() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class GivenClientCertAsAuthTest {

        @Test
        void givenAuthorizedRequest_thenRevokeTokenForUser() {
            Set<String> scopes = new HashSet<>();
            scopes.add("service");
            String pat = SecurityUtils.personalAccessToken(scopes);
            bodyContent = new ValidateRequestModel();
            bodyContent.setServiceId("service");
            bodyContent.setToken(pat);
//            validate before revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
//            revoke all tokens fro USERNAME
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("userId", SecurityUtils.USERNAME);
            given().contentType(ContentType.JSON).config(SslContext.clientCertUser).body(requestBody)
                .when().delete(REVOKE_FOR_USER_ENDPOINT)
                .then().statusCode(204);
//            validate after revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(401);
        }

        //
        @Test
        void givenAuthenticatedCall_thenRevokeUserToken() {
            String pat = SecurityUtils.personalAccessTokenWithClientCert(SslContext.clientCertValid);
            bodyContent = new ValidateRequestModel();
            bodyContent.setServiceId("service");
            bodyContent.setToken(pat);
//            validate before revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
//            revoke all tokens fro USERNAME
            given().contentType(ContentType.JSON).config(SslContext.clientCertValid)
                .when().delete(REVOKE_OWN_TOKENS_ENDPOINT)
                .then().statusCode(204);
//            validate after revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(401);
        }

        @Test
        void givenAuthorizedRequest_thenRevokeTokensForScope() {
            Set<String> scopes = new HashSet<>();
            scopes.add("gateway");
            scopes.add("api-catalog");
            String pat = SecurityUtils.personalAccessToken(scopes);
            bodyContent = new ValidateRequestModel();
            bodyContent.setServiceId("gateway");
            bodyContent.setToken(pat);
//            validate before revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
//            revoke all tokens fro USERNAME
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("serviceId", "api-catalog");
            given().contentType(ContentType.JSON).config(SslContext.clientCertUser).body(requestBody)
                .when().delete(REVOKE_FOR_SCOPE_ENDPOINT)
                .then().statusCode(204);
//            validate after revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(401);
        }

        @Test
        void givenNotAuthorizedCall_thenDontAllowToRevokeTokensForUser() {
            String pat = SecurityUtils.personalAccessTokenWithClientCert(SslContext.clientCertValid);
            bodyContent = new ValidateRequestModel();
            bodyContent.setServiceId("service");
            bodyContent.setToken(pat);
//            validate before revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
//            revoke all tokens fro USERNAME
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("userId", SecurityUtils.USERNAME);
            given().contentType(ContentType.JSON).config(SslContext.clientCertApiml).body(requestBody)
                .when().delete(REVOKE_FOR_USER_ENDPOINT)
                .then().statusCode(401);
//            validate after revocation rule
            given().contentType(ContentType.JSON).body(bodyContent).when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(200);
        }
    }


}
