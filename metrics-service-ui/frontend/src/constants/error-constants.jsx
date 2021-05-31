/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

export const SEND_ERROR = 'SEND_ERROR';
export const CLEAR_ALL_ERRORS = 'CLEAR_ALL_ERRORS';

export class ApiError {
    key = '';
    number = '';
    text = '';
    messageType = {};

    constructor(key, number, messageType, text) {
        this.key = key;
        this.number = number;
        this.messageType = messageType;
        this.text = text;
    }
}

export class MessageType {
    levelInt = 0;
    levelStr = '';
    levelChar = '';

    constructor(levelInt, levelStr, levelChar) {
        this.levelInt = levelInt;
        this.levelStr = levelStr;
        this.levelChar = levelChar;
    }
}
