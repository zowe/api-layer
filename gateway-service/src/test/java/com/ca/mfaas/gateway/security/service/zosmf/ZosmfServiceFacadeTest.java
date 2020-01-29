package com.ca.mfaas.gateway.security.service.zosmf;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.mfaas.gateway.security.service.ZosmfService;
import lombok.AllArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class ZosmfServiceFacadeTest {

    private RestTemplate restTemplate = mock(RestTemplate.class);
    private ZosmfServiceFacade zosmfService;

    @Before
    public void setUp() {
        zosmfService = getZosmfServiceFacade();
    }

    public ZosmfServiceFacade getZosmfServiceFacade() {
        AuthConfigurationProperties authConfigurationProperties = mock(AuthConfigurationProperties.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        ZosmfServiceFacade out = new ZosmfServiceFacade(
            authConfigurationProperties,
            null,
            restTemplate,
            null,
            applicationContext,
            Arrays.asList(
                new ZosmfServiceTest(1),
                new ZosmfServiceTest(2)
            )
        ) {
            @Override
            protected String getURI(String zosmf) {
                return "http://zosmf:1433";
            }
        };
        out = spy(out);

        when(applicationContext.getBean(ZosmfServiceFacade.class)).thenReturn(out);
        out.afterPropertiesSet();
        return out;
    }

    private void mockVersion(int version) {
        ZosmfServiceFacade.ZosmfInfo response = new ZosmfServiceFacade.ZosmfInfo();
        response.setVersion(version);

        when(restTemplate.exchange(
            eq("http://zosmf:1433/zosmf/info"),
            eq(HttpMethod.GET),
            (HttpEntity<?>) any(),
            eq(ZosmfServiceFacade.ZosmfInfo.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    }

    @Test
    public void testValidate() {
        mockVersion(1);

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
            fail();
        } catch (TestException te) {
            assertEquals(1, te.version);
            assertEquals("validate", te.method);
            assertEquals(ZosmfService.TokenType.JWT, te.arguments[0]);
            assertEquals("jwt", te.arguments[1]);
        }

        mockVersion(2);

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
            fail();
        } catch (TestException te) {
            assertEquals(2, te.version);
        }
    }

    @Test
    public void testInvalidate() {
        mockVersion(1);

        try {
            zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
            fail();
        } catch (TestException te) {
            assertEquals(1, te.version);
            assertEquals("invalidate", te.method);
            assertEquals(ZosmfService.TokenType.JWT, te.arguments[0]);
            assertEquals("jwt", te.arguments[1]);
        }

        mockVersion(2);

        try {
            zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
            fail();
        } catch (TestException te) {
            assertEquals(2, te.version);
        }
    }

    @Test
    public void testAuthenticate() {
        mockVersion(1);

        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");

        try {
            zosmfService.authenticate(authentication);
            fail();
        } catch (TestException te) {
            assertEquals(1, te.version);
            assertEquals("authenticate", te.method);
            assertSame(authentication, te.arguments[0]);
        }

        mockVersion(2);

        try {
            zosmfService.authenticate(authentication);
            fail();
        } catch (TestException te) {
            assertEquals(2, te.version);
        }
    }

    @Test
    public void testValidateException() {
        ZosmfServiceFacade.ZosmfInfo response = new ZosmfServiceFacade.ZosmfInfo();
        response.setVersion(1);

        when(restTemplate.exchange(
            anyString(), (HttpMethod) any(), (HttpEntity<?>) any(), (Class<?>) any()
        )).thenThrow(new IllegalArgumentException("msg"));

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
            fail();
        } catch (IllegalArgumentException te) {
        }
        verify(zosmfService, times(1)).evictCaches();
    }

    @Test
    public void testUnknownVersion() {
        mockVersion(-1);

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown version of z/OSMF : -1", e.getMessage());
        }
        verify(zosmfService, times(1)).evictCaches();
    }

    @Test
    public void testMatchesVersion() {
        assertFalse(zosmfService.matchesVersion(1));
        assertFalse(zosmfService.matchesVersion(25));
    }

    @AllArgsConstructor
    private static class TestException extends RuntimeException {

        private static final long serialVersionUID = -5481719144373455488L;

        private final String method;
        private final int version;
        private final Object[] arguments;

    }

    @AllArgsConstructor
    private static class ZosmfServiceTest implements ZosmfService {

        private final int version;

        @Override
        public AuthenticationResponse authenticate(Authentication authentication) {
            throw new TestException("authenticate", version, new Object[] {authentication});
        }

        @Override
        public void validate(TokenType type, String token) {
            throw new TestException("validate", version, new Object[] {type, token});
        }

        @Override
        public void invalidate(TokenType type, String token) {
            throw new TestException("invalidate", version, new Object[] {type, token});
        }

        @Override
        public boolean matchesVersion(int version) {
            return this.version == version;
        }

    }

}
