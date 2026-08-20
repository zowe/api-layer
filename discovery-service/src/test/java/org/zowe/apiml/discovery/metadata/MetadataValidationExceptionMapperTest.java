/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataValidationExceptionMapperTest {

    private static final String MESSAGE_KEY = "org.zowe.apiml.common.metadataNotAllowedInRegistration";

    @Mock
    private MessageService messageService;

    private DomainAllowListExceptionMapper exceptionMapper;

    @BeforeEach
    void setUp() {
        exceptionMapper = new DomainAllowListExceptionMapper(messageService, new ObjectMapper());
    }

    @Nested
    class GivenValidationException {

        @Test
        void whenToResponse_thenReturnInternalServerErrorWithApiMessage() {
            var template = new MessageTemplate(MESSAGE_KEY, "ZWEAM604", MessageType.WARNING, "Invalid metadata found in registration");
            when(messageService.createMessage(MESSAGE_KEY)).thenReturn(Message.of(MESSAGE_KEY, template, new Object[0]));

            var response = exceptionMapper.toResponse(new DomainAllowListMetadataException("URLs not allowed found for instance"));

            assertEquals(SC_BAD_REQUEST, response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
            assertEquals("""
                    {"messages":[{"messageType":"WARNING","messageNumber":"ZWEAM604W","messageContent":"Invalid metadata found in registration","messageKey":"org.zowe.apiml.common.metadataNotAllowedInRegistration"}]}
                    """.trim(), response.getEntity());
        }

    }

}
