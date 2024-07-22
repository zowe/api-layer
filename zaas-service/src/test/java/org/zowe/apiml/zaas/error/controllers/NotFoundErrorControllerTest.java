/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class NotFoundErrorControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        MessageTemplate messageTemplate = new MessageTemplate("org.zowe.apiml.common.endPointNotFound", "number", MessageType.ERROR, "text");
        Message message = Message.of("org.zowe.apiml.common.endPointNotFound", messageTemplate, new Object[0]);
        MessageService messageService = mock(MessageService.class);

        when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);

        NotFoundErrorController notFoundErrorController = new NotFoundErrorController(messageService);
        MockitoAnnotations.openMocks(this);

        mockMvc = MockMvcBuilders
            .standaloneSetup(notFoundErrorController)
            .build();
    }

    @Nested
    class GivenNotFoundErrorRequest {
        @Test
        void whenCallingWithRequestAttribute_thenReturnProperErrorStatus() throws Exception {
            mockMvc.perform(get("/not_found").requestAttr("javax.servlet.error.status_code", 404))
                .andExpect(status().isNotFound());
        }
    }

}
