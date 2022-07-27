/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.gateway.security.login.SuccessfulAccessTokenHandler;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.filter.StoreAccessTokenInfoFilter.TOKEN_REQUEST;

class StoreAccessTokenInfoFilterTest {
    private StoreAccessTokenInfoFilter underTest;
    private MockHttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private static final String VALID_JSON = "{\"validity\": 90, \"scopes\": [\"service\"]}";
    private static final String VALID_JSON_NO_SCOPES = "{\"validity\": 90}";
    private static final String INVALID_JSON = "{ \"notValid\", \"scopes\": [\"service\"]}";
    private static final String INVALID_JSON2 = "{\"valissdity\": 90, \"scopes\": [\"service\"]}";
    private final MessageService messageService = new YamlMessageService("/security-service-messages.yml");

    private final AuthExceptionHandler authExceptionHandler = new AuthExceptionHandler(messageService, new ObjectMapper());

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        underTest = new StoreAccessTokenInfoFilter(authExceptionHandler);
        request.setMethod(HttpMethod.POST.name());

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GivenRequestWithBodyTest {

        @Test
        void thenStoreExpirationTimeAndScopes() throws ServletException, IOException {
            request.setContent(VALID_JSON.getBytes());
            underTest.doFilterInternal(request, response, chain);
            SuccessfulAccessTokenHandler.AccessTokenRequest tokenRequest = (SuccessfulAccessTokenHandler.AccessTokenRequest) request.getAttribute(TOKEN_REQUEST);
            Set<String> scopes = tokenRequest.getScopes();
            assertEquals(90, tokenRequest.getValidity());
            assertTrue(scopes.contains("service"));
            verify(chain, times(1)).doFilter(request, response);
        }

        Stream<byte[]> values() {
            return Stream.of(VALID_JSON_NO_SCOPES.getBytes(), null);
        }

        @ParameterizedTest
        @MethodSource("values")
        void givenNoScopesInRequest_thenThrowException(byte[] body) throws ServletException, IOException {

            request.setContent(body);
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            when(response.getWriter()).thenReturn(writer);
            underTest.doFilterInternal(request, response, chain);
            assertThat(out.toString(), containsString("ZWEAT606E"));
            verify(chain, times(0)).doFilter(request, response);
        }
    }

    @Nested
    class GivenRequestWithNotValidBody {
        @BeforeEach
        public void setUp() {
            response = new MockHttpServletResponse();
        }

        @Test
        void whenInvalidValue_thenReturn400() throws ServletException {
            request.setContent(INVALID_JSON.getBytes());
            underTest.doFilterInternal(request, response, chain);
            assertThat(response.getStatus(), is(400));
        }

        @Test
        void whenInvalidKey_thenReturn400() throws ServletException {
            request.setContent(INVALID_JSON2.getBytes());
            underTest.doFilterInternal(request, response, chain);
            assertThat(response.getStatus(), is(400));
        }
    }
}
