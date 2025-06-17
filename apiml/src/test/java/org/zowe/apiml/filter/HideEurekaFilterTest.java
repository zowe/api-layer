/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import java.io.IOException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HideEurekaFilterTest {

    @Mock private MessageService messageService;

    @Mock private FilterChain chain;

    private HideEurekaFilter filter;

    @BeforeEach
    void setUp() throws IOException, ServletException {
        when(messageService.createMessage("org.zowe.apiml.common.notFound"))
        .thenReturn(Message.of("org.zowe.apiml.common.notFound", new MessageTemplate(
            "org.zowe.apiml.common.notFound",
            "ZWEA1",
            MessageType.INFO,
            "debug message"
        ), new Object[]{}));

        lenient().doNothing().when(chain).doFilter(any(), any());

        filter = new HideEurekaFilter(messageService);
        ReflectionTestUtils.setField(filter, "externalPort", 10010);
    }

    @Nested
    class WhenUsingExternalPort {

        @Test
        void whenFilterWithEurekaPath_thenBlock() throws IOException, ServletException {
            var request = new MockHttpServletRequest();
            request.setServerPort(10010);
            request.setRequestURI("/eureka");

            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(APPLICATION_JSON, response.getHeader(CONTENT_TYPE));
            assertTrue(response.getContentAsString() != null && response.getContentAsString().contains(""));
            assertEquals(404, response.getStatus());
            verify(chain, never()).doFilter(request, response);
        }

        @Test
        void wenFilterWithOtherPath_thenContinue() throws IOException, ServletException {
            var request = new MockHttpServletRequest();
            request.setServerPort(10010);
            request.setRequestURI("/gateway");

            var response = mock(HttpServletResponse.class);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(response);
            verify(chain, times(1)).doFilter(request, response);
        }

    }

    @Nested
    class WhenUsingInternalDiscoveryPort {

        @Test
        void whenFilterWithEurekaPath_thenContinue() throws IOException, ServletException {
            var request = new MockHttpServletRequest();
            request.setServerPort(10011);
            request.setRequestURI("/eureka/apps");

            var response = mock(HttpServletResponse.class);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(response);
            verify(chain, times(1)).doFilter(request, response);
        }

        @Test
        void whenFilterWithOtherPath_thenContinue() throws IOException, ServletException {
            var request = new MockHttpServletRequest();
            request.setServerPort(10011);
            request.setRequestURI("/discovery/test");

            var response = mock(HttpServletResponse.class);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(response);
            verify(chain, times(1)).doFilter(request, response);
        }

    }

}
