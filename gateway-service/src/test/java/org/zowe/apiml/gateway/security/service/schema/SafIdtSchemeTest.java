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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.saf.SafRestAuthenticationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.security.common.token.QueryResponse.Source;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafIdtSchemeTest {
    private SafIdtScheme underTest;
    private AuthSourceService authSourceService;
    private SafRestAuthenticationService safAuthenticationService;

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        safAuthenticationService = mock(SafRestAuthenticationService.class);
        underTest = new SafIdtScheme(authSourceService, safAuthenticationService);
    }

    @Nested
    class WhenTokenIsRequested {
        AuthenticationCommand commandUnderTest;

        @BeforeEach
        void setCommandUnderTest() {
            JwtAuthSource authSource = mock(JwtAuthSource.class);
            commandUnderTest = underTest.createCommand(null, authSource);
        }

        @Nested
        class ThenValidSafTokenIsProduced {
            @Test
            void givenAuthenticatedJwtToken() {
                InstanceInfo info = mock(InstanceInfo.class);

                String validUsername = "hg679853";
                TokenAuthentication authentication = new TokenAuthentication(validUsername, "validJwtToken");
                authentication.setAuthenticated(true);

                when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("validJwtToken")));
                when(authSourceService.isValid(new JwtAuthSource("validJwtToken"))).thenReturn(true);
                when(authSourceService.parse(new JwtAuthSource("validJwtToken"))).thenReturn(new JwtAuthSource.Parsed(validUsername, new Date(), new Date(), Source.ZOWE));
                when(safAuthenticationService.generate(validUsername)).thenReturn(Optional.of("validTokenValidJwtToken"));

                commandUnderTest.apply(info);

                assertThat(getValueOfZuulHeader(), is("validTokenValidJwtToken"));
            }
        }

        @Nested
        class ThenNoTokenIsProduced {
            @Test
            void givenNoJwtToken() {
                InstanceInfo info = mock(InstanceInfo.class);

                when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());

                commandUnderTest.apply(info);

                assertThat(getValueOfZuulHeader(), is(nullValue()));
            }

            @Test
            void givenInvalidJwtToken() {
                InstanceInfo info = mock(InstanceInfo.class);

                when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("invalidJwtToken")));
                when(authSourceService.isValid(new JwtAuthSource("invalidJwtToken"))).thenReturn(false);

                commandUnderTest.apply(info);

                assertThat(getValueOfZuulHeader(), is(nullValue()));
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
