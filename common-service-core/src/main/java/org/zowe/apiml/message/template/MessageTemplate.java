/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.message.template;

import org.zowe.apiml.message.core.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageTemplate {

    private String key;
    private String number;
    private MessageType type;
    private String text;
    private String reason;
    private String action;

    public MessageTemplate(String key, String number, MessageType type, String text) {
        this.key = key;
        this.number = number;
        this.type = type;
        this.text = text;
    }
}
