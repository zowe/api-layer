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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import io.jsonwebtoken.*;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.config.service.security.MockedAuthenticationServiceContext;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfServiceV2;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.CacheUtils;
import org.zowe.apiml.util.EurekaUtils;

import javax.servlet.http.Cookie;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    CacheConfig.class,
    MockedAuthenticationServiceContext.class
})
@PrepareForTest(net.sf.ehcache.Cache.class)
public class AuthenticationServiceTest {

    public static final String ZOSMF = "zosmf";
    private static final String ZOSMF_HOSTNAME = "zosmfhostname";
    private static final int ZOSMF_PORT = 1433;

    private static final String USER = "Me";
    private static final String DOMAIN = "this.com";
    private static final String LTPA = "ltpaToken";
    private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.RS256;

    private Key privateKey;
    private PublicKey publicKey;

    private String zosmfUrl;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private AuthConfigurationProperties authConfigurationProperties;

    @Autowired
    private JwtSecurityInitializer jwtSecurityInitializer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    public static ObjectMapper securityObjectMapper = new ObjectMapper();

    private void mockJwtSecurityInitializer() {
        reset(restTemplate);
        reset(jwtSecurityInitializer);
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        if (keyPair != null) {
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        Mockito.lenient().when(jwtSecurityInitializer.getSignatureAlgorithm()).thenReturn(ALGORITHM);
        Mockito.lenient().when(jwtSecurityInitializer.getJwtSecret()).thenReturn(privateKey);
        Mockito.lenient().when(jwtSecurityInitializer.getJwtPublicKey()).thenReturn(publicKey);
        zosmfUrl = mockZosmfUrl(discoveryClient);
    }

    private String mockZosmfUrl(DiscoveryClient discoveryClient) {
        final InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getHostName()).thenReturn(ZOSMF_HOSTNAME);
        when(instanceInfo.getPort()).thenReturn(ZOSMF_PORT);

        final Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(Collections.singletonList(instanceInfo));
        when(discoveryClient.getApplication(ZOSMF)).thenReturn(application);

        return EurekaUtils.getUrl(instanceInfo);
    }

    @BeforeEach
    public void setUp() {
        mockJwtSecurityInitializer();
    }

    @Test
    void shouldCreateJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        assertFalse(jwtToken.isEmpty());
        assertEquals("java.lang.String", jwtToken.getClass().getName());
    }

    @Test
    void shouldThrowExceptionWithNullSecret() {
        when(jwtSecurityInitializer.getJwtSecret()).thenReturn(null);
        assertThrows(
            IllegalArgumentException.class,
            () -> authService.createJwtToken(USER, DOMAIN, LTPA)
        );
    }

    @Test
    void shouldValidateJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        TokenAuthentication token = new TokenAuthentication(jwtToken);
        TokenAuthentication jwtValidation = authService.validateJwtToken(token);

        assertEquals(USER, jwtValidation.getPrincipal());
        assertEquals(jwtValidation.getCredentials(), jwtToken);
        assertTrue(jwtValidation.isAuthenticated());
    }

    @Test
    void shouldThrowExceptionWhenTokenIsInvalid() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        String brokenToken = jwtToken + "not";
        TokenAuthentication token = new TokenAuthentication(brokenToken);
        assertThrows(
            TokenNotValidException.class,
            () -> authService.validateJwtToken(token)
        );
    }

    @Test
    void shouldThrowExceptionWhenTokenIsExpired() {
        TokenAuthentication token = new TokenAuthentication(createExpiredJwtToken(privateKey));
        assertThrows(
            TokenExpireException.class,
            () -> authService.validateJwtToken(token)
        );
    }

    @Test
    void shouldThrowExceptionWhenOccurUnexpectedException() {
        assertThrows(
            TokenNotValidException.class,
            () -> authService.validateJwtToken((String) null)
        );
    }

    @Test
    void shouldThrowExceptionWhenOccurUnexpectedException2() {
        assertThrows(
            TokenNotValidException.class,
            () -> authService.validateJwtToken((TokenAuthentication) null)
        );
    }

    @Test
    void shouldParseJwtTokenAsQueryResponse() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        String dateNow = new Date().toString().substring(0, 16);
        QueryResponse parsedToken = authService.parseJwtToken(jwtToken);

        assertEquals("org.zowe.apiml.security.common.token.QueryResponse", parsedToken.getClass().getTypeName());
        assertEquals(DOMAIN, parsedToken.getDomain());
        assertEquals(USER, parsedToken.getUserId());
        assertEquals(parsedToken.getCreation().toString().substring(0, 16), dateNow);
        Date toBeExpired = DateUtils.addHours(parsedToken.getCreation(), 24);
        assertEquals(parsedToken.getExpiration(), toBeExpired);
    }

    @Test
    void shouldReadJwtTokenFromRequestCookie() {
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
    void shouldExtractJwtFromRequestHeader() {
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
    void shouldReadLtpaTokenFromJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        assertEquals(LTPA, authService.getLtpaTokenWithValidation(jwtToken));
    }

    @Test
    void shouldThrowExceptionWhenTokenIsInvalidWhileExtractingLtpa() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        String brokenToken = jwtToken + "not";
        assertThrows(
            TokenNotValidException.class,
            () -> authService.getLtpaTokenWithValidation(brokenToken)
        );
    }

    @Test
    void shouldThrowExceptionWhenTokenIsExpiredWhileExtractingLtpa() {
        assertThrows(
            TokenExpireException.class,
            () -> authService.getLtpaTokenWithValidation(createExpiredJwtToken(privateKey))
        );
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
    void invalidateToken() {
        TokenAuthentication tokenAuthentication;

        String jwt1 = authService.createJwtToken("user1", "domain1", "ltpa1");
        assertFalse(authService.isInvalidated(jwt1));
        tokenAuthentication = authService.validateJwtToken(jwt1);
        assertTrue(tokenAuthentication.isAuthenticated());

        InstanceInfo myInstance = mock(InstanceInfo.class);
        when(myInstance.getInstanceId()).thenReturn("myInstance01");
        ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
        when(applicationInfoManager.getInfo()).thenReturn(myInstance);
        when(discoveryClient.getApplicationInfoManager()).thenReturn(applicationInfoManager);
        when(restTemplate.exchange(eq(zosmfUrl + "/zosmf/services/authenticate"), eq(HttpMethod.DELETE), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

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
        verify(restTemplate).delete("https://hostname1:10433/gateway/auth/invalidate/{}", jwt1);
        verify(restTemplate).delete("http://hostname2:10001/gateway/auth/invalidate/{}", jwt1);
        verify(restTemplate, times(1))
            .exchange(eq(zosmfUrl + "/zosmf/services/authenticate"), eq(HttpMethod.DELETE), any(), eq(String.class));
    }

    @Test
    void invalidateTokenCache() {
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

        when(restTemplate.exchange(eq(zosmfUrl + "/zosmf/services/authenticate"), eq(HttpMethod.DELETE), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        authService.invalidateJwtToken(jwtToken01, false);
        assertTrue(authService.validateJwtToken(jwtToken02).isAuthenticated());
        verify(jwtSecurityInitializer, times(2)).getJwtPublicKey();
        verify(restTemplate, times(1))
            .exchange(eq(zosmfUrl + "/zosmf/services/authenticate"), eq(HttpMethod.DELETE), any(), eq(String.class));

        assertFalse(authService.validateJwtToken(jwtToken01).isAuthenticated());
        verify(jwtSecurityInitializer, times(3)).getJwtPublicKey();
    }

    private ZosmfServiceV2 getSpiedZosmfService() {
        return spy(
            new ZosmfServiceV2(
                authConfigurationProperties,
                discoveryClient,
                restTemplate,
                securityObjectMapper
            )
        );
    }

    private AuthenticationService getSpiedAuthenticationService(ZosmfServiceV2 spiedZosmfService) {
        AuthenticationService out = new AuthenticationService(
            applicationContext, authConfigurationProperties, jwtSecurityInitializer,
            spiedZosmfService, discoveryClient, restTemplate, mock(CacheManager.class), mock(CacheUtils.class)
        );
        ReflectionTestUtils.setField(out, "meAsProxy", out);
        return spy(out);
    }

    @Test
    void invalidateZosmfJwtToken() {
        final String token = "zosmfJwtToken";
        final String url = zosmfUrl + "/zosmf/services/authenticate";

        final ZosmfServiceV2 zosmfService = getSpiedZosmfService();
        final AuthenticationService authService = getSpiedAuthenticationService(zosmfService);
        doReturn(new QueryResponse(
            "domain", "userId", new Date(), new Date(), QueryResponse.Source.ZOSMF
        )).when(authService).parseJwtToken(token);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        headers.add(HttpHeaders.COOKIE, "jwtToken=" + token);
        HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
        doReturn(new ResponseEntity<>(HttpStatus.OK)).when(restTemplate)
            .exchange(url, HttpMethod.DELETE, httpEntity, String.class);

        assertTrue(authService.invalidateJwtToken(token, false));
        verify(zosmfService, times(1)).invalidate(ZosmfServiceV2.TokenType.JWT, token);
        verify(restTemplate, times(1))
            .exchange(url, HttpMethod.DELETE, httpEntity, String.class);
    }

    @Test
    void invalidateZosmfLtpaToken() {
        final String jwtToken = "zosmfJwtToken";
        final String ltpaToken = "zosmfLtpaToken";
        final String url = zosmfUrl + "/zosmf/services/authenticate";

        final ZosmfServiceV2 zosmfService = getSpiedZosmfService();
        final AuthenticationService authService = getSpiedAuthenticationService(zosmfService);
        doReturn(new QueryResponse(
            "domain", "userId", new Date(), new Date(), QueryResponse.Source.ZOWE
        )).when(authService).parseJwtToken(jwtToken);
        doReturn(ltpaToken).when(authService).getLtpaToken(jwtToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        headers.add(HttpHeaders.COOKIE, "LtpaToken2=" + ltpaToken);
        HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
        doReturn(new ResponseEntity<>(HttpStatus.OK)).when(restTemplate)
            .exchange(url, HttpMethod.DELETE, httpEntity, String.class);

        assertTrue(authService.invalidateJwtToken(jwtToken, false));
        verify(zosmfService, times(1)).invalidate(ZosmfServiceV2.TokenType.LTPA, ltpaToken);
    }

    @Test
    void testValidateZosmfJwtToken() {
        final String jwtToken = "jwtTokenSource";
        final String userId = "userIdSource";
        final QueryResponse queryResponse = new QueryResponse("domain", userId, new Date(), new Date(), QueryResponse.Source.ZOSMF);

        final ZosmfServiceV2 zosmfService = getSpiedZosmfService();
        final AuthenticationService authService = getSpiedAuthenticationService(zosmfService);

        doAnswer((Answer<Object>) invocation -> {
            assertEquals(jwtToken, invocation.getArgument(0));
            return queryResponse;
        }).when(authService).parseJwtToken(jwtToken);

        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity<?>) any(), (Class<?>) any()))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        TokenAuthentication tokenAuthentication = authService.validateJwtToken(jwtToken);
        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(jwtToken, tokenAuthentication.getCredentials());
        assertEquals(userId, tokenAuthentication.getPrincipal());
        verify(zosmfService, times(1)).validate(ZosmfServiceV2.TokenType.JWT, jwtToken);
    }

    @Test
    void testCreateTokenAuthentication() {
        Consumer<TokenAuthentication> assertTokenAuthentication = x -> {
            assertNotNull(x);
            assertTrue(x.isAuthenticated());
            assertEquals("userXYZ", x.getPrincipal());
            assertEquals("jwtTokenXYZ", x.getCredentials());
        };

        TokenAuthentication tokenAuthentication;

        tokenAuthentication = authService.createTokenAuthentication("userXYZ", "jwtTokenXYZ");
        assertTokenAuthentication.accept(tokenAuthentication);

        tokenAuthentication = authService.validateJwtToken("jwtTokenXYZ");
        assertTokenAuthentication.accept(tokenAuthentication);
    }

    @Test
    void testGetLtpaTokenException() {
        for (String jwtToken : new String[] {"header.body.sign", "wrongJwtToken", ""}) {
            try {
                authService.getLtpaToken(jwtToken);
                fail();
            } catch (TokenNotValidException e) {
                assertTrue(e.getMessage().contains("Token is not valid."));
            }
        }
    }

    @Test
    void testCreateJwtTokenUserExpire() {
        String jwt1 = authService.createJwtToken("user", "domain", "ltpaToken");
        String jwt2 = authService.createJwtToken("expire", "domain", "ltpaToken");

        QueryResponse qr1 = authService.parseJwtToken(jwt1);
        QueryResponse qr2 = authService.parseJwtToken(jwt2);

        assertEquals(
            qr1.getExpiration().getTime() - authConfigurationProperties.getTokenProperties().getExpirationInSeconds() * 1000,
            qr2.getExpiration().getTime() - authConfigurationProperties.getTokenProperties().getShortTtlExpirationInSeconds() * 1000
        );
    }

    @Test
    void testHandleJwtParserException() {
        class AuthenticationServiceExceptionHanlderTest extends AuthenticationService {

            AuthenticationServiceExceptionHanlderTest() {
                super(null, null, null, null, null, null, null, null);
            }

            @Override
            public RuntimeException handleJwtParserException(RuntimeException re) {
                return super.handleJwtParserException(re);
            }

        }

        AuthenticationServiceExceptionHanlderTest as = new AuthenticationServiceExceptionHanlderTest();
        Exception exception;

        exception = as.handleJwtParserException(new ExpiredJwtException(mock(Header.class), mock(Claims.class), "msg"));
        assertTrue(exception instanceof TokenExpireException);
        assertEquals("Token is expired.", exception.getMessage());

        exception = as.handleJwtParserException(new JwtException("msg"));
        assertTrue(exception instanceof TokenNotValidException);
        assertEquals("Token is not valid.", exception.getMessage());

        exception = as.handleJwtParserException(new RuntimeException("msg"));
        assertTrue(exception instanceof TokenNotValidException);
        assertEquals("An internal error occurred while validating the token therefor the token is no longer valid.", exception.getMessage());
    }

    @Test
    void testDistributeInvalidateNotFoundApplication() {
        when(discoveryClient.getApplication("gateway")).thenReturn(null);
        assertFalse(authService.distributeInvalidate("instanceId"));
    }

    @Test
    void testDistributeInvalidateNotFoundInstance() {
        Application application = mock(Application.class);
        when(application.getByInstanceId("instanceId")).thenReturn(null);

        when(discoveryClient.getApplication("gateway")).thenReturn(application);
        assertFalse(authService.distributeInvalidate("instanceId"));
    }

    @Test
    void testDistributeInvalidateSuccess() {
        reset(restTemplate);

        InstanceInfo instanceInfo = createInstanceInfo("instanceId", "host", 1000, 1433);

        Application application = mock(Application.class);
        when(application.getByInstanceId("instanceId")).thenReturn(instanceInfo);
        when(discoveryClient.getApplication("gateway")).thenReturn(application);

        ApplicationContext applicationContext = mock(ApplicationContext.class);

        CacheUtils cacheUtils = mock(CacheUtils.class);
        List<Object> elementsInCache = new ArrayList<>();
        elementsInCache.add("a");
        elementsInCache.add("b");
        when(cacheUtils.getAllRecords(any(), any())).thenReturn(elementsInCache);

        AuthenticationService authenticationService = new AuthenticationService(
            applicationContext, authConfigurationProperties, jwtSecurityInitializer, getSpiedZosmfService(),
            discoveryClient, restTemplate, mock(CacheManager.class), cacheUtils
        );

        when(applicationContext.getBean(AuthenticationService.class)).thenReturn(authenticationService);

        authenticationService.distributeInvalidate(instanceInfo.getInstanceId());

        verify(restTemplate, times(2)).delete(anyString(), anyString());
        verify(restTemplate, times(1)).delete(EurekaUtils.getUrl(instanceInfo) + "/gateway/auth/invalidate/{}", "a");
        verify(restTemplate, times(1)).delete(EurekaUtils.getUrl(instanceInfo) + "/gateway/auth/invalidate/{}", "a");
    }

    @Test
    void givenJwtInCookieAndHeader_whenGetJwtTokenFromRequest_thenPreferCookie() {
        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(cookieName, "jwt1"));
        request.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " jwt2");
        Optional<String> token = authService.getJwtTokenFromRequest(request);
        assertTrue(token.isPresent());
        assertEquals("jwt1", token.get());
    }

    @Test
    void givenOtherCookiesAndJwtInHeader_whenGetJwtTokenFromRequest_thenTakeFromHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("cookie", "value"));
        request.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " jwt");
        Optional<String> token = authService.getJwtTokenFromRequest(request);
        assertTrue(token.isPresent());
        assertEquals("jwt", token.get());
    }
}
