/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import static org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl.AUTHENTICATION_COMMAND_KEY;

@RequiredArgsConstructor
@Slf4j
public class ServiceAuthenticationDecorator {

    private final ServiceAuthenticationService serviceAuthenticationService;
    private final AuthenticationService authenticationService;
    private final PassTicketService passTicketService;

    /**
     * If a service requires authentication,
     *   verify that the specific instance was selected upfront
     *   decide whether it requires valid JWT token and if it does
     *     verify that the request contains valid one
     *
     * Prevent ribbon from retrying if Authentication Exception was thrown or if valid JWT token is required and wasn't
     * provided.
     *
     * @param request Current http request.
     * @return
     */
    public ResponseEntity<ApiMessageView> process(HttpRequest request) {
        final RequestContext context = RequestContext.getCurrentContext();

        if (context.get(AUTHENTICATION_COMMAND_KEY) instanceof ServiceAuthenticationServiceImpl.UniversalAuthenticationCommand) {
            InstanceInfo info = RequestContextUtils.getInstanceInfo().orElseThrow(
                () -> new RequestContextNotPreparedException("InstanceInfo of loadbalanced instance is not present in RequestContext")
            );
            final Authentication authentication = serviceAuthenticationService.getAuthentication(info);
            AuthenticationCommand cmd;

            try {
                final String jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest()).orElse(null);

                cmd = serviceAuthenticationService.getAuthenticationCommand(authentication, jwtToken);

                if (cmd == null) {
                    return null;
                }

                if (cmd.isRequiredValidJwt()
                    && (jwtToken == null || !authenticationService.validateJwtToken(jwtToken).isAuthenticated())) {
                    throw new RequestAbortException(new TokenNotValidException("JWT Token is not authenticated"));
                }
            }

            catch (IRRPassTicketGenerationException ae) {
                log.error(String.format("The request to service %s has failed because Zowe API ML user ID %s is not authorized to generate PassTickets for APPLID %s.", info.getInstanceId(), ae.getUser(), ae.getApplId()));
                return ResponseEntity
                    .status(ae.getErrorCode().getHttpStatus())
                    .body(passTicketService.writeResponse(ae));
            }

            catch (AuthenticationException ae) {
                throw new RequestAbortException(ae);
            }

            cmd.applyToRequest(request);
        }
        return null;
    }
}

