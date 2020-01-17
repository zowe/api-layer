/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package com.ca.mfaas.gateway.security.service.schema;

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.apiml.security.common.auth.AuthenticationScheme;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.gateway.utils.CurrentRequestContextTest;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AuthenticationSchemeFactoryTest extends CurrentRequestContextTest {

    private static final AuthenticationCommand COMMAND = mock(AuthenticationCommand.class);

    private AbstractAuthenticationScheme createScheme(final AuthenticationScheme scheme, final boolean isDefault) {
        return new AbstractAuthenticationScheme() {
            @Override
            public AuthenticationScheme getScheme() {
                return scheme;
            }

            @Override
            public boolean isDefault() {
                return isDefault;
            }

            @Override
            public AuthenticationCommand createCommand(Authentication authentication, QueryResponse token) {
                return COMMAND;
            }
        };
    }

    @Before
    public void init() {
        RequestContext.testSetCurrentContext(null);
    }

    @Test
    public void testInit() {
        // happy day
        new AuthenticationSchemeFactory(
            mock(AuthenticationService.class),
            Arrays.asList(
                createScheme(AuthenticationScheme.BYPASS, true),
                createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false),
                createScheme(AuthenticationScheme.ZOWE_JWT, false)
            )
        );

        // no default
        try {
            new AuthenticationSchemeFactory(
                mock(AuthenticationService.class),
                Arrays.asList(
                    createScheme(AuthenticationScheme.BYPASS, false),
                    createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false),
                    createScheme(AuthenticationScheme.ZOWE_JWT, false)
                )
            );
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("No scheme"));
        }

        // multiple default
        try {
            new AuthenticationSchemeFactory(
                mock(AuthenticationService.class),
                Arrays.asList(
                    createScheme(AuthenticationScheme.BYPASS, true),
                    createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, true),
                    createScheme(AuthenticationScheme.ZOWE_JWT, false)
                )
            );
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Multiple scheme"));
            assertTrue(e.getMessage().contains("as default"));
            assertTrue(e.getMessage().contains(AuthenticationScheme.BYPASS.getScheme()));
            assertTrue(e.getMessage().contains(AuthenticationScheme.HTTP_BASIC_PASSTICKET.getScheme()));
        }
    }

    @Test
    public void testGetSchema() {
        AuthenticationSchemeFactory asf = new AuthenticationSchemeFactory(
            mock(AuthenticationService.class),
            Arrays.asList(
                createScheme(AuthenticationScheme.BYPASS, true),
                createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false),
                createScheme(AuthenticationScheme.ZOWE_JWT, false)
            )
        );

        assertEquals(AuthenticationScheme.BYPASS, asf.getSchema(AuthenticationScheme.BYPASS).getScheme());
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, asf.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET).getScheme());
        assertEquals(AuthenticationScheme.ZOWE_JWT, asf.getSchema(AuthenticationScheme.ZOWE_JWT).getScheme());
        // default one
        assertEquals(AuthenticationScheme.BYPASS, asf.getSchema(null).getScheme());
    }

    @Test
    public void testGetAuthenticationCommand() throws Exception {
        final AbstractAuthenticationScheme byPass = spy(createScheme(AuthenticationScheme.BYPASS, true));
        final AbstractAuthenticationScheme passTicket = spy(createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false));

        AuthenticationService as = mock(AuthenticationService.class);
        AuthenticationSchemeFactory asf = new AuthenticationSchemeFactory(as, Arrays.asList(byPass, passTicket));
        Authentication authentication = new Authentication(AuthenticationScheme.BYPASS, "applid1");

        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContext requestContext = new RequestContext();
        requestContext.setRequest(request);

        QueryResponse qr = new QueryResponse("domain", "userId", new Date(), new Date());
        when(as.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken123"));
        when(as.getJwtTokenFromRequest(null)).thenReturn(Optional.empty());
        when(as.parseJwtToken("jwtToken123")).thenReturn(qr);

        verify(byPass, times(0)).createCommand(authentication, null);
        verify(passTicket, times(0)).createCommand(authentication, null);
        asf.getAuthenticationCommand(authentication);

        verify(byPass, times(1)).createCommand(authentication, null);
        verify(passTicket, times(0)).createCommand(authentication, null);
        authentication.setScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        asf.getAuthenticationCommand(authentication);

        verify(byPass, times(1)).createCommand(authentication, null);
        verify(passTicket, times(1)).createCommand(authentication, null);
        authentication.setScheme(null);
        asf.getAuthenticationCommand(authentication);
        verify(byPass, times(2)).createCommand(authentication, null);
        verify(passTicket, times(1)).createCommand(authentication, null);

        RequestContext.testSetCurrentContext(requestContext);

        verify(byPass, times(0)).createCommand(authentication, qr);
        verify(passTicket, times(0)).createCommand(authentication, qr);
        authentication.setScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        asf.getAuthenticationCommand(authentication);
        verify(byPass, times(0)).createCommand(authentication, qr);
        verify(passTicket, times(1)).createCommand(authentication, qr);
    }

    @Test
    public void testUnknownScheme() throws Exception {
        AuthenticationSchemeFactory asf = new AuthenticationSchemeFactory(
            mock(AuthenticationService.class),
            Arrays.asList(
                createScheme(AuthenticationScheme.BYPASS, true),
                createScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false),
                createScheme(AuthenticationScheme.ZOWE_JWT, false)
            )
        );

        assertNotNull(asf.getSchema(AuthenticationScheme.BYPASS));
        assertNotNull(asf.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET));
        assertNotNull(asf.getSchema(AuthenticationScheme.ZOWE_JWT));
        try {
            // missing implementation
            asf.getSchema(AuthenticationScheme.ZOSMF);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown scheme"));
        }

        assertSame(COMMAND, asf.getAuthenticationCommand(new Authentication(AuthenticationScheme.ZOWE_JWT, "applid")));
        assertSame(COMMAND, asf.getAuthenticationCommand(new Authentication(null, "applid")));
        try {
            // missing implementation
            assertSame(COMMAND, asf.getAuthenticationCommand(new Authentication(AuthenticationScheme.ZOSMF, "applid")));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown scheme"));
        }
    }

}
