package org.zowe.apiml.gateway.security.service.zosmf;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import lombok.AllArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class ZosmfServiceFacadeTest {

    private static final String SERVICE_ID = "zosmf";

    private RestTemplate restTemplate = mock(RestTemplate.class);
    private ZosmfServiceFacadeTestExt zosmfService;

    @Before
    public void setUp() {
        zosmfService = getZosmfServiceFacade();
    }

    public ZosmfServiceFacadeTestExt getZosmfServiceFacade() {
        AuthConfigurationProperties authConfigurationProperties = mock(AuthConfigurationProperties.class);
        when(authConfigurationProperties.getZosmfServiceId()).thenReturn(SERVICE_ID);
        when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(SERVICE_ID);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        ZosmfServiceFacadeTestExt out = new ZosmfServiceFacadeTestExt(
            authConfigurationProperties,
            null,
            restTemplate,
            null,
            applicationContext,
            Arrays.asList(
                new ZosmfServiceTest(1),
                new ZosmfServiceTest(2),
                spy(new ZosmfServiceTest2(3))
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
        response.setSafRealm("domain");

        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-ZOSMF-HEADER", "");

        when(restTemplate.exchange(
            eq("http://zosmf:1433/zosmf/info"),
            eq(HttpMethod.GET),
            eq(new HttpEntity<>(headers)),
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

        mockVersion(3);

        zosmfService.validate(ZosmfService.TokenType.LTPA, "lpta");
        verify(zosmfService.getService(3), times(1)).validate(ZosmfService.TokenType.LTPA, "lpta");
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

        mockVersion(3);

        zosmfService.invalidate(ZosmfService.TokenType.LTPA, "lpta");
        verify(zosmfService.getService(3), times(1)).invalidate(ZosmfService.TokenType.LTPA, "lpta");
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

        mockVersion(3);

        zosmfService.authenticate(authentication);
        verify(zosmfService.getService(3), times(1)).authenticate(authentication);
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
            // IllegalArgumentException is expected
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
    public void testIsSupported() {
        assertFalse(zosmfService.isSupported(1));
        assertFalse(zosmfService.isSupported(25));
    }

    @Test
    public void testNoBody() {
        when(restTemplate.exchange(
            anyString(), (HttpMethod) any(), (HttpEntity<?>) any(), (Class<?>) any()
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT));

        try {
            zosmfService.getImplementation();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown version of z/OSMF : 0", e.getMessage());
        }
    }

    @Test
    public void testEvictCacheAllService() {
        zosmfService.evictCacheAllService();
        verify(zosmfService, times(1)).evictCaches();
    }

    @Test
    public void testEvictCacheService() {
        ZosmfServiceFacade zosmfServiceFacade = getZosmfServiceFacade();
        zosmfServiceFacade.evictCacheService(null);
        zosmfServiceFacade.evictCacheService("anyService");
        verify(zosmfServiceFacade, never()).evictCaches();
        zosmfServiceFacade.evictCacheService(SERVICE_ID);
        verify(zosmfServiceFacade, times(1)).evictCaches();
    }

    @Test
    public void testSetDomain() {
        ZosmfService.AuthenticationResponse authenticationResponse = mock(ZosmfService.AuthenticationResponse.class);

        ZosmfService zosmfService = mock(ZosmfService.class);
        when(zosmfService.authenticate(any())).thenReturn(authenticationResponse);

        ZosmfServiceFacade zosmfServiceFacade = getZosmfServiceFacade();
        doReturn(new ZosmfServiceFacade.ImplementationWrapper(null, zosmfService))
            .when(zosmfServiceFacade).getImplementation();

        zosmfServiceFacade.authenticate(null);
        verify(authenticationResponse, never()).setDomain(any());

        ZosmfServiceFacade.ZosmfInfo zosmfInfo = new ZosmfServiceFacade.ZosmfInfo();
        zosmfInfo.setSafRealm("realm");
        doReturn(new ZosmfServiceFacade.ImplementationWrapper(zosmfInfo, zosmfService))
            .when(zosmfServiceFacade).getImplementation();

        zosmfServiceFacade.authenticate(null);
        verify(authenticationResponse, times(1)).setDomain("realm");
    }

    @Test
    public void testReadTokenFromCookie() {
        assertNull(new ZosmfServiceFacadeTestExt(null, null, null, null, null, null).readTokenFromCookie(null, null));
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
        public boolean isSupported(int version) {
            return this.version == version;
        }

    }

    @AllArgsConstructor
    private static class ZosmfServiceTest2 implements ZosmfService {

        private int version;

        @Override
        public AuthenticationResponse authenticate(Authentication authentication) {
            return mock(AuthenticationResponse.class);
        }

        @Override
        public void validate(TokenType type, String token) {

        }

        @Override
        public void invalidate(TokenType type, String token) {

        }

        @Override
        public boolean isSupported(int version) {
            return this.version == version;
        }

    }

    private static class ZosmfServiceFacadeTestExt extends ZosmfServiceFacade {

        public ZosmfServiceFacadeTestExt(AuthConfigurationProperties authConfigurationProperties, DiscoveryClient discovery, RestTemplate restTemplateWithoutKeystore, ObjectMapper securityObjectMapper, ApplicationContext applicationContext, List<ZosmfService> implementations) {
            super(authConfigurationProperties, discovery, restTemplateWithoutKeystore, securityObjectMapper, applicationContext, implementations);
        }

        public ZosmfService getService(int version) {
            for (final ZosmfService zosmfService : implementations) {
                if (zosmfService.isSupported(version)) return zosmfService;
            }
            return null;
        }

        @Override
        protected String readTokenFromCookie(List<String> cookies, String cookieName) {
            return super.readTokenFromCookie(cookies, cookieName);
        }
    }

}
