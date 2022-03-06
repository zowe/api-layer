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
import com.netflix.zuul.monitoring.CounterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.security.common.token.TokenExpireException;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@ExtendWith(MockitoExtension.class)
class ServiceAuthenticationFilterTest extends CleanCurrentRequestContextTest {

    @Mock
    private ServiceAuthenticationServiceImpl serviceAuthenticationService;

    @InjectMocks
    private ServiceAuthenticationFilter serviceAuthenticationFilter;

    @Mock
    private AuthenticationCommand command;

    @Mock
    private AuthSourceService authSourceService;

    @Test
    void testConfig() {
        assertEquals("pre", serviceAuthenticationFilter.filterType());
        assertEquals(11, serviceAuthenticationFilter.filterOrder());
        assertTrue(serviceAuthenticationFilter.shouldFilter());
    }

    @Test
    void testRun() {
        Mockito.when(serviceAuthenticationService.getAuthenticationCommand(anyString(), any())).thenReturn(command);

        HttpServletRequest request = mock(HttpServletRequest.class);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);

        JwtAuthSource authSource = new JwtAuthSource("token");
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(authSource));

        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", authSource);
        verify(command, times(1)).apply(null);

        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());
        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", null);
        verify(serviceAuthenticationService, times(2)).getAuthenticationCommand(anyString(), any());

        reset(requestContext);
        reset(authSourceService);
        CounterFactory.initialize(new CounterFactory() {
            @Override
            public void increment(String name) {
            }
        });
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("error");
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("token")));
        when(serviceAuthenticationService.getAuthenticationCommand(eq("error"), any()))
            .thenThrow(new RuntimeException("Potential exception"));
        try {
            serviceAuthenticationFilter.run();
            fail();
        } catch (ZuulRuntimeException zre) {
            assertTrue(zre.getCause() instanceof ZuulException);
            ZuulException ze = (ZuulException) zre.getCause();
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ze.nStatusCode);
            assertEquals(new RuntimeException("Potential exception").getLocalizedMessage(), ze.errorCause);
        }
    }

    private AuthenticationCommand createJwtValidationCommand(String jwtToken) {
        JwtAuthSource authSource = new JwtAuthSource(jwtToken);
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);
        doReturn(Optional.of(authSource)).when(authSourceService).getAuthSourceFromRequest();

        AuthenticationCommand cmd = mock(AuthenticationCommand.class);
        doReturn(cmd).when(serviceAuthenticationService).getAuthenticationCommand("service", authSource);
        doReturn(true).when(cmd).isRequiredValidSource();

        return cmd;
    }

    @Test
    void givenValidJwt_whenTokenRequired_thenCallThrought() {
        String jwtToken = "invalidJwtToken";
        AuthenticationCommand cmd = createJwtValidationCommand(jwtToken);
        doReturn(false).when(cmd).isValidSource(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), times(1)).setSendZuulResponse(false);
        verify(RequestContext.getCurrentContext(), times(1)).setResponseStatusCode(401);
        verify(cmd, never()).apply(any());
    }

    @Test
    void givenValidJwt_whenTokenRequired_thenRejected() {
        String jwtToken = "validJwtToken";
        AuthenticationCommand cmd = createJwtValidationCommand(jwtToken);
        doReturn(true).when(cmd).isValidSource(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), never()).setSendZuulResponse(anyBoolean());
        verify(RequestContext.getCurrentContext(), never()).setResponseStatusCode(anyInt());
        verify(cmd, times(1)).apply(null);
    }

    @Test
    void givenValidJwt_whenCommandFailed_thenInternalError() {
        String jwtToken = "validJwtToken";
        AuthenticationCommand cmd = createJwtValidationCommand(jwtToken);
        doThrow(new RuntimeException()).when(cmd).apply(null);
        doReturn(true).when(cmd).isValidSource(any());
        CounterFactory.initialize(new CounterFactory() {
            @Override
            public void increment(String name) {
            }
        });

        try {
            serviceAuthenticationFilter.run();
            fail();
        } catch (ZuulRuntimeException zre) {
            assertTrue(zre.getCause() instanceof ZuulException);
            ZuulException ze = (ZuulException) zre.getCause();
            assertEquals(500, ze.nStatusCode);
        }
    }

    @Test
    void givenExpiredJwt_thenCallThrought() {
        String jwtToken = "expiredJwtToken";
        AuthenticationCommand cmd = createJwtValidationCommand(jwtToken);
        doThrow(new TokenExpireException("Token is expired.")).when(cmd).isValidSource(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), never()).setSendZuulResponse(false);
        verify(RequestContext.getCurrentContext(), never()).setResponseStatusCode(401);
        verify(cmd, never()).apply(any());
    }

    @Test
    void givenInvalidJwt_whenAuthenticationException_thenReject() {
        String jwtToken = "unparsableJwtToken";
        AuthenticationCommand cmd = createJwtValidationCommand(jwtToken);
        AuthenticationException ae = mock(AuthenticationException.class);
        doThrow(ae).when(cmd).isValidSource(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), times(1)).setSendZuulResponse(false);
        verify(RequestContext.getCurrentContext(), times(1)).setResponseStatusCode(401);
        verify(cmd, never()).apply(any());
    }

}
