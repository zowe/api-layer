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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpRequest;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.ServiceAuthenticationService;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import static org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl.AUTHENTICATION_COMMAND_KEY;

@RequiredArgsConstructor
public class ServiceAuthenticationDecorator {

    private final ServiceAuthenticationService serviceAuthenticationService;
    private final AuthSourceService authSourceService;

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
     */
    public void process(HttpRequest request) {
        final RequestContext context = RequestContext.getCurrentContext();

        if (context.get(AUTHENTICATION_COMMAND_KEY) instanceof ServiceAuthenticationServiceImpl.UniversalAuthenticationCommand) {
            InstanceInfo info = RequestContextUtils.getInstanceInfo().orElseThrow(
                () -> new RequestContextNotPreparedException("InstanceInfo of loadbalanced instance is not present in RequestContext")
            );
            final Authentication authentication = serviceAuthenticationService.getAuthentication(info);
            AuthenticationCommand cmd;

            try {
                final Optional<JwtAuthSource> authSource = authSourceService.getAuthSource();
                final String jwtToken = authSource.map(JwtAuthSource::getSource).orElse(null);

                cmd = serviceAuthenticationService.getAuthenticationCommand(authentication, jwtToken);

                if (cmd == null) {
                    return;
                }

                if (cmd.isRequiredValidJwt()
                    && (!authSource.isPresent() || !authSourceService.isValid(authSource.get()))) {
                    throw new RequestAbortException(new TokenNotValidException("JWT Token is not authenticated"));
                }
            }
            catch (AuthenticationException ae) {
                throw new RequestAbortException(ae);
            }

            cmd.applyToRequest(request);
        }
    }
}

