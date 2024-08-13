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
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.zaas.ZaasApplication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderEndpoint;

import javax.net.ssl.SSLContext;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;
import static org.zowe.apiml.zaas.zaas.SchemeController.CONTROLLER_PATH;

@SpringBootTest(
    properties = {
        "apiml.security.oidc.validationType=endpoint",
        "apiml.security.oidc.enabled=true",
        "apiml.security.oidc.userInfo.uri=https://oidc.provider.com/user/info",
        "apiml.security.filterChainConfiguration=new"
    },
    classes = {
        ZaasApplication.class,
        OIDCTokenProviderEndpoint.class,
        OIDCTokenProviderJwkEndpointTest.Config.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("OIDCTokenProviderEndpointTest")
class OIDCTokenProviderJwkEndpointTest {

    private static final String MF_USER = "USER";
    private static final String VALID_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";
    private static final String INVALID_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiLAogICJ0eXBlIjogImludmFsaWQiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";

    @LocalServerPort
    private int port;

    private String zaasEndpoint;
    private RestAssuredConfig withClientCert;

    @Autowired
    HttpConfig httpConfig;

    @Autowired
    OIDCTokenProviderEndpoint oidcTokenProviderEndpoint;

    @Autowired
    CloseableHttpClient secureHttpClientWithKeystore;

    private CloseableHttpResponse createResponse(int responseCode, String body, Header...headers) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        Mockito.doReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), responseCode, "")).when(response).getStatusLine();
        Mockito.doReturn(new StringEntity(body, ContentType.APPLICATION_JSON)).when(response).getEntity();
        Mockito.doReturn(headers).when(response).getAllHeaders();
        return response;
    }

    @BeforeAll
    public void init() throws IOException {
        zaasEndpoint = "https://localhost:" + port + CONTROLLER_PATH + "/zoweJwt";

        SSLContext sslContext = httpConfig.secureSslContext();
        withClientCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))); // NOSONAR

        Mockito.doAnswer(invocation -> {
            HttpUriRequest request = invocation.getArgument(0);
            String authHeader = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
            if (StringUtils.equals(ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN, authHeader)) {
                return createResponse(HttpStatus.SC_OK, "{\"detail\":\"information\")");
            }
            return createResponse(HttpStatus.SC_UNAUTHORIZED, "{\"error\":\"message\")");
        }).when(secureHttpClientWithKeystore).execute(ArgumentMatchers.any());
    }

    @Test
    void givenValidTokenWithoutMapping_thenSetOidcToken() {
        Config.mfUserExists = false;
        RestAssured.given().config(withClientCert)
            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)
        .when()
            .post(zaasEndpoint)
        .then()
            .statusCode(Is.is(HttpStatus.SC_UNAUTHORIZED))
            .header(ApimlConstants.HEADER_OIDC_TOKEN, VALID_TOKEN);
    }

    @Test
    void givenValidTokenWithMapping_thenResponseZoweToken() {
        Config.mfUserExists = true;
        RestAssured.given().config(withClientCert)
            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)
        .when()
            .post(zaasEndpoint)
        .then()
            .statusCode(Is.is(HttpStatus.SC_OK))
            .body("cookieName", CoreMatchers.equalTo(COOKIE_AUTH_NAME))
            .body("headerName", CoreMatchers.nullValue())
            .body("token", CoreMatchers.not(CoreMatchers.equalTo(VALID_TOKEN)))
            .body("token", CoreMatchers.not(CoreMatchers.nullValue()));
    }

    @Test
    void givenUnparseableToken_thenRejectWithoutOidcToken() {
        RestAssured.given().config(withClientCert)
            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " X" + INVALID_TOKEN)
        .when()
            .post(zaasEndpoint)
        .then()
            .statusCode(Is.is(HttpStatus.SC_UNAUTHORIZED))
            .header(ApimlConstants.HEADER_OIDC_TOKEN, CoreMatchers.nullValue());
    }

    @Test
    void givenInvalidToken_thenRejectWithoutOidcToken() {
        RestAssured.given().config(withClientCert)
            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + INVALID_TOKEN)
        .when()
            .post(zaasEndpoint)
        .then()
            .statusCode(Is.is(HttpStatus.SC_UNAUTHORIZED))
            .header(ApimlConstants.HEADER_OIDC_TOKEN, CoreMatchers.nullValue());
    }

    @Profile("OIDCTokenProviderEndpointTest")
    @Configuration
    static class Config {

        private static boolean mfUserExists;

        @Bean
        @Primary
        public AuthenticationMapper oidcMapper() {
            return authSource -> mfUserExists && VALID_TOKEN.equals(authSource.getRawSource()) ? MF_USER : null;
        }

        @Bean
        CloseableHttpClient secureHttpClientWithKeystore() {
            return mock(CloseableHttpClient.class);
        }

    }

}
