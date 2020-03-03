/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.sf.ehcache.Element;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.EurekaUtils;

import javax.servlet.http.Cookie;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    CacheConfig.class,
    AuthenticationServiceTest.Context.class
})
@RunWith(PowerMockRunner.class)
@PrepareForTest(net.sf.ehcache.Cache.class)
public class AuthenticationServiceTest {

    private static final String USER = "Me";
    private static final String DOMAIN = "this.com";
    private static final String LTPA = "ltpaToken";
    private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.RS256;

    private Key privateKey;
    private PublicKey publicKey;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private AuthConfigurationProperties authConfigurationProperties;

    @Autowired
    private JwtSecurityInitializer jwtSecurityInitializer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EurekaClient discoveryClient;

    private void mockJwtSecurityInitializer() {
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        if (keyPair != null) {
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        Mockito.lenient().when(jwtSecurityInitializer.getSignatureAlgorithm()).thenReturn(ALGORITHM);
        Mockito.lenient().when(jwtSecurityInitializer.getJwtSecret()).thenReturn(privateKey);
        Mockito.lenient().when(jwtSecurityInitializer.getJwtPublicKey()).thenReturn(publicKey);
    }

    @BeforeEach
    public void setUp() {
        mockJwtSecurityInitializer();
    }

    @Test
    public void shouldCreateJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        assertFalse(jwtToken.isEmpty());
        assertEquals("java.lang.String", jwtToken.getClass().getName());
    }

    @Test
    public void shouldThrowExceptionWithNullSecret() {
        when(jwtSecurityInitializer.getJwtSecret()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> {
            authService.createJwtToken(USER, DOMAIN, LTPA);
        });
    }

    @Test
    public void shouldValidateJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        TokenAuthentication token = new TokenAuthentication(jwtToken);
        TokenAuthentication jwtValidation = authService.validateJwtToken(token);

        assertEquals(USER, jwtValidation.getPrincipal());
        assertEquals(jwtValidation.getCredentials(), jwtToken);
        assertTrue(jwtValidation.isAuthenticated());
    }

    @Test
    public void shouldThrowExceptionWhenTokenIsInvalid() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        String brokenToken = jwtToken + "not";
        TokenAuthentication token = new TokenAuthentication(brokenToken);
        assertThrows(TokenNotValidException.class, () -> {
            authService.validateJwtToken(token);
        });
    }

    @Test
    public void shouldThrowExceptionWhenTokenIsExpired() {
        TokenAuthentication token = new TokenAuthentication(createExpiredJwtToken(privateKey));
        assertThrows(TokenExpireException.class, () -> {
            authService.validateJwtToken(token);
        });
    }

    @Test
    public void shouldThrowExceptionWhenOccurUnexpectedException() {
        assertThrows(TokenNotValidException.class, () -> {
            authService.validateJwtToken((String) null);
        });
    }

    @Test
    public void shouldThrowExceptionWhenOccurUnexpectedException2() {
        assertThrows(TokenNotValidException.class, () -> {
            authService.validateJwtToken((TokenAuthentication) null);
        });
    }

    @Test
    public void shouldParseJwtTokenAsQueryResponse() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        String dateNow = new Date().toString().substring(0, 16);
        QueryResponse parsedToken = authService.parseJwtToken(jwtToken);

        assertEquals("org.zowe.apiml.security.common.token.QueryResponse", parsedToken.getClass().getTypeName());
        assertEquals(DOMAIN, parsedToken.getDomain());
        assertEquals(USER, parsedToken.getUserId());
        assertEquals(parsedToken.getCreation().toString().substring(0, 16), dateNow);
        Date toBeExpired = DateUtils.addDays(parsedToken.getCreation(), 1);
        assertEquals(parsedToken.getExpiration(), toBeExpired);
    }

    @Test
    public void shouldReadJwtTokenFromRequestCookie() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        MockHttpServletRequest request = new MockHttpServletRequest();

        Optional<String> optionalToken = authService.getJwtTokenFromRequest(request);
        assertFalse(optionalToken.isPresent());

        request.setCookies(new Cookie("apimlAuthenticationToken", jwtToken));

        optionalToken = authService.getJwtTokenFromRequest(request);
        assertTrue(optionalToken.isPresent());
        assertEquals(optionalToken.get(), jwtToken);
    }

    @Test
    public void shouldExtractJwtFromRequestHeader() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        Optional<String> optionalToken = authService.getJwtTokenFromRequest(request);
        assertFalse(optionalToken.isPresent());

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", String.format("Bearer %s", jwtToken));
        optionalToken = authService.getJwtTokenFromRequest(request);
        assertTrue(optionalToken.isPresent());
        assertEquals(optionalToken.get(), jwtToken);
    }

    @Test
    public void shouldReadLtpaTokenFromJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        assertEquals(LTPA, authService.getLtpaTokenFromJwtToken(jwtToken));
    }

    @Test
    public void shouldThrowExceptionWhenTokenIsInvalidWhileExtractingLtpa() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        String brokenToken = jwtToken + "not";
        assertThrows(TokenNotValidException.class, () -> {
            authService.getLtpaTokenFromJwtToken(brokenToken);
        });
    }

    @Test
    public void shouldThrowExceptionWhenTokenIsExpiredWhileExtractingLtpa() {
        assertThrows(TokenExpireException.class, () -> {
            authService.getLtpaTokenFromJwtToken(createExpiredJwtToken(privateKey));
        });
    }

    private String createExpiredJwtToken(Key secretKey) {
        long expiredTimeMillis = System.currentTimeMillis() - 1000;

        return Jwts.builder()
            .setExpiration(new Date(expiredTimeMillis))
            .setIssuer(authConfigurationProperties.getTokenProperties().getIssuer())
            .signWith(ALGORITHM, secretKey)
            .compact();
    }

    private InstanceInfo createInstanceInfo(String instanceId, String hostName, int port, int securePort) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getInstanceId()).thenReturn(instanceId);
        when(out.getHostName()).thenReturn(hostName);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        return out;
    }

    @Test
    public void invalidateToken() {
        TokenAuthentication tokenAuthentication;

        reset(discoveryClient);
        reset(restTemplate);

        String jwt1 = authService.createJwtToken("user1", "domain1", "ltpa1");
        assertFalse(authService.isInvalidated(jwt1));
        tokenAuthentication = authService.validateJwtToken(jwt1);
        assertTrue(tokenAuthentication.isAuthenticated());

        InstanceInfo myInstance = mock(InstanceInfo.class);
        when(myInstance.getInstanceId()).thenReturn("myInstance01");
        ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
        when(applicationInfoManager.getInfo()).thenReturn(myInstance);
        when(discoveryClient.getApplicationInfoManager()).thenReturn(applicationInfoManager);

        Application application = mock(Application.class);
        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("instance02", "hostname1", 10000, 10433),
            createInstanceInfo("myInstance01", "localhost", 10000, 10433),
            createInstanceInfo("instance03", "hostname2", 10001, 0)
        );
        when(application.getInstances()).thenReturn(instances);
        when(discoveryClient.getApplication("gateway")).thenReturn(application);

        authService.invalidateJwtToken(jwt1, true);
        assertTrue(authService.isInvalidated(jwt1));
        tokenAuthentication = authService.validateJwtToken(jwt1);
        assertFalse(tokenAuthentication.isAuthenticated());
        verify(restTemplate, times(2)).delete(anyString(), (Object[]) any());
        verify(restTemplate).delete("https://hostname1:10433/auth/invalidate/{}", jwt1);
        verify(restTemplate).delete("http://hostname2:10001/auth/invalidate/{}", jwt1);
    }

    @Test
    public void invalidateTokenCache() {
        reset(jwtSecurityInitializer);
        mockJwtSecurityInitializer();

        String jwtToken01 = authService.createJwtToken("user01", "domain01", "ltpa01");
        String jwtToken02 = authService.createJwtToken("user02", "domain02", "ltpa02");

        assertFalse(authService.isInvalidated(jwtToken01));
        assertFalse(authService.isInvalidated(jwtToken02));

        verify(jwtSecurityInitializer, never()).getJwtPublicKey();

        assertTrue(authService.validateJwtToken(jwtToken01).isAuthenticated());
        verify(jwtSecurityInitializer, times(1)).getJwtPublicKey();
        assertTrue(authService.validateJwtToken(jwtToken01).isAuthenticated());
        verify(jwtSecurityInitializer, times(1)).getJwtPublicKey();

        assertTrue(authService.validateJwtToken(jwtToken02).isAuthenticated());
        verify(jwtSecurityInitializer, times(2)).getJwtPublicKey();

        authService.invalidateJwtToken(jwtToken01, false);
        assertTrue(authService.validateJwtToken(jwtToken02).isAuthenticated());
        verify(jwtSecurityInitializer, times(2)).getJwtPublicKey();

        assertFalse(authService.validateJwtToken(jwtToken01).isAuthenticated());
        verify(jwtSecurityInitializer, times(3)).getJwtPublicKey();
    }

    @Test
    public void testDistributeInvalidateNotFoundApplication() {
        when(discoveryClient.getApplication("gateway")).thenReturn(null);
        assertFalse(authService.distributeInvalidate("instanceId"));
    }

    @Test
    public void testDistributeInvalidateNotFoundInstance() {
        Application application = mock(Application.class);
        when(application.getByInstanceId("instanceId")).thenReturn(null);

        when(discoveryClient.getApplication("gateway")).thenReturn(application);
        assertFalse(authService.distributeInvalidate("instanceId"));
    }

    @Test
    public void testDistributeInvalidateSuccess() {
        reset(restTemplate);

        InstanceInfo instanceInfo = createInstanceInfo("instanceId", "host", 1000, 1433);

        Application application = mock(Application.class);
        when(application.getByInstanceId("instanceId")).thenReturn(instanceInfo);
        when(discoveryClient.getApplication("gateway")).thenReturn(application);

        Map<Object, Element> mapCache = new HashMap<>();
        mapCache.put("a", new Element("a", "a"));
        mapCache.put("b", new Element("b", "b"));

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        net.sf.ehcache.Cache ehCache = PowerMockito.mock(net.sf.ehcache.Cache.class);
        when(ehCache.getAll(any())).thenReturn(mapCache);
        when(cache.getNativeCache()).thenReturn(ehCache);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        AuthenticationService authenticationService = new AuthenticationService(
            applicationContext, authConfigurationProperties, jwtSecurityInitializer,
            discoveryClient, restTemplate, cacheManager
        );

        when(applicationContext.getBean(AuthenticationService.class)).thenReturn(authenticationService);

        authenticationService.distributeInvalidate(instanceInfo.getInstanceId());

        verify(restTemplate, times(2)).delete(anyString(), anyString());
        verify(restTemplate, times(1)).delete(EurekaUtils.getUrl(instanceInfo) + "/auth/invalidate/{}", "a");
        verify(restTemplate, times(1)).delete(EurekaUtils.getUrl(instanceInfo) + "/auth/invalidate/{}", "a");
    }

    @Configuration
    public static class Context {

        @Autowired
        private ApplicationContext applicationContext;

        @Bean
        public AuthConfigurationProperties getAuthConfigurationProperties() {
            return new AuthConfigurationProperties();
        }

        @Bean
        public JwtSecurityInitializer getJwtSecurityInitializer() {
            return mock(JwtSecurityInitializer.class);
        }

        @Bean
        public RestTemplate getRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public EurekaClient getDiscoveryClient() {
            return mock(EurekaClient.class);
        }

        @Bean
        public AuthenticationService getAuthenticationService(CacheManager cacheManager) {
            return new AuthenticationService(applicationContext, getAuthConfigurationProperties(), getJwtSecurityInitializer(), getDiscoveryClient(), getRestTemplate(), cacheManager);
        }

    }

}
