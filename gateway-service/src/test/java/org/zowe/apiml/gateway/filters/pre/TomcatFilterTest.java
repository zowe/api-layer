/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.pre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.yaml.YamlMessageService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TomcatFilterTest {

    private TomcatFilter filter;

    private static MessageService messageService;
    private static ObjectMapper mapper;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private Message mockedMessage;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        messageService = mock(YamlMessageService.class);
        mapper = mock(ObjectMapper.class);
        mockedMessage = mock(Message.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    public void shouldRejectEncodedSlashRequestsWhenConfiguredToReject() throws IOException, ServletException {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "false");
        when(messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", "/api/v1/encoded%2fslash"))
            .thenReturn(Message.of("org.zowe.apiml.key", createMessageTemplate("text"), new Object[]{"parameter"}));
        when(request.getRequestURI()).thenReturn("/api/v1/encoded%2fslash");
        filter = new TomcatFilter(messageService, mapper);

        filter.doFilter(request, response, filterChain);
        verify(response).setContentType("application/json");
    }

    @Test
    public void shouldAllowNonEncodedSlashRequestsAndMoveToNextFilterWhenConfiguredToReject() throws IOException, ServletException {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "false");
        when(messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", "/api/v1/normal"))
            .thenReturn(Message.of("org.zowe.apiml.key", createMessageTemplate("text"), new Object[]{"parameter"}));
        filter = new TomcatFilter(messageService, mapper);

        when(request.getRequestURI()).thenReturn("/api/v1/normal");

        filter.doFilter(request, response, filterChain);
        //verify(response).
    }

    @Test
    public void shouldAllowAnyRequestAndMoveToNextFilterWhenConfiguredToAllow() throws IOException, ServletException {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        when(messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", anyString()))
            .thenReturn(Message.of("org.zowe.apiml.key", createMessageTemplate("text"), new Object[]{"parameter"}));
        filter = new TomcatFilter(messageService, mapper);

        when(request.getRequestURI()).thenReturn("/api/v1/normal");
        filter.doFilter(request, response, filterChain);

        reset(request);
        when(request.getRequestURI()).thenReturn("/api/v1/encoded%2fslash");
        filter.doFilter(request, response, filterChain);

        //verify(nextValve, times(2)).invoke(request,response);
    }

    private MessageTemplate createMessageTemplate(String messageText) {
        MessageTemplate messageTemplate = new MessageTemplate();
        messageTemplate.setKey("org.zowe.apiml.gateway.requestContainEncodedSlash");
        messageTemplate.setNumber("ZWEAM700");
        messageTemplate.setType(MessageType.ERROR);
        messageTemplate.setText(messageText);

        return messageTemplate;
    }
}
