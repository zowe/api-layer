/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.restassured.RestAssured;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithBasePath;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test to verify authentication chain with Personal Access Tokens (reads body)
 */
@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.personalAccessToken.enabled=true"
})
@ActiveProfiles("test")
class AccessTokenAuthTest extends AcceptanceTestWithBasePath {

    @BeforeEach
    void setup() {

    }

    @Nested
    class GivenPersonalAccessToken {

        @Nested
        class WhenCorrectCredentials {

            @Test
            void whenCreateToken_withChunked_thenSucceed() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                headers.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjp1c2Vy");

                HttpClient client = HttpClients
                    .custom()
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

                PatRequest request = new PatRequest(Collections.singletonList("gateway"));

                assertDoesNotThrow(() -> new RestTemplate(new HttpComponentsClientHttpRequestFactory(client))
                    .exchange(URI.create(basePath + "/gateway/api/v1/auth/access-token/generate"), HttpMethod.POST, new HttpEntity<>(request, headers), String.class)
                    .getBody());
            }

            @Test
            void whenCreateToken_withMultipart_thenSucceed() {
                given()
                    .log().all()
                    .body("{\"scopes\": [\"gateway\"]}")
                    .contentType("application/json")
                    .accept("application/json")
                    .auth()
                        .preemptive()
                        .basic("user", "user")
                .when()
                    .post(basePath + "/gateway/api/v1/auth/access-token/generate")
                .then()
                    .statusCode(200);
            }

        }

    }

    @AllArgsConstructor
    @Data
    @Builder
    public static class PatRequest {
        private List<String> scopes;
    }

}
