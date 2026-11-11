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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.zowe.apiml.acceptance.AcceptanceTest;
import org.zowe.apiml.handler.LocalTokenProvider;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.CLIENT_CERT_HEADER;

@AcceptanceTest
class ActuatorX509SecurityTest {

    private static final String EXISTING_JWT = jwt("EXISTINGUSER");
    private static final String TRUSTED_CERTIFICATE_AUTHORITY = "TRUSTED_CERTIFICATE";
    private static X509Certificate clientCertificate;

    @MockitoSpyBean private CompoundAuthProvider basicProvider;
    @MockitoBean private LocalTokenProvider tokenProvider;
    @MockitoBean private SafResourceAccessVerifying safResourceAccessVerifying;

    @Autowired private WebFilterChainProxy securityChain;

    @BeforeAll
    static void loadCertificates() throws Exception {
        clientCertificate = loadCertificate("client/client-certs.p12", "user");
    }

    private static X509Certificate loadCertificate(String file, String alias) throws Exception {
        var keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(Path.of("../keystore", file))) {
            keyStore.load(input, "password".toCharArray());
        }
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    static Stream<String> representativeEndpoints() {
        return Stream.of(10010, 10011).flatMap(port ->
            Stream.of("/application/info", "/application/loggers").map(path -> "https://localhost:" + port + path));
    }

    @ParameterizedTest
    @MethodSource("representativeEndpoints")
    void givenBasicCredentialsAndCertificate_whenRequestingActuatorEndpoint_thenBasicCredentialsTakePrecedence(String uri) {
        var request = MockServerHttpRequest.get(uri).sslInfo(sslInfo(clientCertificate));
        stubBasicCredentials(request);

        assertAuthenticatedAs(perform(request), "EXISTINGUSER");
    }

    @ParameterizedTest
    @MethodSource("representativeEndpoints")
    void givenJwtAndCertificate_whenRequestingActuatorEndpoint_thenCertificateTakesPrecedence(String uri) {
        var request = MockServerHttpRequest.get(uri).sslInfo(sslInfo(clientCertificate))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + EXISTING_JWT);

        assertAuthenticatedAs(perform(request), TRUSTED_CERTIFICATE_AUTHORITY);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void givenInvalidJwtAndCertificate_whenRequestingActuatorEndpoint_thenCertificateIsAccepted() {
        var exchange = perform(MockServerHttpRequest.get("https://localhost:10010/application/info")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt").sslInfo(sslInfo(clientCertificate)));

        assertAuthenticatedAs(exchange, TRUSTED_CERTIFICATE_AUTHORITY);
    }

    @Test
    void givenInvalidJwtWithoutCertificate_whenRequestingActuatorEndpoint_thenUnauthorized() {
        when(tokenProvider.validateToken("invalid-jwt")).thenReturn(Mono.error(new BadCredentialsException("Invalid JWT")));
        var exchange = perform(MockServerHttpRequest.get("https://localhost:10010/application/info")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void stubBasicCredentials(MockServerHttpRequest.BaseBuilder<?> request) {
        doReturn(true).when(basicProvider).supports(UsernamePasswordAuthenticationToken.class);
        doReturn(UsernamePasswordAuthenticationToken.authenticated("EXISTINGUSER", null, List.of()))
            .when(basicProvider).authenticate(any());
        var headers = new HttpHeaders();
        headers.setBasicAuth("EXISTINGUSER", "password");
        request.headers(headers);
    }

    static Stream<Arguments> actuatorWrites() {
        return Stream.of(10010, 10011).flatMap(port -> Stream.of(false, true)
            .map(allowed -> Arguments.of(port, allowed)));
    }

    @ParameterizedTest
    @MethodSource("actuatorWrites")
    void givenTrustedClientCertificate_whenWritingToActuator_thenSafPermissionDecidesTheOutcome(int port, boolean allowed) {
        when(safResourceAccessVerifying.hasSafResourceAccess(
            argThat(ActuatorX509SecurityTest::isTrustedCertificate), eq("ZOWE"), eq("APIML.DEBUG"), eq("CONTROL")))
            .thenReturn(allowed);

        var exchange = perform(MockServerHttpRequest.method(HttpMethod.POST,
            "https://localhost:" + port + "/application/loggers/ROOT").sslInfo(sslInfo(clientCertificate)));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(allowed ? HttpStatus.OK : HttpStatus.FORBIDDEN);
        verify(safResourceAccessVerifying).hasSafResourceAccess(
            argThat(ActuatorX509SecurityTest::isTrustedCertificate), eq("ZOWE"), eq("APIML.DEBUG"), eq("CONTROL"));
    }

    @Test
    void givenForwardedCertificateHeaderWithoutTlsPeer_whenRequestingActuatorEndpoint_thenUnauthorized() throws Exception {
        var exchange = perform(MockServerHttpRequest.get("https://localhost:10010/application/info")
            .header(CLIENT_CERT_HEADER, Base64.getEncoder().encodeToString(clientCertificate.getEncoded())));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static boolean isTrustedCertificate(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> TRUSTED_CERTIFICATE_AUTHORITY.equals(authority.getAuthority()));
    }

    private static String jwt(String user) {
        return new PlainJWT(new JWTClaimsSet.Builder().subject(user).issuer("APIML")
            .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(3600))).build()).serialize();
    }

    private MockServerWebExchange perform(MockServerHttpRequest.BaseBuilder<?> request) {
        var exchange = MockServerWebExchange.from(request);
        securityChain.filter(exchange, securedExchange -> securedExchange.<Authentication>getPrincipal()
            .map(authentication -> isTrustedCertificate(authentication) ? TRUSTED_CERTIFICATE_AUTHORITY : authentication.getName())
            .defaultIfEmpty("anonymous")
            .flatMap(username -> {
                securedExchange.getResponse().setStatusCode(HttpStatus.OK);
                return securedExchange.getResponse().writeWith(Mono.just(securedExchange.getResponse().bufferFactory()
                    .wrap(username.getBytes(StandardCharsets.UTF_8))));
            })).block(Duration.ofSeconds(10));
        return exchange;
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
