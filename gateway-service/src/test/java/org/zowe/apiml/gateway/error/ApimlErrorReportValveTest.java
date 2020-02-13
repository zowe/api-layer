/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.error;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.yaml.YamlMessageService;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ApimlErrorReportValveTest {
    private static MessageService messageService;
    private ApimlErrorReportValve logValve;
    private Request req;
    private Response resp;
    private Valve nextValve;
    private Message mockedMessage;

    @Before
    public void setup() {
        req = mock(Request.class);
        resp = mock(Response.class);
        nextValve = mock(Valve.class);
        messageService = mock(YamlMessageService.class);
        mockedMessage = mock(Message.class);
    }

    @Test
    public void shouldRejectEncodedSlashRequestsWhenConfiguredToReject() throws IOException, ServletException {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "false");
        when(messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", "/api/v1/encoded%2fslash"))
            .thenReturn(Message.of("org.zowe.apiml.key", createMessageTemplate("text"), new Object[]{"parameter"}));
        logValve = new ApimlErrorReportValve(messageService);

        when(req.getRequestURI()).thenReturn("/api/v1/encoded%2fslash");

        logValve.invoke(req,resp);
        verify(resp).setContentType("application/json");
        verify(resp).setAppCommitted(true);
    }

    @Test
    public void shouldAllowNonEncodedSlashRequestsAndMoveToNextValveWhenConfiguredToReject() throws IOException, ServletException {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "false");
        when(messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", "/api/v1/normal"))
            .thenReturn(Message.of("org.zowe.apiml.key", createMessageTemplate("text"), new Object[]{"parameter"}));
        logValve = new ApimlErrorReportValve(messageService);
        logValve.setNext(nextValve);
        when(req.getRequestURI()).thenReturn("/api/v1/normal");

        logValve.invoke(req,resp);
        verify(nextValve).invoke(req,resp);
    }

    @Test
    public void shouldAllowAnyRequestAndMoveToNextValveWhenConfiguredToAllow() throws IOException, ServletException {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        when(messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", anyString()))
            .thenReturn(Message.of("org.zowe.apiml.key", createMessageTemplate("text"), new Object[]{"parameter"}));
        logValve = new ApimlErrorReportValve(messageService);
        logValve.setNext(nextValve);

        when(req.getRequestURI()).thenReturn("/api/v1/normal");
        logValve.invoke(req,resp);

        reset(req);
        when(req.getRequestURI()).thenReturn("/api/v1/encoded%2fslash");
        logValve.invoke(req,resp);

        verify(nextValve, times(2)).invoke(req,resp);
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
