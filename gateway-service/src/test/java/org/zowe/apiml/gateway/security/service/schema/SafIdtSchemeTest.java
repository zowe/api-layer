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
import io.jsonwebtoken.Jwts;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.auth.AuthenticationScheme.SAF_IDT;

@ExtendWith(MockitoExtension.class)
class SafIdtSchemeTest {

    private SafIdtScheme underTest;
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    @Mock
    private PassTicketService passTicketService;
    @Mock
    private SafIdtProvider safIdtProvider;

    @BeforeEach
    void setUp() {
        underTest = new SafIdtScheme(authConfigurationProperties, passTicketService, safIdtProvider);
        underTest.initCookieName();
        underTest.defaultIdtExpiration = 10;
    }

    @Nested
    @DisplayName("when token is requested")
    class WhenTokenIsRequestedTests {
        AuthenticationCommand commandUnderTest;

        private static final String USERNAME = "USERNAME";
        private static final String APPLID = "ANYAPPL";
        private static final String PASSTICKET = "PASSTICKET";

        private final Authentication auth = new Authentication(SAF_IDT, APPLID);
        private final QueryResponse queryResponse = new QueryResponse(
                null,
                USERNAME,
                null,
                null,
                null
        );

        @Nested
        @DisplayName("then valid token is produced")
        class ThenValidSafTokenIsProducedTests {

            @BeforeEach
            void setUp() throws IRRPassTicketGenerationException {
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
            }

            @Test
            void givenAuthenticatedJwtToken_whenApply() {
                assertThat(underTest.getScheme(), is(AuthenticationScheme.SAF_IDT));

                String safIdt = Jwts.builder()
                        .setExpiration(new Date(System.currentTimeMillis() + 1000000))
                        .compact();

                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                AuthenticationCommand ac = underTest.createCommand(auth, () -> queryResponse);
                assertNotNull(ac);
                assertFalse(ac.isExpired());
                assertTrue(ac.isRequiredValidJwt());

                ac.apply(null);
                assertThat(getValueOfZuulHeader(), is(safIdt));
            }

            @Test
            void givenAuthenticatedJwtToken_whenApplyToRequest() {
                String safIdt = Jwts.builder()
                        .setExpiration(new Date(System.currentTimeMillis() + 1000000))
                        .compact();

                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                AuthenticationCommand ac = commandUnderTest = underTest.createCommand(auth, () -> queryResponse);
                assertNotNull(ac);

                HttpRequest httpRequest = new HttpGet("/test/request");

                ac.applyToRequest(httpRequest);
                assertThat(httpRequest.getHeaders("X-SAF-Token"),
                        hasItemInArray(hasToString("X-SAF-Token: " + safIdt)));
            }

            @Test
            void givenIdtWithoutExpiration_whenApply() {
                String safIdt = Jwts.builder()
                        .setSubject(USERNAME)
                        .compact();

                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                AuthenticationCommand ac = commandUnderTest = underTest.createCommand(auth, () -> queryResponse);
                assertNotNull(ac);
                assertFalse(ac.isExpired());
            }
        }

        @Nested
        @DisplayName("then no token is produced")
        class ThenNoTokenIsProducedTests {

            @Test
            void givenNoJwtToken() {
                AuthenticationCommand ac = underTest.createCommand(auth, () -> null);
                assertThat(ac, is(AuthenticationCommand.EMPTY));
            }

            @Test
            void givenNoRightsToGeneratePassticket() throws IRRPassTicketGenerationException {
                when(passTicketService.generate(USERNAME, APPLID))
                        .thenThrow(new IRRPassTicketGenerationException(8, 8, 0));

                PassTicketException ex = assertThrows(PassTicketException.class,
                        () -> underTest.createCommand(auth, () -> queryResponse));
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
