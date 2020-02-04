/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.message.core;

import lombok.RequiredArgsConstructor;

/**
 * Represents the severity of a problem. Higher severity has higher value.
 */
@RequiredArgsConstructor
public enum MessageType {
    TRACE(0, "TRACE", 'T'),
    DEBUG(10, "DEBUG", 'D'),
    INFO(20, "INFO", 'I'),
    WARNING(30, "WARNING", 'W'),
    ERROR(40, "ERROR", 'E');

    private final int levelInt;
    private final String levelStr;
    private final char levelChar;

    public int toInt() {
        return levelInt;
    }

    public char toChar() {
        return levelChar;
    }

    @Override
    public String toString() {
        return levelStr;
    }
}
