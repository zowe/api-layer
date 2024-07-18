/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error.check;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.NestedServletException;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.auth.saf.EndpointImproprietyConfigureException;
import org.zowe.apiml.security.common.auth.saf.UnsupportedResourceClassException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SafEndpointCheckTest {

    private static final String ENDPOINT_URL = "http://endpoint/url";
    private static final String UNEXPECTED_RESOURCE_CLASS = "UnexpectedResourceClass";

    private SafEndpointCheck safEndpointCheck;

    @BeforeAll
    void setUp() {
        MessageService messageService = new YamlMessageService("/security-common-log-messages.yml");
        safEndpointCheck = new SafEndpointCheck(messageService);
    }

    private ApiMessage getApiMessage(ResponseEntity<ApiMessageView> response) {
        assertNotNull(response);
        assertNotNull(response.getBody());
        List<ApiMessage> messages = response.getBody().getMessages();
        assertEquals(1, messages.size());
        return messages.get(0);
    }

    @Test
    void givenCoveredEndpointImproprietyConfigureException_whenCheck_thenReturnMessage() {
        EndpointImproprietyConfigureException eice = new EndpointImproprietyConfigureException("An error", ENDPOINT_URL);
        NestedServletException coveredException = new NestedServletException("msg", eice);
        ResponseEntity<ApiMessageView> response = safEndpointCheck.checkError(new MockHttpServletRequest(), coveredException);

        ApiMessage message = getApiMessage(response);
        assertEquals("org.zowe.apiml.security.common.auth.saf.endpoint.endpointImproprietyConfigure", message.getMessageKey());
        assertEquals("ZWEAT603E", message.getMessageNumber());
        assertTrue(message.getMessageContent().contains(ENDPOINT_URL));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void givenCoveredUnsupportedResourceClassException_whenCheck_thenReturnMessage() {
        UnsupportedResourceClassException urce = new UnsupportedResourceClassException(UNEXPECTED_RESOURCE_CLASS, "message");
        NestedServletException coveredException = new NestedServletException("msg", urce);
        ResponseEntity<ApiMessageView> response = safEndpointCheck.checkError(new MockHttpServletRequest(), coveredException);

        ApiMessage message = getApiMessage(response);
        assertEquals("org.zowe.apiml.security.common.auth.saf.endpoint.nonZoweClass", message.getMessageKey());
        assertEquals("ZWEAT602E", message.getMessageNumber());
        assertTrue(message.getMessageContent().contains(UNEXPECTED_RESOURCE_CLASS));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void givenNonEndpointException_whenCheck_thenReturnNull() {
        assertNull(safEndpointCheck.checkError(new MockHttpServletRequest(), new RuntimeException()));
    }

}