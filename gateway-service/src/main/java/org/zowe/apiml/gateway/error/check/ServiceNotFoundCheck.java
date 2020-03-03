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

import com.netflix.zuul.exception.ZuulException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
public class ServiceNotFoundCheck implements ErrorCheck {
    private final MessageService messageService;

    @Override
    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {
        if (exc instanceof ZuulException) {
            ZuulException exception = (ZuulException) exc;
            if(exception.nStatusCode == HttpStatus.NOT_FOUND.value()) {
                ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.endPointNotFound", exception.errorCause).mapToView();
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(messageView);
            }
        }

        return null;
    }
}
