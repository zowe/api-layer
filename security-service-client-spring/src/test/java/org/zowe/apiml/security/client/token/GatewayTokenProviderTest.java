/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.client.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.security.client.handler.RestResponseHandler;
import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayTokenProviderTest {

    @Nested
    class Authenticate {

        private static final String USER = "USER";
        private static final String DOMAIN = "PASS";
        private static final String VALID_TOKEN = "VALID_TOKEN";

        private final GatewaySecurityService gatewaySecurityService = mock(GatewaySecurityService.class);
        private final GatewayTokenProvider gatewayTokenProvider = new GatewayTokenProvider(gatewaySecurityService);

        @Test
        void shouldAuthenticateValidToken() {
            when(gatewaySecurityService.query(VALID_TOKEN)).thenReturn(new QueryResponse(DOMAIN, USER, new Date(), new Date(), "issuer", Collections.emptyList(), QueryResponse.Source.ZOWE));
            TokenAuthentication tokenAuthentication = new TokenAuthentication(VALID_TOKEN);

            Authentication processedAuthentication = gatewayTokenProvider.authenticate(tokenAuthentication);

            assertTrue(processedAuthentication instanceof TokenAuthentication);
            assertTrue(processedAuthentication.isAuthenticated());
            assertEquals(VALID_TOKEN, processedAuthentication.getCredentials());
            assertEquals(USER, processedAuthentication.getName());
        }

        @Test
        void shouldSupportTokenAuthentication() {
            assertTrue(gatewayTokenProvider.supports(TokenAuthentication.class));
        }

        @Test
        void shouldNotSupportGenericAuthentication() {
            assertFalse(gatewayTokenProvider.supports(Authentication.class));
        }

    }

    @Nested
    class Tokens {

        GatewayTokenProvider gatewayTokenProvider;
        CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);

        @BeforeEach
        void setUp() throws IOException {
            GatewayClient gatewayClient = new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build());

            GatewaySecurityService gatewaySecurityService = new GatewaySecurityService(
                gatewayClient,
                new AuthConfigurationProperties(),
                closeableHttpClient,
                new RestResponseHandler()
            );

            this.gatewayTokenProvider = new GatewayTokenProvider(gatewaySecurityService);

            doAnswer(invocation -> {
                HttpUriRequestBase request = invocation.getArgument(0);
                HttpClientResponseHandler handler = invocation.getArgument(1);

                String token = null;
                AbstractHttpEntity entity = null;
                if (request instanceof HttpGet get) {
                    // query
                    assertEquals("https://localhost/gateway/api/v1/auth/query", get.getUri().toString());
                    String[] cookieParts = get.getHeader("cookie").getValue().split("=");
                    assertEquals("apimlAuthenticationToken", cookieParts[0]);
                    token = cookieParts[1];
                    entity = new StringEntity(new ObjectMapper().writeValueAsString(new QueryResponse()), ContentType.APPLICATION_JSON);
                } else if (request instanceof HttpPost post) {
                    // oid validation
                    assertEquals("https://localhost/gateway/api/v1/auth/oidc-token/validate", post.getUri().toString());
                    token = new ObjectMapper().readTree(post.getEntity().getContent()).get("token").asText();
                } else {
                    fail("Unknown request");
                }

                var response = new BasicClassicHttpResponse("valid".equals(token) ? 200 : 401);
                if (entity != null) {
                    response.setEntity(entity);
                }
                return handler.handleResponse(response);
            }).when(closeableHttpClient).execute(any(), (HttpClientResponseHandler<?>) any());
        }

        @Test
        void givenValidOidcToken_whenAuthenticate_thenCallOidcEndpoint() {
            var authenticate = gatewayTokenProvider.authenticate(new TokenAuthentication("valid", TokenAuthentication.Type.OIDC));
            assertTrue(authenticate.isAuthenticated());
        }

        @ParameterizedTest
        @CsvSource({
            ",",
            "JWT"
        })
        void givenValidJwtToken_whenAuthenticate_thenCallOidcEndpoint(TokenAuthentication.Type type) {
            var authenticate = gatewayTokenProvider.authenticate(new TokenAuthentication("valid", type));
            assertTrue(authenticate.isAuthenticated());
        }

        @Test
        void givenInvalidOidcToken_whenAuthenticate_thenCallOidcEndpoint() {
            assertThrows(AuthenticationException.class, () -> gatewayTokenProvider.authenticate(new TokenAuthentication("invalid", TokenAuthentication.Type.OIDC)));
        }

        @ParameterizedTest
        @CsvSource({
            ",",
            "JWT"
        })
        void givenInvalidJwtToken_whenAuthenticate_thenCallOidcEndpoint(TokenAuthentication.Type type) {
            assertThrows(AuthenticationException.class, () -> gatewayTokenProvider.authenticate(new TokenAuthentication("invalid", type)));
        }

    }

}
