/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.message.api;

import org.zowe.apiml.message.core.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.util.List;

/**
 * User facing messages that can be provided with API responses.
 * <p>
 * We should include as much useful data as possible and keep in mind different users of the message structure.
 * Note that some users might have malicious intents therefore do not disclose your private data.
 */
@Setter
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

    /**
     * The severity of a problem. This field is required.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Typical mainframe message number (not including the message level one-letter code) that can be found in documentation.
     * The message number is usually in this format "pppnnnn" where ppp is a product code and nnnn
     * is a four-digit number.
     * <p>
     * Example: "PFI0031"
     */
    public String getMessageNumber() {
        return messageNumber;
    }

    /**
     * Readable message in US English. This field is required. It should be a sentence starting with a capital letter
     * and ending with a full stop (.).
     */
    public String getMessageContent() {
        return messageContent;
    }

    /**
     * Supplements the messageContent field, supplying more information about why the message is present.
     * This field is optional.
     */
    public String getMessageAction() {
        return messageAction;
    }

    /**
     * Recommendation of the actions to take in response to the message.
     * This field is optional.
     */
    public String getMessageReason() {
        return messageReason;
    }

    /**
     * Optional unique key describing the reason of the error.
     * It should be a dot delimited string "org.zowe.apiml.service[.subservice].detail".
     * The purpose of this field is to enable UI to show a meaningful and localized error message.
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * Optional error message parameters. Used for formatting of localized messages.
     */
    public List<T> getMessageParameters() {
        return messageParameters;
    }

    /**
     * Optional unique ID of the message instance. Used for finding of the message in the logs.
     * The same ID should be printed in the log.
     * This field is optional.
     * <p>
     * Example: "123e4567-e89b-12d3-a456-426655440000"
     */
    public String getMessageInstanceId() {
        return messageInstanceId;
    }

    /**
     * Qualified Java package or class name that generated the error.
     * This field is optional.
     * <p>
     * Example: org.zowe.apiml.product.package
     */
    public String getMessageComponent() {
        return messageComponent;
    }

    /**
     * Source service that generated the error (can MFaaS service name or host:port).
     * This field is optional.
     * <p>
     * Example: apiml-discovery-service, ca31:12345
     */
    public String getMessageSource() {
        return messageSource;
    }
}
