/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * This interface is intended for REST API responses that contain error,
 * warning, or informational messages in the common MFaaS format.
 * <p>
 * It is preferred to return successful responses without messages if possible
 * and use only plain responses without wrapping for them.
 * <p>
 * The {@link ApiMessage} and its implementation {@link com.ca.mfaas.rest.response.impl.BasicApiMessage} should be used
 * in the case when a problem (an error) happens and then the response contains only the error(s).
 * <p>
 * When a response needs to contain both data and messages (e.g. warnings)
 * then it is adviced for the response class to implement {@link ApiMessage} too.
 * But this should be an exception and we should try to make the REST API easy to use without
 * the need for the API user to process informational and warning messages.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface ApiMessage {
    /**
     * @return a list of messages that contain error, warning, and informational content.
     */
    List<Message> getMessages();
}
