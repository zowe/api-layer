/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.controllers;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.auth.saf.AccessLevel;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
@SuppressWarnings("squid:S1075") // CONTEXT_PATH and FULL_CONTEXT_PATH don't need to be parametrized
public class SafResourceAccessController {

    private final SafResourceAccessVerifying safResourceAccessVerifying;
    private final MessageService messageService;
    public static final String CONTEXT_PATH = "/auth/check";
    public static final String FULL_CONTEXT_PATH = "/gateway/auth/check";

    @PostMapping(path = CONTEXT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @HystrixCommand
    public ResponseEntity<ApiMessageView> hasSafAccess(@RequestBody CheckRequestModel request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (safResourceAccessVerifying
            .hasSafResourceAccess(authentication, request.getResourceClass(),
                request.getResourceName(), request.getAccessLevel().name())) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(
                messageService.createMessage("org.zowe.apiml.security.unauthorized",authentication.getPrincipal().toString()).mapToView(),
                HttpStatus.UNAUTHORIZED);
        }
    }

    @Data
    static class CheckRequestModel {
        private String resourceClass;
        private String resourceName;
        private AccessLevel accessLevel;
    }

    @Data
    @AllArgsConstructor
    static class ErrorInfo {
        private String error;
        private String exception;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseBody
    public ErrorInfo errorDeserializingRequest(HttpServletRequest req, Exception ex) {
        return new ErrorInfo("Failed to deserialize the request body", ex.getMessage());
    }
}
