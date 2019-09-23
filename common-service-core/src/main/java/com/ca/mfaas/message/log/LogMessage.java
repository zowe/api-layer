/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.log;

import com.ca.mfaas.message.core.MessageType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LogMessage {

    private final MessageType type;
    private final String text;

    private LogMessage(MessageType type,
                       String text) {
        this.type = type;
        this.text = text;
    }

    public static LogMessage of(MessageType type, String text) {
        return new LogMessage(type, text);
    }

    public void log() {
        switch (type) {
            case TRACE: log.trace(text);
            case DEBUG: log.debug(text);
            case INFO: log.info(text);
            case WARNING: log.warn(text);
            case ERROR: log.error(text);
        }
    }
}
