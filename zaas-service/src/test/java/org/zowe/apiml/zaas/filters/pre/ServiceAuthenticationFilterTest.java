/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.filters.pre;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.CounterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.zaas.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.zaas.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.security.common.token.TokenExpireException;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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

    @Mock
    private static MessageService messageService;

    @Test
    void testConfig() {
        assertEquals("pre", serviceAuthenticationFilter.filterType());
        assertEquals(11, serviceAuthenticationFilter.filterOrder());
        assertTrue(serviceAuthenticationFilter.shouldFilter());
    }

    private static Stream<AuthSource> provideAuthSources() {
        return Stream.of(
            new JwtAuthSource("token"),
            new X509AuthSource(mock(X509Certificate.class))
        );
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testRun(AuthSource authSource) {
        Authentication authentication = new Authentication(AuthenticationScheme.BYPASS, "");
        when(serviceAuthenticationService.getAuthentication(anyString())).thenReturn(authentication);
        when(serviceAuthenticationService.getAuthSourceByAuthentication(authentication)).thenReturn(Optional.of(authSource));
        when(serviceAuthenticationService.getAuthenticationCommand(anyString(), any(), any())).thenReturn(command);

        HttpServletRequest request = mock(HttpServletRequest.class);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);

        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", authentication, authSource);
        verify(command, times(1)).apply(null);

        when(serviceAuthenticationService.getAuthSourceByAuthentication(authentication)).thenReturn(Optional.empty());
        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", authentication, null);
        verify(serviceAuthenticationService, times(2)).getAuthenticationCommand(anyString(), any(Authentication.class), any());

        reset(requestContext);
        reset(authSourceService);
        CounterFactory.initialize(new CounterFactory() {
            @Override
            public void increment(String name) {
            }
        });
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("error");
        when(serviceAuthenticationService.getAuthenticationCommand(eq("error"), any(), any()))
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

    private AuthenticationCommand createValidationCommand(AuthSource authSource) {
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);

        AuthenticationCommand cmd = mock(AuthenticationCommand.class);
        Authentication authentication = new Authentication(AuthenticationScheme.BYPASS, "");
        doReturn(authentication).when(serviceAuthenticationService).getAuthentication("service");
        doReturn(Optional.of(authSource)).when(serviceAuthenticationService).getAuthSourceByAuthentication(authentication);
        doReturn(cmd).when(serviceAuthenticationService).getAuthenticationCommand("service", authentication, authSource);
        doReturn(true).when(cmd).isRequiredValidSource();

        return cmd;
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenInvalidAuthSource_whenAuthSourceRequired_thenCallThrough(AuthSource authSource) {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        doReturn(message).when(messageService).createMessage(anyString(), (Object) any());

        AuthenticationCommand cmd = createValidationCommand(authSource);
        doReturn(false).when(authSourceService).isValid(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext()).addZuulRequestHeader(eq(ApimlConstants.AUTH_FAIL_HEADER), anyString());
        verify(RequestContext.getCurrentContext()).addZuulResponseHeader(eq(ApimlConstants.AUTH_FAIL_HEADER), anyString());
        verify(RequestContext.getCurrentContext(), never()).setResponseStatusCode(anyInt());
        verify(cmd, never()).apply(any());
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenValidAuthSource_whenAuthSourceRequired_thenRejected(AuthSource authSource) {
        AuthenticationCommand cmd = createValidationCommand(authSource);
        doReturn(true).when(authSourceService).isValid(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), never()).setSendZuulResponse(anyBoolean());
        verify(RequestContext.getCurrentContext(), never()).setResponseStatusCode(anyInt());
        verify(cmd, times(1)).apply(null);
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenValidAuthSource_whenCommandFailed_thenInternalError(AuthSource authSource) {
        AuthenticationCommand cmd = createValidationCommand(authSource);
        doThrow(new RuntimeException()).when(cmd).apply(null);
        doReturn(true).when(authSourceService).isValid(any());
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

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenExpiredJwt_thenCallThrough(AuthSource authSource) {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        doReturn(message).when(messageService).createMessage(anyString(), (Object) any());

        AuthenticationCommand cmd = createValidationCommand(authSource);
        doThrow(new TokenExpireException("Token is expired.")).when(authSourceService).isValid(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), never()).setSendZuulResponse(false);
        verify(RequestContext.getCurrentContext(), never()).setResponseStatusCode(401);
        verify(cmd, never()).apply(any());
    }

    @Test
    void givenNoMappedDistributedId_thenCallThrough() {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        doReturn(message).when(messageService).createMessage(anyString(), (Object) any());

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);

        AuthSource authSource = new JwtAuthSource("token");
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, "");
        doReturn(authentication).when(serviceAuthenticationService).getAuthentication("service");
        doReturn(Optional.of(authSource)).when(serviceAuthenticationService).getAuthSourceByAuthentication(authentication);
        doThrow(new NoMainframeIdentityException("User not found."))
            .when(serviceAuthenticationService)
            .getAuthenticationCommand(eq("service"), any(Authentication.class), any(AuthSource.class));

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), never()).setSendZuulResponse(false);
        verify(RequestContext.getCurrentContext(), never()).setResponseStatusCode(401);
    }

    @Test
    void givenNoMappedDistributedIdAndInvalidToken_thenCallThrough() {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        doReturn(message).when(messageService).createMessage(anyString(), (Object) any());
        RequestContext.testSetCurrentContext(new RequestContext());
        doThrow(new NoMainframeIdentityException("User not found.", null, false))
            .when(serviceAuthenticationService)
            .getAuthentication((String) null);

        assertNull(serviceAuthenticationFilter.run());

        assertNotNull(RequestContext.getCurrentContext().getZuulRequestHeaders().get(ApimlConstants.AUTH_FAIL_HEADER.toLowerCase()));
        assertEquals(ApimlConstants.AUTH_FAIL_HEADER, RequestContext.getCurrentContext().getZuulResponseHeaders().get(0).first());
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenInvalidAuthSource_whenAuthenticationException_thenReject(AuthSource authSource) {
        AuthenticationCommand cmd = createValidationCommand(authSource);
        AuthenticationException ae = mock(AuthenticationException.class);
        doThrow(ae).when(authSourceService).isValid(any());

        serviceAuthenticationFilter.run();

        verify(RequestContext.getCurrentContext(), times(1)).setSendZuulResponse(false);
        verify(RequestContext.getCurrentContext(), times(1)).setResponseStatusCode(401);
        verify(cmd, never()).apply(any());
    }

}
