/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.token;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.acceptance.common.Service;
import org.zowe.apiml.acceptance.config.ApimlRoutingConfig;
import org.zowe.apiml.acceptance.config.DiscoveryClientTestConfig;
import org.zowe.apiml.acceptance.config.GatewayOverrideConfig;
import org.zowe.apiml.acceptance.config.GatewayTestApplication;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.gateway.security.mapping.AuthenticationMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.ApimlConstants.BEARER_AUTHENTICATION_PREFIX;
import static org.zowe.apiml.constants.ApimlConstants.HEADER_OIDC_TOKEN;

@SpringBootTest(
    classes = GatewayTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "apiml.security.oidc.validationType=endpoint",
        "apiml.security.oidc.enabled=true",
        "apiml.security.oidc.userInfo.uri=https://localhost/user/info",
        "apiml.security.filterChainConfiguration=new",
        "server.internal.enabled=false"
    }
)
@Import({
    GatewayOverrideConfig.class, DiscoveryClientTestConfig.class, ApimlRoutingConfig.class,
    OIDCTokenProviderEndpoint.class, OIDCTokenProviderEndpointTest.Config.class
})
class OIDCTokenProviderEndpointTest {

    private final static String MF_USER = "USER";

    private final static String VALID_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";
    private final static String INVALID_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiLAogICJ0eXBlIjogImludmFsaWQiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";

    @Autowired
    OIDCTokenProviderEndpoint oidcTokenProviderEndpoint;

    @Autowired
    protected ApplicationRegistry applicationRegistry;

    @Autowired
    @Qualifier("mockProxy")
    protected CloseableHttpClient mockClient;

    @LocalServerPort
    protected int port;

    protected String basePath;

    private CloseableHttpResponse createResponse(int responseCode, String body, Header...headers) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), responseCode, "")).when(response).getStatusLine();
        doReturn(new StringEntity(body, ContentType.APPLICATION_JSON)).when(response).getEntity();
        doReturn(headers).when(response).getAllHeaders();
        return response;
    }

    private Header[] copyHeaders(HttpUriRequest request, String...headerNames) {
        return Arrays.stream(headerNames)
            .map(request::getFirstHeader)
            .filter(Objects::nonNull)
            .toArray(Header[]::new);
    }

    @BeforeEach
    public void setBasePath() throws IOException {
        basePath = String.format("https://localhost:%d", port);
        ReflectionTestUtils.setField(oidcTokenProviderEndpoint, "endpointUrl", "https://oidc.provider.com/user/info");
        ReflectionTestUtils.setField(oidcTokenProviderEndpoint, "secureHttpClientWithKeystore", mockClient);

        doAnswer(invocation -> {
            HttpUriRequest request = invocation.getArgument(0);
            switch (request.getURI().getPath()) {
            case "/user/info":
                String authHeader = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
                if (StringUtils.equals(BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN, authHeader)) {
                    return createResponse(SC_OK, "{\"detail\":\"information\")");
                }
                return createResponse(SC_UNAUTHORIZED, "{\"error\":\"message\")");
            case "/request":
                return createResponse(SC_OK, "{\"status\":\"ok\"}", copyHeaders(request, HttpHeaders.AUTHORIZATION, HEADER_OIDC_TOKEN));
            default:
                fail("Unknown endpoint");
            }
            return null;
        }).when(mockClient).execute(any());

        MetadataBuilder serviceMetadataBuilder = MetadataBuilder.defaultInstance().withZoweJwt();
        Service service = new Service("app", "/app/api/v1/**", "api/v1");
        applicationRegistry.addApplication(service, serviceMetadataBuilder, false);
        applicationRegistry.setCurrentApplication("app");
    }

    @Test
    void givenValidTokenWithoutMapping_thenSetOidcToken() {
        Config.mfUserExists = false;
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)
        .when()
            .get(basePath + "/app/api/v1/request")
        .then()
            .statusCode(is(SC_OK))
            .header(HEADER_OIDC_TOKEN, VALID_TOKEN)
            .header(HttpHeaders.AUTHORIZATION, nullValue());
    }

    @Test
    void givenValidTokenWithMapping_thenSetZoweToken() {
        Config.mfUserExists = true;
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)
        .when()
            .get(basePath + "/app/api/v1/request")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.AUTHORIZATION, CoreMatchers.startsWith(BEARER_AUTHENTICATION_PREFIX))
            .header(HttpHeaders.AUTHORIZATION, not(containsString(VALID_TOKEN)))
            .header(HEADER_OIDC_TOKEN, nullValue());
    }

    @Test
    void givenUnparseableToken_thenDoNotChangeAuthorization() {
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " X" + INVALID_TOKEN)
        .when()
            .get(basePath + "/app/api/v1/request")
        .then()
            .statusCode(is(SC_OK))
            .header("x-zowe-auth-failure", is("ZWEAG102E Token is not valid"))
            .header(HEADER_OIDC_TOKEN, nullValue());
    }

    @Test
    void givenInvalidToken_thenDoNotChangeAuthorization() {
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + INVALID_TOKEN)
        .when()
            .get(basePath + "/app/api/v1/request")
        .then()
            .statusCode(is(SC_OK))
            .header("x-zowe-auth-failure", is("ZWEAG102E Token is not valid"))
            .header(HEADER_OIDC_TOKEN, nullValue());
    }

    @Configuration
    static class Config {

        private static boolean mfUserExists;

        @Bean
        @Primary
        public AuthenticationMapper oidcMapper() {
            return authSource -> mfUserExists && VALID_TOKEN.equals(authSource.getRawSource()) ? MF_USER : null;
        }

    }

}
