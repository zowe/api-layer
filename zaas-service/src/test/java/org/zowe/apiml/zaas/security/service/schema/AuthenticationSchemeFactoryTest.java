/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.zaas.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationSchemeFactoryTest extends CleanCurrentRequestContextTest {

    private static final AuthenticationCommand COMMAND = mock(AuthenticationCommand.class);
    private static final JwtAuthSource JWT_AUTH_SOURCE = new JwtAuthSource("token");
    private static final X509AuthSource X509_AUTH_SOURCE = new X509AuthSource(mock(X509Certificate.class));

    protected static Stream<AuthSource> provideAuthSources() {
        return Stream.of(
            JWT_AUTH_SOURCE,
            X509_AUTH_SOURCE
        );
    }

    private IAuthenticationScheme createScheme(final AuthenticationScheme scheme, final boolean isDefault, AuthSource authSource) {
        return new IAuthenticationScheme() {
            @Override
            public AuthenticationScheme getScheme() {
                return scheme;
            }

            @Override
            public boolean isDefault() {
                return isDefault;
            }

            @Override
            public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
                return COMMAND;
            }

            @Override
            public Optional<AuthSource> getAuthSource() {
                return Optional.of(authSource);
            }
        };
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testInit_OK(AuthSource authSource) {
        assertDoesNotThrow(() -> {
            new AuthenticationSchemeFactory(
                Arrays.asList(
                    createScheme(AuthenticationScheme.BYPASS, true, authSource),
                    createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false, authSource),
                    createScheme(AuthenticationScheme.ZOWE_JWT, false, authSource)
                )
            );
        });
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testInit_NoDefault(AuthSource authSource) {
        List<IAuthenticationScheme> schemes = Arrays.asList(
            createScheme(AuthenticationScheme.BYPASS, false, authSource),
            createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false, authSource),
            createScheme(AuthenticationScheme.ZOWE_JWT, false, authSource)
        );

        // no default
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AuthenticationSchemeFactory(schemes);
        });
        assertTrue(exception.getMessage().contains("No scheme"));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testInit_MultipleDefaults(AuthSource authSource) {
        List<IAuthenticationScheme> schemes = Arrays.asList(
            createScheme(AuthenticationScheme.BYPASS, true, authSource),
            createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, true, authSource),
            createScheme(AuthenticationScheme.ZOWE_JWT, false, authSource)
        );
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AuthenticationSchemeFactory(schemes);
        });
        assertTrue(exception.getMessage().contains("Multiple scheme"));
        assertTrue(exception.getMessage().contains("as default"));
        assertTrue(exception.getMessage().contains(AuthenticationScheme.BYPASS.getScheme()));
        assertTrue(exception.getMessage().contains(AuthenticationScheme.HTTP_BASIC_PASSTICKET.getScheme()));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testInit_MultipleSameScheme(AuthSource authSource) {
        List<IAuthenticationScheme> schemes = Arrays.asList(
            createScheme(AuthenticationScheme.BYPASS, true, authSource),
            createScheme(AuthenticationScheme.BYPASS, false, authSource));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AuthenticationSchemeFactory(schemes);
        });
        assertTrue(exception.getMessage().contains("Multiple beans for scheme"));
        assertTrue(exception.getMessage().contains("AuthenticationSchemeFactoryTest$1"));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testGetSchema(AuthSource authSource) {
        AuthenticationSchemeFactory asf = new AuthenticationSchemeFactory(
            Arrays.asList(
                createScheme(AuthenticationScheme.BYPASS, true, authSource),
                createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false, authSource),
                createScheme(AuthenticationScheme.ZOWE_JWT, false, authSource)
            )
        );

        assertEquals(AuthenticationScheme.BYPASS, asf.getSchema(AuthenticationScheme.BYPASS).getScheme());
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, asf.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET).getScheme());
        assertEquals(AuthenticationScheme.ZOWE_JWT, asf.getSchema(AuthenticationScheme.ZOWE_JWT).getScheme());
        // default one
        assertEquals(AuthenticationScheme.BYPASS, asf.getSchema(null).getScheme());
    }

    @Nested
    class GivenAuthenticationSchemesDefined {
        HttpServletRequest request;
        RequestContext requestContext;

        IAuthenticationScheme byPass;
        IAuthenticationScheme passTicket;
        AuthenticationSchemeFactory asf;
        Authentication authentication;

        @BeforeEach
        void setup() {
            request = mock(HttpServletRequest.class);
            requestContext = new RequestContext();
            requestContext.setRequest(request);

            byPass = spy(createScheme(AuthenticationScheme.BYPASS, true, JWT_AUTH_SOURCE));
            passTicket = spy(createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false, X509_AUTH_SOURCE));

            asf = new AuthenticationSchemeFactory(Arrays.asList(byPass, passTicket));
            authentication = new Authentication(AuthenticationScheme.BYPASS, "applid1");
        }

        @Test
        void whenBypassAuthentication_thenByPassCommandCreate() {
            asf.getAuthenticationCommand(authentication);
            verify(byPass, times(1)).createCommand(authentication, JWT_AUTH_SOURCE);
            verify(passTicket, times(0)).createCommand(authentication, X509_AUTH_SOURCE);
        }

        @Test
        void whenHttpPassTicketAuthentication_thenHttpPassTicketCommandCreate() {
            authentication.setScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET);

            asf.getAuthenticationCommand(authentication);
            verify(byPass, times(0)).createCommand(authentication, JWT_AUTH_SOURCE);
            verify(passTicket, times(1)).createCommand(authentication, X509_AUTH_SOURCE);
        }

        @Test
        void whenAuthenticationSchemeIsNull_thenByPassTicketCommandCreate() {
            authentication.setScheme(null);

            asf.getAuthenticationCommand(authentication);
            verify(byPass, times(1)).createCommand(authentication, JWT_AUTH_SOURCE);
            verify(passTicket, times(0)).createCommand(authentication, X509_AUTH_SOURCE);
        }

        @Test
        void whenNullAuthentication_thenByPassTicketCommandCreate() {
            asf.getAuthenticationCommand(null);
            verify(byPass, times(1)).createCommand(null, JWT_AUTH_SOURCE);
            verify(passTicket, times(0)).createCommand(null, X509_AUTH_SOURCE);
        }
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testUnknownScheme(AuthSource authSource) {
        AuthenticationSchemeFactory asf = new AuthenticationSchemeFactory(
            Arrays.asList(
                createScheme(AuthenticationScheme.BYPASS, true, authSource),
                createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false, authSource),
                createScheme(AuthenticationScheme.ZOWE_JWT, false, authSource)
            )
        );

        assertNotNull(asf.getSchema(AuthenticationScheme.BYPASS));
        assertNotNull(asf.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET));
        assertNotNull(asf.getSchema(AuthenticationScheme.ZOWE_JWT));

        // missing implementation
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> asf.getSchema(AuthenticationScheme.ZOSMF));
        assertTrue(exception.getMessage().contains("Unknown scheme"));

        assertSame(COMMAND, asf.getAuthenticationCommand(new Authentication(AuthenticationScheme.ZOWE_JWT, "applid")));
        assertSame(COMMAND, asf.getAuthenticationCommand(new Authentication(null, "applid")));

        // missing implementation
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, "applid");
        exception = assertThrows(IllegalArgumentException.class, () -> asf.getAuthenticationCommand(authentication));
        assertTrue(exception.getMessage().contains("Unknown scheme"));
    }

}
