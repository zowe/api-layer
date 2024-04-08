/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * This filter is responsible for customization request to clients from security point of view. In this filter is
 * fetched AuthenticationCommand which support target security. In case it is possible decide now (all instances
 * use the same authentication) it will modify immediately. Otherwise in request params will be set a command to
 * load balancer. The request will be modified after specific instance will be selected.
 */
public class ServiceAuthenticationFilter extends PreZuulFilter {
    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    @Autowired
    private ServiceAuthenticationServiceImpl serviceAuthenticationService;

    @Autowired
    private AuthSourceService authSourceService;

    @Autowired
    private MessageService messageService;

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 6;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        final RequestContext context = RequestContext.getCurrentContext();

        boolean rejected = false;
        AuthenticationCommand cmd = null;

        final String serviceId = (String) context.get(SERVICE_ID_KEY);
        try {
            Authentication authentication = serviceAuthenticationService.getAuthentication(serviceId);
            Optional<AuthSource> authSource = serviceAuthenticationService.getAuthSourceByAuthentication(authentication);
            cmd = serviceAuthenticationService.getAuthenticationCommand(serviceId, authentication, authSource.orElse(null));

            // Verify authentication source validity if it is required for the schema
            if (authSource.isPresent() && !isSourceValidForCommand(authSource.get(), cmd)) {
                throw new AuthSchemeException("org.zowe.apiml.gateway.security.invalidAuthentication");
            }
        } catch (TokenExpireException tee) {
            String error = this.messageService.createMessage("org.zowe.apiml.gateway.security.expiredToken").mapToLogMessage();
            sendErrorMessage(error, context);
            return null;
        } catch (TokenNotValidException notValidException) {
            String error = this.messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToLogMessage();
            sendErrorMessage(error, context);
            return null;
        } catch (NoMainframeIdentityException noIdentityException) {
            String error = this.messageService.createMessage("org.zowe.apiml.gateway.security.schema.x509.mappingFailed").mapToLogMessage();
            sendErrorMessage(error, context);
            return null;
        } catch (AuthenticationException ae) {
            rejected = true;
        } catch (AuthSchemeException ase) {
            String error;
            if (ase.getParams() != null) {
                error = this.messageService.createMessage(ase.getMessage(), (Object[]) ase.getParams()).mapToLogMessage();
            } else {
                error = this.messageService.createMessage(ase.getMessage()).mapToLogMessage();
            }
            sendErrorMessage(error, context);
            return null;
        } catch (Exception e) {
            throw new ZuulRuntimeException(
                new ZuulException(e, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getLocalizedMessage())
            );
        }

        if (rejected) {
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(SC_UNAUTHORIZED);
        } else if (cmd != null) {
            try {
                // Update ZUUL context by authentication schema
                cmd.apply(null);
            } catch (Exception e) {
                throw new ZuulRuntimeException(
                    new ZuulException(e, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getLocalizedMessage())
                );
            }
        }

        return null;
    }

    private void sendErrorMessage(String error, RequestContext context) {
        logger.log(MessageType.DEBUG, error);
        context.addZuulRequestHeader(ApimlConstants.AUTH_FAIL_HEADER, error);
        context.addZuulResponseHeader(ApimlConstants.AUTH_FAIL_HEADER, error);
    }

    private boolean isSourceValidForCommand(AuthSource authSource, AuthenticationCommand cmd) {
        return !cmd.isRequiredValidSource() || authSourceService.isValid(authSource);
    }

}
