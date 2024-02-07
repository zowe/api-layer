/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.error.check;

import com.netflix.client.ClientException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.*;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;

import javax.servlet.http.HttpServletRequest;

/**
 * Handler for exceptions that arise during load balancing
 */
@RequiredArgsConstructor
public class LoadBalancerErrorCheck implements ErrorCheck {
    private final MessageService messageService;
    @Override
    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {

        int exceptionIndex = -1;

        if ( (exceptionIndex = ExceptionUtils.indexOfType(exc, ClientException.class)) != -1) {
            Throwable t = ExceptionUtils.getThrowables(exc)[exceptionIndex];
            if (t instanceof ClientException && StringUtils.startsWith(t.getMessage(), "Load balancer does not have available server for client")) {
                ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.loadBalancerDoesNotHaveAvailableServer",
                    t.getMessage().substring(t.getMessage().lastIndexOf(":") + 2)).mapToView();
                return getApiMessageViewResponseEntity(messageView);
            }

        }

        return null;
    }

    private ResponseEntity<ApiMessageView> getApiMessageViewResponseEntity(ApiMessageView messageView) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

}
