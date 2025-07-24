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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties.CookieProperties;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpUtilsTest {

    @Mock private AuthConfigurationProperties authProperties;

    @InjectMocks
    private HttpUtils httpUtils;

    @Test
    void shouldSetMaxAge_onPostConstruct() {
        var properties = new CookieProperties();
        properties.setCookieMaxAge(100);

        var defaultValue = ReflectionTestUtils.getField(httpUtils, "cookieMaxAge");
        assertEquals(-1, defaultValue);

        when(authProperties.getCookieProperties()).thenReturn(properties);
        httpUtils.readConfig();

        var value = ReflectionTestUtils.getField(httpUtils, "cookieMaxAge");
        assertEquals(100, value);
    }

    @Test
    void shouldExtractTokenFromCookie() {
        var cookie = new HttpCookie("apimlAuthenticationToken", "test-token");
        var request = MockServerHttpRequest.get("/logout")
            .cookie(cookie)
            .build();

        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(httpUtils.getTokenFromRequest(exchange))
            .expectNext("test-token")
            .verifyComplete();
    }

    @Test
    void shouldExtractTokenFromAuthorizationHeader() {
        var request = MockServerHttpRequest.get("/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .build();

        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(httpUtils.getTokenFromRequest(exchange))
            .expectNext("test-token")
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForMissingToken() {
        var request = MockServerHttpRequest.get("/logout").build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(httpUtils.getTokenFromRequest(exchange))
            .verifyComplete();
    }

    @Test
    void testCreateResponseCookie() { // test defaults
        var jwt = "sample.jwt.token";
        when(authProperties.getCookieProperties()).thenReturn(new CookieProperties());
        httpUtils.readConfig();
        ResponseCookie cookie = httpUtils.createResponseCookie(jwt);
        assertEquals("apimlAuthenticationToken", cookie.getName());
        assertEquals(jwt, cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(-1, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }

}
