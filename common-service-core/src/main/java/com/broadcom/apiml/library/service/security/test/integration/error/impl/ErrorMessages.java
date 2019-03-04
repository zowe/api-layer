/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.error.impl;

import java.util.ArrayList;
import java.util.List;

public class ErrorMessages {
    private List<ErrorMessage> messages;

    public ErrorMessages(List<ErrorMessage> messages) {
        this.messages = messages;
    }

    public ErrorMessages() {
        this.messages = new ArrayList<>();
    }

    public List<ErrorMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ErrorMessage> messages) {
        this.messages = messages;
    }
}
