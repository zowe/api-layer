/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;

import com.nimbusds.jwt.JWTClaimsSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import org.apache.commons.lang.time.DateUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.security.common.util.JWTTestUtils;
import org.zowe.apiml.security.common.util.JwtUtils;
import org.zowe.apiml.util.CacheUtils;
import org.zowe.apiml.util.EurekaUtils;
import org.zowe.apiml.zaas.config.CacheConfig;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest { //NOSONAR, needs to be public

    public static final String ZOSMF = "zosmf";

    private static final String USER = "Me";
    private Set<String> scopes;
    private static final String DOMAIN = "this.com";
    private static final String LTPA = "ltpaToken";
    private static final JWSAlgorithm ALGORITHM = JWSAlgorithm.RS256;

    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    @Mock
    private ApplicationContext applicationContext;

    private AuthenticationService authService;

    private AuthConfigurationProperties authConfigurationProperties;

    @Mock
    private JwtSecurity jwtSecurityInitializer;
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ZosmfService zosmfService;
    @Mock
    private EurekaClient eurekaClient;
    @Mock
    private CacheUtils cacheUtils;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache validatedJwtTokensCache;
    @Mock
    private Cache invalidatedJwtTokensCache;

    static {
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        if (keyPair != null) {
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
    }

    @BeforeEach
    void setup() {
        authConfigurationProperties = new AuthConfigurationProperties();
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        lenient().when(cacheManager.getCache("validatedJwtTokens")).thenReturn(validatedJwtTokensCache);
        lenient().when(cacheManager.getCache("invalidatedJwtTokens")).thenReturn(invalidatedJwtTokensCache);

        authService = new AuthenticationService(
            applicationContext, authConfigurationProperties, jwtSecurityInitializer,
            zosmfService, eurekaClient, restTemplate, cacheManager, cacheUtils
        );
        authService.afterPropertiesSet();

        scopes = new HashSet<>();
        scopes.add("Service1");
        scopes.add("Service2");
    }

    @Nested
    class GivenCorrectInputsTest {

        @BeforeEach
        void setup() {
            stubJWTSecurityForSign();

        }

        @Test
        void thenCreatePersonalAccessToken() {
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            String pat = authService.createLongLivedJwtToken(USER, 60, scopes);
            QueryResponse parsedPAT = authService.parseJwtWithSignature(pat);
            assertEquals(QueryResponse.Source.ZOWE_PAT, parsedPAT.getSource());
        }

        @Test
        void thenCreateValidJwtToken() {
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

            TokenAuthentication token = new TokenAuthentication(jwtToken);
            TokenAuthentication jwtValidation = authService.validateJwtToken(token);

            verify(validatedJwtTokensCache, times(1)).put(jwtToken, jwtValidation);
            assertEquals(USER, jwtValidation.getPrincipal());
            assertEquals(jwtValidation.getCredentials(), jwtToken);
            assertTrue(jwtValidation.isAuthenticated());
        }

        @Test
        void thenParseJwtTokenAsQueryResponse() {
            String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

            String dateNow = new Date().toString().substring(0, 16);
            QueryResponse parsedToken = authService.parseJwtToken(jwtToken).getQueryResponse();

            assertEquals(QueryResponse.class, parsedToken.getClass());
            assertEquals(DOMAIN, parsedToken.getDomain());
            assertEquals(USER, parsedToken.getUserId());
            assertEquals(parsedToken.getCreation().toString().substring(0, 16), dateNow);
            Date toBeExpired = DateUtils.addHours(parsedToken.getCreation(), 8);
            assertEquals(parsedToken.getExpiration(), toBeExpired);
        }

        @Test
        void thenGetTokenWithDefaultExpiration() {
            String jwt1 = authService.createJwtToken("user", DOMAIN, LTPA);

            QueryResponse qr1 = authService.parseJwtToken(jwt1).getQueryResponse();
            Date toBeExpired = DateUtils.addSeconds(qr1.getCreation(), authConfigurationProperties.getTokenProperties().getExpirationInSeconds());
            assertEquals(qr1.getExpiration(), toBeExpired);
        }

        @Test
        void thenGetShortLivedToken() {
            String jwt2 = authService.createJwtToken("expire", DOMAIN, LTPA);
            QueryResponse qr2 = authService.parseJwtToken(jwt2).getQueryResponse();
            Date toBeExpired2 = DateUtils.addSeconds(qr2.getCreation(), (int) authConfigurationProperties.getTokenProperties().getShortTtlExpirationInSeconds());
            assertEquals(qr2.getExpiration(), toBeExpired2);
        }

    }

    @Nested
    class GivenInvalidTokenAuthenticationTest {

        @Test
        void thenThrowTokenNotValidException() {
            stubJWTSecurityForSign();
            String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
            String brokenToken = jwtToken + "not";
            TokenAuthentication tokenAuthentication = new TokenAuthentication(brokenToken);
            assertThrows(
                TokenNotValidException.class,
                () -> authService.validateJwtToken(tokenAuthentication)
            );
            verify(validatedJwtTokensCache, times(1)).get(brokenToken);
        }

        @Test
        void givenNullValue_thenThrowTokenNotValidException() {
            assertThrows(
                TokenNotValidException.class,
                () -> authService.validateJwtToken((TokenAuthentication) null)
            );
            verify(validatedJwtTokensCache, never()).put(any(),any());
        }

        @Test
        void givenExpiredToken_thenThrowsTokenExpireException() {
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            var jwtToken = createExpiredJwtToken(privateKey);
            assertThrows(
                TokenExpireException.class,
                () -> authService.validateJwtToken(jwtToken)
            );

            verify(validatedJwtTokensCache, times(1)).get(jwtToken);
        }

        @Test
        void givenExpiredTokenInvalidatedJwtTokensCache_thenThrowsTokenExpireException() {
            var jwtToken = createExpiredJwtToken(privateKey);
            when(validatedJwtTokensCache.get(jwtToken)).thenReturn(new SimpleValueWrapper( new TokenAuthentication(jwtToken)));

            assertThrows(
                TokenExpireException.class,
                () -> authService.validateJwtToken(jwtToken)
            );

            verify(validatedJwtTokensCache, times(1)).get(jwtToken);
            verify(jwtSecurityInitializer, never()).getJwtVerifier();
            verify(zosmfService, never()).validate(any());
        }

        @Test
        void whenParseJWT_thenThrowTokenNotValidException() {
            String invalidToken = "invalidToken";

            assertThrows(TokenNotValidException.class,
                () -> authService.parseJwtWithSignature(invalidToken));
        }

    }

    @Nested
    class GivenInvalidTokenStringTest {
        @Test
        void thenThrowsTokenNotValidException() {
            assertThrows(
                TokenNotValidException.class,
                () -> authService.validateJwtToken((String) null)
            );
            verify(validatedJwtTokensCache, never()).put(any(),any());
        }
    }

    @Nested
    class GivenReadJWTFromRequestTest {

        @Test
        void givenJwtInCookie_thenReadJwtTokenFromRequestCookie() {
            String jwtToken = "token";
            MockHttpServletRequest request = new MockHttpServletRequest();

            Optional<String> optionalToken = authService.getJwtTokenFromRequest(request);
            assertFalse(optionalToken.isPresent());

            request.setCookies(new Cookie("apimlAuthenticationToken", jwtToken));

            optionalToken = authService.getJwtTokenFromRequest(request);
            assertTrue(optionalToken.isPresent());
            assertEquals(optionalToken.get(), jwtToken);
        }

        @Test
        void givenJwtInAuthorizationHeader_thenReadJwtFromRequestHeader() {
            String jwtToken = "token";
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

        @Nested
        class GivenPriorityOfTokensTest {

            MockHttpServletRequest request;

            @BeforeEach
            void setup() {
                request = new MockHttpServletRequest();
                request.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " jwtInAuthHeader");
            }

            @Test
            void givenJwtInCookieAndHeader_whenGetJwtTokenFromRequest_thenPreferCookie() {
                String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
                request.setCookies(new Cookie(cookieName, "jwtInCookies"));

                Optional<String> token = authService.getJwtTokenFromRequest(request);
                assertTrue(token.isPresent());
                assertEquals("jwtInCookies", token.get());
            }

            @Test
            void givenOtherCookiesAndJwtInHeader_whenGetJwtTokenFromRequest_thenTakeFromHeader() {
                request.setCookies(new Cookie("cookie", "value"));

                Optional<String> token = authService.getJwtTokenFromRequest(request);
                assertTrue(token.isPresent());
                assertEquals("jwtInAuthHeader", token.get());
            }

        }

    }

    @Nested
    class GivenPATInTheRequestTest {
        @Test
        void givenTokenIsAvailableInCookie_thenGetFromCookie() {
            String pat = "personalAccessToken";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("personalAccessToken", pat));
            Optional<String> result = authService.getPATFromRequest(request);
            assertTrue(result.isPresent());
            assertEquals(pat, result.get());
        }

        @Test
        void givenTokenNotPresentInCookie_thenGetFromHeader() {
            String pat = "personalAccessToken";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(ApimlConstants.PAT_HEADER_NAME, pat);
            Optional<String> result = authService.getPATFromRequest(request);
            assertTrue(result.isPresent());
            assertEquals(pat, result.get());
        }

        @Test
        void givenNoTokenInRequest_thenReturnEmptyResult() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            Optional<String> result = authService.getPATFromRequest(request);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class GivenReadLTPATokenTest {

        @Test
        void givenLTPAExists_thenReadLtpaTokenFromJwtToken() {
            stubJWTSecurityForSign();
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
            assertEquals(LTPA, authService.getLtpaToken(jwtToken));
        }

        @Test
        void givenInvalidJWT_thenThrowTokenNotValidException() {
            stubJWTSecurityForSign();
            String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
            String brokenToken = jwtToken + "not";
            assertThrows(
                TokenNotValidException.class,
                () -> authService.getLtpaToken(brokenToken)
            );
        }

        @Test
        void givenExpiredJWT_thenThrowTokenExpireException() {
            var expiredJwtToken = createExpiredJwtToken(privateKey);
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            assertThrows(
                TokenExpireException.class,
                () -> authService.getLtpaToken(expiredJwtToken)
            );
        }

        @Test
        void givenIncorrectLTPAToken_thenThrowTokenNotValidException() {
            for (String jwtToken : new String[]{"header.body.sign", "wrongJwtToken", ""}) {
                Throwable t = assertThrows(TokenNotValidException.class, () -> authService.getLtpaToken(jwtToken));
                assertTrue(t.getMessage().contains("Token is not valid."));
            }
        }

    }

    private String createExpiredJwtToken(PrivateKey privateKey) {
        return createJwtTokenWithExpiry(privateKey, System.currentTimeMillis() - 1000);
    }

    private String createJwtTokenWithExpiry(PrivateKey privateKey, long expireAt) {
        return Jwts.builder()
            .setExpiration(new Date(expireAt))
            .setIssuer(authConfigurationProperties.getTokenProperties().getIssuer())
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

    private InstanceInfo createInstanceInfo(String instanceId, String hostName, int securePort, boolean isSecureEnabled) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getInstanceId()).thenReturn(instanceId);
        when(out.getHostName()).thenReturn(hostName);
        when(out.getSecurePort()).thenReturn(securePort);
        when(out.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(isSecureEnabled);
        return out;
    }

    @Nested
    class GivenInvalidateZosmfTokenTest {

        public static final String JWT_TOKEN = "zosmfJwtToken";
        public static final String LTPA_TOKEN = "zosmfLtpaToken";

        @Test
        void givenNoInstancesAvailable_thenReturnFalse() {
            when(eurekaClient.getApplication(CoreService.ZAAS.getServiceId())).thenReturn(null);
            assertFalse(authService.invalidateJwtToken(JWT_TOKEN, true));
        }

        @Test
        void givenTokenWasNotInvalidateOnAnotherInstance_thenRethrowException() {

            stubJWTSecurityForSign();
            authConfigurationProperties.getTokenProperties().setIssuer(ZOSMF);
            String token = authService.createJwtToken("user", "dom", null);
            doThrow(new BadCredentialsException("Invalid Credentials")).when(zosmfService).invalidate(ZosmfService.TokenType.JWT, token);

            Exception exception = assertThrows(BadCredentialsException.class, () -> {
                authService.invalidateJwtToken(token, false);
            });

            assertEquals("Invalid Credentials", exception.getMessage());
            verify(zosmfService, times(1)).invalidate(ZosmfService.TokenType.JWT, token);
            verify(validatedJwtTokensCache, never()).evict(any());
            verify(invalidatedJwtTokensCache, never()).put(any(), any());
        }

        @Test
        void givenTokenWasAlreadyInvalidateOnAnotherInstance_thenReturnInvalidatedTrue() {
            Application application = mock(Application.class);
            ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
            InstanceInfo instanceInfo = mock(InstanceInfo.class);
            InstanceInfo instanceInfo2 = mock(InstanceInfo.class);
            when(eurekaClient.getApplication(CoreService.ZAAS.getServiceId())).thenReturn(application);
            when(eurekaClient.getApplicationInfoManager()).thenReturn(applicationInfoManager);
            when(applicationInfoManager.getInfo()).thenReturn(instanceInfo);
            when(instanceInfo.getInstanceId()).thenReturn("instanceId");
            when(application.getInstances()).thenReturn(Collections.singletonList(instanceInfo2));
            when(instanceInfo2.getInstanceId()).thenReturn("insncId2");
            when(instanceInfo2.getSecurePort()).thenReturn(100);
            when(instanceInfo2.getHostName()).thenReturn("localhost");

            stubJWTSecurityForSign();
            authConfigurationProperties.getTokenProperties().setIssuer(ZOSMF);
            String token = authService.createJwtToken("user", DOMAIN, null);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();
            when(restTemplate.exchange("http://localhost:0/zaas/api/v1/auth/invalidate",
                HttpMethod.DELETE,
                requestEntity,
                Void.class)).thenReturn(responseEntity);
            doThrow(new BadCredentialsException("Invalid Credentials")).when(zosmfService).invalidate(ZosmfService.TokenType.JWT, token);

            assertTrue(authService.invalidateJwtToken(token, true));
        }

        @Test
        void invalidateZosmfJwtToken() {
            stubJWTSecurityForSign();
            authConfigurationProperties.getTokenProperties().setIssuer(ZOSMF);
            String token = authService.createJwtToken("user", DOMAIN, null);

            assertTrue(authService.invalidateJwtToken(token, false));
            verify(zosmfService, times(1)).invalidate(ZosmfService.TokenType.JWT, token);
            verify(validatedJwtTokensCache, times(1)).evict(token);
            verify(invalidatedJwtTokensCache, times(1)).put(token, Boolean.TRUE);
        }

        @Test
        void invalidateZosmfLtpaToken() {
            stubJWTSecurityForSign();
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            String token = authService.createJwtToken("user", DOMAIN, LTPA_TOKEN);

            assertTrue(authService.invalidateJwtToken(token, false));
            verify(zosmfService, times(1)).invalidate(ZosmfService.TokenType.LTPA, LTPA_TOKEN);
            verify(validatedJwtTokensCache, times(1)).evict(token);
            verify(invalidatedJwtTokensCache, times(1)).put(token, Boolean.TRUE);
        }

        @Test
        void thenValidateZosmfJwtToken() {
            final String userId = "userIdSource";
            stubJWTSecurityForSign();
            authConfigurationProperties.getTokenProperties().setIssuer(ZOSMF);
            String zosmfJwt = authService.createJwtToken(userId, DOMAIN, LTPA_TOKEN);

            when(zosmfService.validate(zosmfJwt)).thenReturn(true);
            TokenAuthentication tokenAuthentication = authService.validateJwtToken(zosmfJwt);
            assertTrue(tokenAuthentication.isAuthenticated());
            assertEquals(zosmfJwt, tokenAuthentication.getCredentials());
            assertEquals(userId, tokenAuthentication.getPrincipal());
            verify(zosmfService, times(1)).validate(zosmfJwt);
            verify(validatedJwtTokensCache, times(1)).put(zosmfJwt, tokenAuthentication);
        }
    }

    @Nested
    class GivenZosmfTokenValidationFails {
        @Test
        void whenValidationFails_thenDoNotCacheResult() {
            stubJWTSecurityForSign();
            authConfigurationProperties.getTokenProperties().setIssuer(ZOSMF);
            String token = authService.createJwtToken("user", DOMAIN, null);

            when(zosmfService.validate(token)).thenThrow(new ServiceNotAccessibleException("All validation strategies failed"));

            assertThrows(ServiceNotAccessibleException.class, () -> authService.validateJwtToken(token));
            verify(validatedJwtTokensCache, never()).put(any(), any());
        }
    }

        @Nested
    class GivenTokenOriginTest {

        private static final String TOKEN = "some_token";

        @Test
        void thenReturnCorrectOrigin() throws ParseException {
            final Map<String, Object> map = new HashMap<>();
            map.put(Claims.ISSUER, "APIML_PAT");
            var tokenClaims = JWTClaimsSet.parse(map);

            AuthSource.Origin originResult;
            try (MockedStatic<JwtUtils> jwtUtilsMock = Mockito.mockStatic(JwtUtils.class)) {
                jwtUtilsMock.when(() -> JwtUtils.getJwtClaims(TOKEN)).thenReturn(tokenClaims);
                originResult = authService.getTokenOrigin(TOKEN);
            }
            assertEquals(AuthSource.Origin.ZOWE_PAT, originResult);
        }

        @Test
        void whenTokenIsExpired_thenThrowException() {
            try (MockedStatic<JwtUtils> jwtUtilsMock = Mockito.mockStatic(JwtUtils.class)) {
                jwtUtilsMock.when(() -> JwtUtils.getJwtClaims(TOKEN)).thenThrow(new TokenExpireException("token is expired"));
                assertThrows(TokenExpireException.class, () -> authService.getTokenOrigin(TOKEN));
            }
        }

        @Test
        void whenTokenIsNotValid_thenThrowException() {
            try (MockedStatic<JwtUtils> jwtUtilsMock = Mockito.mockStatic(JwtUtils.class)) {
                jwtUtilsMock.when(() -> JwtUtils.getJwtClaims(TOKEN)).thenThrow(new TokenNotValidException("token is not valid"));
                assertThrows(TokenNotValidException.class, () -> authService.getTokenOrigin(TOKEN));
            }
        }
    }

    void stubJWTSecurityForSign() {
        lenient().when(jwtSecurityInitializer.getSignatureAlgorithm()).thenReturn(ALGORITHM);
        lenient().when(jwtSecurityInitializer.getJwtAlgorithm()).thenReturn(AlgorithmIdentifiers.RSA_USING_SHA256);
        var jwk = mock(JsonWebKey.class);
        when(jwk.getKeyId()).thenReturn("kid");
        when(jwtSecurityInitializer.getJwkPublicKey()).thenReturn(Optional.of(jwk));
        when(jwtSecurityInitializer.getJwtSecret()).thenReturn(privateKey);
    }

    @Nested
    class GivenJwtInvalidationCacheTest {

        @Test
        void whenTokenAlreadyInvalidated_thenUseCache() {
            stubJWTSecurityForSign();
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));

            String jwtToken01 = authService.createJwtToken("user01", "domain01", "ltpa01");
            when(invalidatedJwtTokensCache.get(jwtToken01)).thenReturn(null);
            authService.invalidateJwtToken(jwtToken01, false);
            verify(validatedJwtTokensCache,times(1)).evict(jwtToken01);
            verify(invalidatedJwtTokensCache,times(1)).put(jwtToken01, Boolean.TRUE);
            verify(zosmfService, times(1)).invalidate(ZosmfService.TokenType.LTPA, "ltpa01");

            Mockito.reset(validatedJwtTokensCache, invalidatedJwtTokensCache, zosmfService);
            when(invalidatedJwtTokensCache.get(jwtToken01)).thenReturn(new SimpleValueWrapper(Boolean.TRUE));
            authService.invalidateJwtToken(jwtToken01, false);
            verify(invalidatedJwtTokensCache, times(1)).get(jwtToken01);
            verify(validatedJwtTokensCache, never()).evict(jwtToken01);
            verify(invalidatedJwtTokensCache, never()).put(jwtToken01, Boolean.TRUE);
            verify(zosmfService, never()).invalidate(any(), any());
        }
    }

    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = { CacheConfig.class, AuthenticationService.class, AuthConfigurationProperties.class })
    class GivenCacheJWTTest {

        @MockitoBean
        private JwtSecurity jwtSecurity;

        @MockitoBean
        private ZosmfService zosmfService;

        @MockitoBean
        private EurekaClient eurekaClient;

        @MockitoBean
        private GatewayClient gatewayClient;

        @MockitoBean(name = "restTemplateWithKeystore")
        private RestTemplate restTemplateWithKeystore;

        @Autowired
        private AuthenticationService authService;

        @Autowired
        private JwtSecurity jwtSecurityInitializer;

        @Test
        void thenUseCache() {
            when(jwtSecurityInitializer.getSignatureAlgorithm()).thenReturn(ALGORITHM);
            when(jwtSecurityInitializer.getJwtAlgorithm()).thenReturn(AlgorithmIdentifiers.RSA_USING_SHA256);
            when(jwtSecurityInitializer.getJwtSecret()).thenReturn(privateKey);
            when(jwtSecurityInitializer.getJwtPublicKey()).thenReturn(publicKey);
            when(jwtSecurityInitializer.getJwtVerifier()).thenReturn(new RSASSAVerifier((RSAPublicKey) publicKey));
            var jwk = mock(JsonWebKey.class);
            when(jwk.getKeyId()).thenReturn("kid");
            when(jwtSecurityInitializer.getJwkPublicKey()).thenReturn(Optional.of(jwk));
            String jwtToken01 = authService.createJwtToken("user01", "domain01", "ltpa01");
            String jwtToken02 = authService.createJwtToken("user02", "domain02", "ltpa02");

            assertFalse(authService.isInvalidated(jwtToken01));
            assertFalse(authService.isInvalidated(jwtToken02));

            verify(jwtSecurityInitializer, never()).getJwtPublicKey();

            assertTrue(authService.validateJwtToken(jwtToken01).isAuthenticated());
            verify(jwtSecurityInitializer, times(1)).getJwtVerifier();
            assertTrue(authService.validateJwtToken(jwtToken01).isAuthenticated());
            verify(jwtSecurityInitializer, times(1)).getJwtVerifier();

            assertTrue(authService.validateJwtToken(jwtToken02).isAuthenticated());
            verify(jwtSecurityInitializer, times(2)).getJwtVerifier();

            authService.invalidateJwtToken(jwtToken01, false);
            assertTrue(authService.validateJwtToken(jwtToken02).isAuthenticated());
            verify(jwtSecurityInitializer, times(3)).getJwtVerifier();
            assertThrows(TokenNotValidException.class, () -> authService.validateJwtToken(jwtToken01));
            verify(jwtSecurityInitializer, times(3)).getJwtVerifier();
        }
    }

    @Test
    void givenCreateTokenAuthentication_thenCreateCorrectObject() {
        var user = "userXYZ";
        var token = JWTTestUtils.createDummyAPIMLToken(user);
        Consumer<TokenAuthentication> assertTokenAuthentication = x -> {
            assertNotNull(x);
            assertTrue(x.isAuthenticated());
            assertEquals(user, x.getPrincipal());
            assertEquals(token, x.getCredentials());
        };

        TokenAuthentication tokenAuthentication;

        tokenAuthentication = authService.createTokenAuthentication(user, token);
        assertTokenAuthentication.accept(tokenAuthentication);
    }

    @Nested
    class GivenDistributedInvalidationTest {

        @Test
        void whenNoServiceAvailable_thenReturnFailure() {
            when(eurekaClient.getApplication("zaas")).thenReturn(null);
            assertFalse(authService.distributeInvalidate("instanceId"));
        }

        @Test
        void whenNoInstanceAvailable_thenReturnFailure() {
            Application application = mock(Application.class);
            when(application.getByInstanceId("instanceId")).thenReturn(null);

            when(eurekaClient.getApplication("zaas")).thenReturn(application);
            assertFalse(authService.distributeInvalidate("instanceId"));
        }

        @Test
        void whenInstancesAvailable_thenReturnSuccess() {

            InstanceInfo instanceInfo = createInstanceInfo("instanceId", "host", 1433, true);

            Application application = mock(Application.class);
            when(application.getByInstanceId("instanceId")).thenReturn(instanceInfo);
            when(eurekaClient.getApplication("zaas")).thenReturn(application);

            List<Object> elementsInCache = new ArrayList<>();
            elementsInCache.add("a");
            elementsInCache.add("b");
            when(cacheUtils.getAllRecords(any(), any())).thenReturn(elementsInCache);

            authService.distributeInvalidate(instanceInfo.getInstanceId());

            verify(restTemplate, times(1)).delete(EurekaUtils.getUrl(instanceInfo) + "/zaas/api/v1/auth/invalidate/{}", "a");
            verify(restTemplate, times(1)).delete(EurekaUtils.getUrl(instanceInfo) + "/zaas/api/v1/auth/invalidate/{}", "b");
        }

        @Test
        void givenHttpClientErrorOnInvalidateAnotherInstance_thenReturnFalse() {
            String token = "jwtToken";

            Application application = mock(Application.class);
            ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);

            InstanceInfo myInstance = mock(InstanceInfo.class);
            InstanceInfo otherInstance = mock(InstanceInfo.class);

            when(eurekaClient.getApplication(CoreService.ZAAS.getServiceId()))
                .thenReturn(application);

            when(eurekaClient.getApplicationInfoManager())
                .thenReturn(applicationInfoManager);

            when(applicationInfoManager.getInfo())
                .thenReturn(myInstance);

            when(myInstance.getInstanceId())
                .thenReturn("myInstance");

            when(application.getInstances())
                .thenReturn(List.of(myInstance, otherInstance));

            doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(),any(),any(),(Class<Object>)any());

            assertFalse(authService.invalidateJwtToken(token, true));

        }
    }
}
