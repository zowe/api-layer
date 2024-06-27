/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TomcatFilterTest {

    private static final String ALLOW_ENCODED_SLASHES_FIELD = "allowEncodedSlashes";
    private static final String ENCODED_REQUEST_URI = "/api/v1/encoded%2fslash";
    private static final String NORMAL_REQUEST_URI = "/api/v1/normal";
    private static MessageService messageService;
    @Mock
    private ObjectMapper objectMapper;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private TomcatFilter filter;
    private FilterChain filterChain;

    @BeforeAll
    static void initMessageService() {
        messageService = new YamlMessageService("/gateway-log-messages.yml");
    }

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldRejectEncodedSlashRequestsWhenConfiguredToReject() throws IOException, ServletException {
        filter = new TomcatFilter(messageService, objectMapper);
        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, false);

        request.setRequestURI(ENCODED_REQUEST_URI);
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

        Message message = messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", request.getRequestURI());
        verify(objectMapper).writeValue(response.getWriter(), message.mapToView());
    }

    @Test
    void shouldAllowNonEncodedSlashRequestsAndMoveToNextFilterWhenConfiguredToReject() throws IOException, ServletException {
        filter = new TomcatFilter(messageService, objectMapper);
        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, false);

        request.setRequestURI(NORMAL_REQUEST_URI);
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowAnyRequestAndMoveToNextFilterWhenConfiguredToAllow() throws IOException, ServletException {
        filter = new TomcatFilter(messageService, objectMapper);
        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, true);

        request.setRequestURI(NORMAL_REQUEST_URI);
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        request.setRequestURI(ENCODED_REQUEST_URI);
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldThrowServletExceptionOnIOExceptionWhenWritingResponse() throws IOException {
        HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockedResponse = mock(HttpServletResponse.class);

        filter = new TomcatFilter(messageService, objectMapper);
        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, false);

        when(mockedRequest.getRequestURI()).thenReturn(ENCODED_REQUEST_URI);
        when(mockedResponse.getWriter()).thenThrow(new IOException());

        assertThrows(ServletException.class, () -> filter.doFilter(mockedRequest, mockedResponse, filterChain));
    }
}
