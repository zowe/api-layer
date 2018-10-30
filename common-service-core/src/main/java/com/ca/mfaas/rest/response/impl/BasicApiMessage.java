/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.rest.response.impl;

import com.ca.mfaas.rest.response.ApiMessage;
import com.ca.mfaas.rest.response.Message;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The implementation of {@link ApiMessage} should be used in the case when a problem (an error) happens
 * and then the response contains only the error(s).
 */
public class BasicApiMessage implements ApiMessage {
    private final List<Message> messages;

    @JsonCreator
    public BasicApiMessage(@JsonProperty("messages") @JsonDeserialize(contentAs = BasicMessage.class) List<Message> messages) {
        this.messages = messages;
    }

    public BasicApiMessage(Message message) {
        this.messages = Collections.singletonList(message);
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BasicApiMessage)) {
            return false;
        }
        BasicApiMessage that = (BasicApiMessage) o;
        return Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages);
    }

    @Override
    public String toString() {
        return "BasicApiMessage{" +
            "messages=" + messages +
            '}';
    }
}
