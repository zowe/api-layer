/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource.Parsed;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class X509SchemeTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";
    static MessageService messageService;

    RequestContext context;
    HttpServletRequest request;
    X509Certificate x509Certificate;
    AuthSourceService authSourceService;
    X509AuthSource authSource;
    X509AuthSource.Parsed parsedSource;
    X509Scheme x509Scheme;
    Authentication authentication;

    @BeforeAll
    static void setForAll() {
        messageService = new YamlMessageService();
        messageService.loadMessages("/gateway-messages.yml");
    }

    @BeforeEach
    void setup() {
        context = spy(RequestContext.class);
        RequestContext.testSetCurrentContext(context);

        request = mock(HttpServletRequest.class);
        when(context.getRequest()).thenReturn(request);

        x509Certificate = mock(X509Certificate.class);
        authSource = new X509AuthSource(x509Certificate);
        parsedSource = new Parsed("commonName", new Date(), new Date(), Origin.X509, "", "distName");

        authSourceService = mock(AuthSourceService.class);
        x509Scheme = new X509Scheme(authSourceService, messageService);
        authentication = new Authentication(AuthenticationScheme.X509, null, null);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenCertificateInRequest {

        @BeforeEach
        void init() {
            doReturn(Optional.of(new X509AuthSource(x509Certificate))).when(authSourceService).getAuthSourceFromRequest();
            doReturn(true).when(authSourceService).isValid(any(AuthSource.class));
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
        }

        @Test
        void whenPublicCertificateIsRequested_onlyCorrectHeaderIsSet() {
            authentication =
                new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(0)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(0)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getCommonName());
        }

        @Test
        void whenAllHeadersAreRequested_allHeadersAreSet() {
            authentication =
                new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY + "," + DISTINGUISHED_NAME + "," + COMMON_NAME);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);

            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(1)).addZuulRequestHeader(COMMON_NAME, parsedSource.getCommonName());
        }

        @Test
        void certificatePassOnIsSetAfterApply() {
            authentication = new Authentication(AuthenticationScheme.X509, null, PUBLIC_KEY);
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, atLeastOnce()).set(RoutingConstants.FORCE_CLIENT_WITH_APIML_CERT_KEY);
        }

        @Test
        void whenAuthenticationHeadersMissing_thenSendAllHeaders() {
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, parsedSource.getPublicKey());
            verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, parsedSource.getDistinguishedName());
            verify(context, times(1)).addZuulRequestHeader(COMMON_NAME, parsedSource.getCommonName());
        }

        @Test
        void whenUnknownAuthenticationHeader_thenNoHeaderIsSet() {
            authentication =
                new Authentication(AuthenticationScheme.X509, null, "Unknown");
            X509Scheme.X509Command command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
            command.apply(null);
            verifyNoHeadersSet();
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class NoCertificateInRequest {
        @Test
            void givenNoClientCertificate_andX509SchemeRequired_thenNoHeaderIsSet() {
            doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest();

            AuthenticationCommand command = x509Scheme.createCommand(authentication, null);

            assertNotNull(command);
            assertEquals(AuthenticationCommand.EMPTY, command);

            command.apply(null);
            verifyNoHeadersSet();
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class IncorrectCertificateInRequest {
        X509Scheme.X509Command command;

        @BeforeEach
        void init() {
            command = (X509Scheme.X509Command) x509Scheme.createCommand(authentication, authSource);
        }

        @Test
        void givenExceptionDuringParsing_thenNoHeaderIsSet() {
            String errorHeaderValue = "ZWEAG163E Error occurred while parsing X509 certificate.";
            doThrow(new InvalidCertificateException("error")).when(authSourceService).parse(any(AuthSource.class));

            command.apply(null);

            verifyErrorHeaderSet(errorHeaderValue);
            verifyNoHeadersSet();
        }
    }

    private void verifyNoHeadersSet() {
        verify(context, times(0)).addZuulRequestHeader(eq(PUBLIC_KEY), anyString());
        verify(context, times(0)).addZuulRequestHeader(eq(DISTINGUISHED_NAME), anyString());
        verify(context, times(0)).addZuulRequestHeader(eq(COMMON_NAME), anyString());
    }

    private void verifyErrorHeaderSet(String errorMessage) {
        verify(context, times(1)).addZuulRequestHeader("X-Zowe-Auth-Failure", errorMessage);
    }
}
