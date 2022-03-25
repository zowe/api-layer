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

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import io.jsonwebtoken.Jwts;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.zowe.apiml.auth.AuthenticationScheme.SAF_IDT;

class SafIdtSchemeTest {
    private SafIdtScheme underTest;
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final JwtAuthSource authSource = new JwtAuthSource("token");
    
    private AuthSourceService authSourceService;
    private PassTicketService passTicketService;
    private SafIdtProvider safIdtProvider;

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        passTicketService = mock(PassTicketService.class);
        safIdtProvider = mock(SafIdtProvider.class);

        underTest = new SafIdtScheme(authConfigurationProperties, authSourceService, passTicketService, safIdtProvider);
        underTest.initCookieName();
        underTest.defaultIdtExpiration = 10;
    }

    @Nested
    class WhenTokenIsRequested {
        private static final String USERNAME = "USERNAME";
        private static final String APPLID = "ANYAPPL";
        private static final String PASSTICKET = "PASSTICKET";

        private final Authentication auth = new Authentication(SAF_IDT, APPLID);
        private final JwtAuthSource.Parsed parsedAuthSource = new JwtAuthSource.Parsed(
                USERNAME,
                null,
                null,
                null
        );

        @Nested
        class ThenValidSafTokenIsProduced {
            @BeforeEach
            void setUp() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
            }
            
            @Nested
            class WhenApply {

                @Test
                void givenAuthenticatedJwtToken() {
                    String safIdt = Jwts.builder()
                            .setExpiration(new Date(System.currentTimeMillis() + 1000000))
                            .compact();

                    when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);
                    
                    AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                    assertNotNull(ac);
                    assertFalse(ac.isExpired());
                    assertTrue(ac.isRequiredValidSource());

                    ac.apply(null);
                    assertThat(getValueOfZuulHeader(), is(safIdt));
                }

                @Test
                void givenIdtWithoutExpiration() {
                    String safIdt = Jwts.builder()
                        .setSubject(USERNAME)
                        .compact();

                    when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                    AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                    assertNotNull(ac);
                    assertFalse(ac.isExpired());
                }
            }

            @Nested
            class WhenApplyToRequest {
                @Test
                void givenAuthenticatedJwtToken() {
                    String safIdt = Jwts.builder()
                        .setExpiration(new Date(System.currentTimeMillis() + 1000000))
                        .compact();

                    when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                    AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                    assertNotNull(ac);

                    HttpRequest httpRequest = new HttpGet("/test/request");

                    ac.applyToRequest(httpRequest);
                    assertThat(httpRequest.getHeaders("X-SAF-Token"),
                            hasItemInArray(hasToString("X-SAF-Token: " + safIdt)));
                    }
            }
        }

        @Nested
        class ThenNoTokenIsProduced {
            @Test
            void givenNoJwtToken() {
                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertThat(ac, is(AuthenticationCommand.EMPTY));
            }

            @Test
            void givenInvalidJwtToken() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID))
                        .thenThrow(new IRRPassTicketGenerationException(8, 8, 0));

                PassTicketException ex = assertThrows(PassTicketException.class,
                        () -> underTest.createCommand(auth, authSource));
                assertThat(ex.getMessage(), allOf(containsString(USERNAME), containsString(APPLID)));
            }
        }
    }

    private String getValueOfZuulHeader() {
        final RequestContext context = RequestContext.getCurrentContext();
        String valueOfHeader = context.getZuulRequestHeaders().get("x-saf-token");
        if (valueOfHeader == null) {
            return null;
        }

        return valueOfHeader.replace("x-saf-token=", "");
    }
}
