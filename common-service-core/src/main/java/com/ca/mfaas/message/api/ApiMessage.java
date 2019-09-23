/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.api;

import com.ca.mfaas.message.core.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiMessage<T> {

    private MessageType messageType;
    private String messageNumber;
    private String messageContent;
    private String messageAction;
    private String messageReason;
    private String messageKey;
    private List<T> messageParameters;
    private String messageInstanceId;
    private String messageComponent;
    private String messageSource;


    public ApiMessage(String messageKey, MessageType messageType, String messageNumber, String messageContent) {
        this.messageKey = messageKey;
        this.messageType = messageType;
        this.messageNumber = messageNumber;
        this.messageContent = messageContent;
    }
}
