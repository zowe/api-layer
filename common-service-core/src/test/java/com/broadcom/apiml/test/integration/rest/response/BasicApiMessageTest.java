/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.rest.response;

import com.broadcom.apiml.test.integration.rest.response.impl.BasicApiMessage;
import com.broadcom.apiml.test.integration.rest.response.impl.BasicMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicApiMessageTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void fullJsonFormat() throws IOException {
        ApiMessage message = new BasicApiMessage(
            Collections.singletonList(new BasicMessage(MessageType.ERROR, "MAS0001E", "Error"))
        );
        String jsonString = "{\n" +
            "  \"messages\":[\n" +
            "    {\n" +
            "     \"messageType\":\"ERROR\",\n" +
            "     \"messageNumber\":\"MAS0001E\",\n" +
            "     \"messageContent\":\"Error\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        assertThatJson(message).isEqualTo(jsonString);

        Object deserialized = mapper.readValue(mapper.writeValueAsString(message),
            BasicApiMessage.class);
        assertEquals(deserialized, message);
    }

    @Test
    public void equalsTest() {
        ApiMessage message = new BasicApiMessage(new BasicMessage(MessageType.ERROR, "MAS0001", "Error"));

        assertTrue(message.equals(message));
    }
}
