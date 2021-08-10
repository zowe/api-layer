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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.SafAuthenticationService;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Optional;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafIdtSchemeTest {
    private SafIdtScheme underTest;
    private AuthenticationService authenticationService;
    private SafAuthenticationService safAuthenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        safAuthenticationService = mock(SafAuthenticationService.class);
        underTest = new SafIdtScheme(authenticationService, safAuthenticationService);
    }

    @Nested
    class WhenTokenIsRequested {
        AuthenticationCommand commandUnderTest;

        @BeforeEach
        void setCommandUnderTest() {
            QueryResponse response = mock(QueryResponse.class);
            Supplier<QueryResponse> supplier = () -> response;
            commandUnderTest = underTest.createCommand(null, supplier);
        }

        @Nested
        class ThenValidSafTokenIsProduced {
            @Test
            void givenAuthenticatedJwtToken() {
                InstanceInfo info = mock(InstanceInfo.class);

                TokenAuthentication authentication = new TokenAuthentication("validJwtToken");
                authentication.setAuthenticated(true);

                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of("validJwtToken"));
                when(authenticationService.validateJwtToken("validJwtToken")).thenReturn(authentication);
                when(safAuthenticationService.generateSafIdt("validJwtToken")).thenReturn("validTokenValidJwtToken");

                commandUnderTest.apply(info);

                assertThat(getValueOfZuulHeader(), is("validTokenValidJwtToken"));
            }
        }

        @Nested
        class ThenNoTokenIsProduced {
            @Test
            void givenNoJwtToken() {
                InstanceInfo info = mock(InstanceInfo.class);

                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.empty());

                commandUnderTest.apply(info);

                assertThat(getValueOfZuulHeader(), is(nullValue()));
            }

            @Test
            void givenInvalidJwtToken() {
                InstanceInfo info = mock(InstanceInfo.class);

                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of("invalidJwtToken"));
                when(authenticationService.validateJwtToken("invalidJwtToken")).thenReturn(new TokenAuthentication("invalidJwtToken"));

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
