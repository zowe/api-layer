/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.stomp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class StompController {

    @MessageMapping("/replyWithSameSize/{id}")
    @SendTo("/topic/replyWithSameSize/{id}")
    public String replyWithSameSize(@DestinationVariable String id, @Payload String payload) throws IllegalArgumentException {
        var payloadSize = payload.getBytes().length;
        log.info("Received stomp message id {} with payload size {}. Sending the same size back.", id, payloadSize);

        char c = 'B';
        return String.valueOf(c).repeat(payloadSize);
    }
}
