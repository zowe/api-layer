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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * This class is intended for REST API responses that contain error,
 * warning, or informational messages in the common MFaaS format.
 *
 * It is preferred to return successful responses without messages if possible
 * and use only plain responses without wrapping for them.
 *
 * Its implementation {@link ApiMessage} should be used
 * in the case when a problem (an error) happens and then the response contains only the error(s).
 *
 * When a response needs to contain both data and messages (e.g. warnings)
 * then it is adviced for the response class to implement {@link ApiMessage} too.
 * But this should be an exception and we should try to make the REST API easy to use without
 * the need for the API user to process informational and warning messages.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AllArgsConstructor
public class ApiMessageView {

    /**
     * @return a list of messages that contain error, warning, and informational content.
     */
    private List<ApiMessage> messages;

}
