/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.error.ZosAuthenticationException;
import org.zowe.apiml.security.common.token.InvalidTokenTypeException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.io.IOException;

/**
 * Handler for exceptions that are thrown during the security client rest calls
 */
@Slf4j
@Component
public class RestResponseHandler {

    public void handleErrorType(CloseableHttpResponse response, ErrorType errorType, Object... logParameters) {
        switch (response.getCode()) {
            case 401:
                if (errorType != null) {
                    if (errorType.equals(ErrorType.BAD_CREDENTIALS)) {
                        throw new BadCredentialsException(errorType.getDefaultMessage());
                    } else if (errorType.equals(ErrorType.TOKEN_NOT_VALID)) {
                        throw new TokenNotValidException(errorType.getDefaultMessage());
                    } else if (errorType.equals(ErrorType.TOKEN_NOT_PROVIDED)) {
                        throw new TokenNotProvidedException(errorType.getDefaultMessage());
                    } else if (errorType.equals(ErrorType.INVALID_TOKEN_TYPE)) {
                        throw new InvalidTokenTypeException(errorType.getDefaultMessage());
                    } else if (errorType.equals(ErrorType.USER_SUSPENDED)) {
                        throw new ZosAuthenticationException(PlatformReturned.builder().errno(163).errnoMsg("org.zowe.apiml.security.platform.errno.EMVSSAFEXTRERR").build());
                    } else if (errorType.equals(ErrorType.NEW_PASSWORD_INVALID)) {
                        throw new ZosAuthenticationException(PlatformReturned.builder().errno(169).errnoMsg("org.zowe.apiml.security.platform.errno.EMVSPASSWORD").build());
                    } else if (errorType.equals(ErrorType.PASSWORD_EXPIRED)) {
                        throw new ZosAuthenticationException(PlatformReturned.builder().errno(168).errnoMsg("org.zowe.apiml.security.platform.errno.EMVSEXPIRE").build());
                    }
                }
                throw new BadCredentialsException(ErrorType.BAD_CREDENTIALS.getDefaultMessage());
            case 400:
                throw new AuthenticationCredentialsNotFoundException(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getDefaultMessage());
            case 405:
                throw new AuthMethodNotSupportedException(ErrorType.AUTH_METHOD_NOT_SUPPORTED.getDefaultMessage());
            case 500: case 503:
                throw new ServiceNotAccessibleException(ErrorType.SERVICE_UNAVAILABLE.getDefaultMessage());
            default:
                addDebugMessage(null, ErrorType.AUTH_GENERAL.getDefaultMessage(), logParameters);
                throw new AuthenticationServiceException(ErrorType.AUTH_GENERAL.getDefaultMessage());
        }
    }

    public void handleException(Exception exception) { //TODO: maybe revert
        throw new GatewayNotAvailableException(ErrorType.GATEWAY_NOT_AVAILABLE.getDefaultMessage(), exception);
    }

    private void addDebugMessage(Exception exception, String genericLogErrorMessage, Object... logParameters) {
        if (genericLogErrorMessage != null) {
            if (logParameters.length > 0) {
                log.debug(genericLogErrorMessage, logParameters);
            } else {
                log.debug(genericLogErrorMessage, exception);
            }
        }
    }
}
