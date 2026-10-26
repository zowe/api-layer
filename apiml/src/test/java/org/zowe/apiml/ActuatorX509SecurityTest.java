/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.filters.security.AuthExceptionHandlerReactive;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.handler.LocalTokenProvider;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.util.CertificateLoggingUtils;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.CLIENT_CERT_HEADER;

@ExtendWith(MockitoExtension.class)
class ActuatorX509SecurityTest {

    private static final String EXISTING_JWT = jwt("EXISTINGUSER");
    private static final String MAPPED_JWT = jwt("MAPPEDUSER");
    private static final List<String> INFO_ENDPOINTS = List.of(
        "/application/health", "/application/info", "/application/version", "/gateway/version", "/gateway/api/v1/version"
    );
    private static X509Certificate clientCertificate;
    private static X509Certificate serviceCertificate;

    @Mock private CompoundAuthProvider basicProvider;
    @Mock private LocalTokenProvider tokenProvider;
    @Mock private AuthenticationMapper certificateMapper;
    @Mock private TokenCreationService tokenCreationService;
    @Mock private CertificateValidator certificateValidator;
    @Mock private FailedAuthenticationWebHandler failedAuthenticationHandler;
    @Mock private AuthExceptionHandlerReactive tokenExceptionHandler;
    @Mock private SafResourceAccessVerifying safResourceAccessVerifying;

    private X509AuthenticationProvider x509Provider;
    private WebSecurityConfig config;
    private final AuthConfigurationProperties authProperties = new AuthConfigurationProperties();

    @BeforeAll
    static void loadCertificates() throws Exception {
        clientCertificate = loadCertificate("client/client-certs.p12", "user");
        serviceCertificate = loadCertificate("service/service.keystore.p12", "localhost");
    }

    private static X509Certificate loadCertificate(String file, String alias) throws Exception {
        var keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(Path.of("../keystore", file))) {
            keyStore.load(input, "password".toCharArray());
        }
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    @BeforeEach
    void setUp() {
        x509Provider = new X509AuthenticationProvider(certificateMapper, tokenCreationService);
        ReflectionTestUtils.setField(x509Provider, "isClientCertEnabled", true);
        config = new WebSecurityConfig(basicProvider, x509Provider, tokenProvider,
            Set.of(CertificateLoggingUtils.base64EncodePublicKey(serviceCertificate)), certificateValidator,
            failedAuthenticationHandler, null, null);
        ReflectionTestUtils.setField(config, "isHealthEndpointProtected", true);
        ReflectionTestUtils.setField(config, "internalDiscoveryPort", 10011);
    }

    static Stream<String> protectedEndpoints() {
        return Stream.of(10010, 10011).flatMap(port ->
            Stream.concat(INFO_ENDPOINTS.stream(), Stream.of("/application/loggers", "/application/gateway/routes"))
                .map(path -> "https://localhost:" + port + path));
    }

    static Stream<String> representativeEndpoints() {
        return Stream.of(10010, 10011).flatMap(port ->
            Stream.of("/application/info", "/application/loggers").map(path -> "https://localhost:" + port + path));
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    void mappedCertificateAuthenticatesAsMainframeUser(String uri) {
        allowCertificateMapping();

        var exchange = perform(MockServerHttpRequest.get(uri).sslInfo(sslInfo(clientCertificate)));

        assertAuthenticatedAs(exchange, "MAPPEDUSER");
        verify(certificateMapper).mapToMainframeUserId(new X509AuthSource(clientCertificate));
        verifyNoInteractions(basicProvider, tokenProvider, safResourceAccessVerifying);
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    void missingCredentialsAreRejected(String uri) {
        var exchange = perform(MockServerHttpRequest.get(uri));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @ParameterizedTest
    @MethodSource("representativeEndpoints")
    void unmappedCertificateIsRejected(String uri) {
        var exchange = perform(MockServerHttpRequest.get(uri).sslInfo(sslInfo(clientCertificate)));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(certificateMapper).mapToMainframeUserId(new X509AuthSource(clientCertificate));
        verifyNoInteractions(tokenCreationService);
    }

    @ParameterizedTest
    @MethodSource("representativeEndpoints")
    void disabledCertificateAuthenticationIsRejected(String uri) {
        ReflectionTestUtils.setField(x509Provider, "isClientCertEnabled", false);

        var exchange = perform(MockServerHttpRequest.get(uri).sslInfo(sslInfo(clientCertificate)));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(certificateMapper, tokenCreationService);
    }

    @ParameterizedTest
    @MethodSource("representativeEndpoints")
    void serviceCertificateDoesNotAuthenticateAsAUser(String uri) {
        var exchange = perform(MockServerHttpRequest.get(uri).sslInfo(sslInfo(serviceCertificate)));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(certificateMapper, tokenCreationService);
    }

    static Stream<Arguments> existingCredentials() {
        return representativeEndpoints().flatMap(uri -> Stream.of("basic", "bearer", "cookie")
            .flatMap(type -> Stream.of(false, true).map(withCertificate -> Arguments.of(uri, type, withCertificate))));
    }

    @ParameterizedTest
    @MethodSource("existingCredentials")
    void basicAndJwtContinueToWorkAndTakePrecedence(String uri, String type, boolean withCertificate) {
        var request = MockServerHttpRequest.get(uri);
        if (withCertificate) {
            request.sslInfo(sslInfo(clientCertificate));
        }
        if ("basic".equals(type)) {
            when(basicProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
            when(basicProvider.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("EXISTINGUSER", null, List.of()));
            var headers = new HttpHeaders();
            headers.setBasicAuth("EXISTINGUSER", "password");
            request.headers(headers);
        } else {
            var response = new QueryResponse();
            response.setUserId("EXISTINGUSER");
            when(tokenProvider.validateToken(EXISTING_JWT)).thenReturn(Mono.just(response));
            if ("bearer".equals(type)) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + EXISTING_JWT);
            } else {
                request.cookie(new HttpCookie(authProperties.getCookieProperties().getCookieName(), EXISTING_JWT));
            }
        }

        assertAuthenticatedAs(perform(request), "EXISTINGUSER");
        verifyNoInteractions(certificateMapper, tokenCreationService);
    }

    @Test
    void invalidJwtIsNotBypassedByACertificate() {
        when(tokenProvider.validateToken("invalid-jwt")).thenReturn(Mono.error(new BadCredentialsException("Invalid JWT")));
        when(tokenExceptionHandler.handleTokenNotValid(any())).thenAnswer(invocation -> {
            var exchange = invocation.getArgument(0, org.springframework.web.server.ServerWebExchange.class);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        });

        var exchange = perform(MockServerHttpRequest.get("https://localhost:10010/application/info")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt").sslInfo(sslInfo(clientCertificate)));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(certificateMapper, tokenCreationService);
    }

    @ParameterizedTest
    @ValueSource(ints = {10010, 10011})
    void disablingHealthProtectionKeepsInfoEndpointsPublic(int port) {
        ReflectionTestUtils.setField(config, "isHealthEndpointProtected", false);
        for (var path : INFO_ENDPOINTS) {
            assertAuthenticatedAs(perform(MockServerHttpRequest.get("https://localhost:" + port + path)), "anonymous");
        }
        var protectedExchange = perform(MockServerHttpRequest.get("https://localhost:" + port + "/application/loggers"));
        assertThat(protectedExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(certificateMapper, tokenCreationService);
    }

    static Stream<Arguments> actuatorWrites() {
        return Stream.of(10010, 10011).flatMap(port -> Stream.of(false, true)
            .map(allowed -> Arguments.of(port, allowed)));
    }

    @ParameterizedTest
    @MethodSource("actuatorWrites")
    void actuatorWritesRequireSafPermissionForTheMappedUser(int port, boolean allowed) {
        allowCertificateMapping();
        when(safResourceAccessVerifying.hasSafResourceAccess(
            argThat(auth -> "MAPPEDUSER".equals(auth.getName())), eq("ZOWE"), eq("APIML.DEBUG"), eq("CONTROL")))
            .thenReturn(allowed);

        var exchange = perform(MockServerHttpRequest.method(HttpMethod.POST,
            "https://localhost:" + port + "/application/loggers/ROOT").sslInfo(sslInfo(clientCertificate)));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(allowed ? HttpStatus.OK : HttpStatus.FORBIDDEN);
        verify(safResourceAccessVerifying).hasSafResourceAccess(
            argThat(auth -> "MAPPEDUSER".equals(auth.getName())), eq("ZOWE"), eq("APIML.DEBUG"), eq("CONTROL"));
    }

    @Test
    void trustedProxyCanForwardAClientCertificate() throws Exception {
        allowCertificateMapping();
        when(certificateValidator.isForwardingEnabled()).thenReturn(true);
        when(certificateValidator.hasGatewayChain(any())).thenReturn(true);

        var exchange = perform(MockServerHttpRequest.get("https://localhost:10010/application/info")
            .sslInfo(sslInfo(serviceCertificate))
            .header(CLIENT_CERT_HEADER, Base64.getEncoder().encodeToString(clientCertificate.getEncoded())));

        assertAuthenticatedAs(exchange, "MAPPEDUSER");
        verify(certificateMapper).mapToMainframeUserId(new X509AuthSource(clientCertificate));
    }

    @Test
    void certificateHeaderWithoutTrustedTlsPeerIsRejected() throws Exception {
        var exchange = perform(MockServerHttpRequest.get("https://localhost:10010/application/info")
            .header(CLIENT_CERT_HEADER, Base64.getEncoder().encodeToString(clientCertificate.getEncoded())));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(certificateMapper, tokenCreationService);
    }

    private void allowCertificateMapping() {
        when(certificateMapper.mapToMainframeUserId(new X509AuthSource(clientCertificate))).thenReturn("MAPPEDUSER");
        when(tokenCreationService.createJwtTokenWithoutCredentials("MAPPEDUSER")).thenReturn(MAPPED_JWT);
    }

    private static String jwt(String user) {
        return new PlainJWT(new JWTClaimsSet.Builder().subject(user).issuer("APIML")
            .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(3600))).build()).serialize();
    }

    private MockServerWebExchange perform(MockServerHttpRequest.BaseBuilder<?> request) {
        var exchange = MockServerWebExchange.from(request);
        securityChain().filter(exchange, securedExchange -> securedExchange.<Authentication>getPrincipal()
            .map(Authentication::getName)
            .defaultIfEmpty("anonymous")
            .flatMap(username -> {
                securedExchange.getResponse().setStatusCode(HttpStatus.OK);
                return securedExchange.getResponse().writeWith(Mono.just(securedExchange.getResponse().bufferFactory()
                    .wrap(username.getBytes(StandardCharsets.UTF_8))));
            })).block(Duration.ofSeconds(10));
        return exchange;
    }

    private WebFilterChainProxy securityChain() {
        var chains = Map.of(
            "healthEndpointFilterChain", config.healthEndpointFilterChain(ServerHttpSecurity.http(), authProperties, tokenExceptionHandler),
            "applicationEndpointsProtected", config.applicationEndpointsProtected(ServerHttpSecurity.http(), authProperties,
                tokenExceptionHandler, config.actuatorAuthorizationManager(safResourceAccessVerifying)),
            "discoveryBasicAuthOrToken", config.discoveryBasicAuthOrToken(ServerHttpSecurity.http(), authProperties, tokenExceptionHandler)
        );
        // Match Spring's bean ordering, including the Discovery fallback that also matches actuator requests.
        var orderedChains = Arrays.stream(WebSecurityConfig.class.getDeclaredMethods())
            .filter(method -> chains.containsKey(method.getName()))
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .map(method -> chains.get(method.getName()))
            .toArray(SecurityWebFilterChain[]::new);
        return new WebFilterChainProxy(orderedChains);
    }

    private static SslInfo sslInfo(X509Certificate certificate) {
        return new SslInfo() {
            @Override
            public String getSessionId() {
                return "test-session";
            }

            @Override
            public X509Certificate[] getPeerCertificates() {
                return new X509Certificate[]{certificate};
            }
        };
    }

    private static void assertAuthenticatedAs(MockServerWebExchange exchange, String username) {
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo(username);
    }
}
