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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.auth.saf.EndpointImproprietyConfigureException;
import org.zowe.apiml.security.common.auth.saf.UnsupportedResourceClassException;

@RequiredArgsConstructor
public class SafEndpointCheck implements ErrorCheck {

    private final MessageService messageService;

    private ResponseEntity<ApiMessageView> createResponse(EndpointImproprietyConfigureException eice) {
        ApiMessageView apiMessage = messageService.createMessage(
            "org.zowe.apiml.security.common.auth.saf.endpoint.endpointImproprietyConfigure",
            eice.getEndpoint()
        ).mapToView();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiMessage);
    }

    private ResponseEntity<ApiMessageView> createResponse(UnsupportedResourceClassException urce) {
        ApiMessageView apiMessage = messageService.createMessage(
            "org.zowe.apiml.security.common.auth.saf.endpoint.nonZoweClass",
            urce.getResourceClass()
        ).mapToView();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiMessage);
    }

    @Override
    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {
        int exceptionIndex;

        exceptionIndex = ExceptionUtils.indexOfType(exc, EndpointImproprietyConfigureException.class);
        if (exceptionIndex != -1) {
            return createResponse((EndpointImproprietyConfigureException) ExceptionUtils.getThrowables(exc)[exceptionIndex]);
        }

        exceptionIndex = ExceptionUtils.indexOfType(exc, UnsupportedResourceClassException.class);
        if (exceptionIndex != -1) {
            return createResponse((UnsupportedResourceClassException) ExceptionUtils.getThrowables(exc)[exceptionIndex]);
        }

        return null;
    }

}
