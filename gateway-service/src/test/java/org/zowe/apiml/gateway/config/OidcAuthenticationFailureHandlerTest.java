/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.zowe.apiml.gateway.config.oidc.ClientConfiguration;
import org.zowe.apiml.gateway.config.oidc.OidcAuthenticationFailureHandler;
import org.zowe.apiml.gateway.controllers.GatewayExceptionHandler;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OidcAuthenticationFailureHandlerTest {

    private static final String PRIVATE_DESCRIPTION = "provider diagnostic containing private data";
    private static final String CALLBACK = "https://gateway.example/gateway/login/oauth2/code/test?code=private-code&state=state";
    private static final List<String> COOKIES = List.of(WebSecurity.COOKIE_NONCE, WebSecurity.COOKIE_STATE,
        WebSecurity.COOKIE_RETURN_URL, WebSecurity.COOKIE_CODE_VERIFIER);

    private final ObjectMapper mapper = new ObjectMapper();
    private WebSecurity webSecurity;
    private OidcAuthenticationFailureHandler handler;
    private GatewayExceptionHandler exceptionHandler;
    private InMemoryReactiveClientRegistrationRepository registrations;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GatewayExceptionHandler(mapper, new YamlMessageService("/gateway-log-messages.yml"),
            new AcceptHeaderLocaleContextResolver());
        var configuration = mock(ClientConfiguration.class);
        when(configuration.isConfigured()).thenReturn(true);
        var context = mock(ApplicationContext.class);
        when(context.getBean("gatewayExceptionHandler", GatewayExceptionHandler.class)).thenReturn(exceptionHandler);
        webSecurity = new WebSecurity(configuration, null, null, context);
        webSecurity.sameSite = "Lax";
        handler = webSecurity.oidcAuthenticationFailureHandler();
        registrations = new InMemoryReactiveClientRegistrationRepository(ClientRegistration.withRegistrationId("test")
            .clientId("client").clientSecret("secret").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://gateway.example/gateway/login/oauth2/code/test").scope("openid")
            .authorizationUri("https://provider.example/authorize").tokenUri("https://provider.example/token")
            .jwkSetUri("https://provider.example/jwks").build());
    }

    static Stream<Arguments> failures() {
        return Stream.of(
            Arguments.of(oauth2("invalid_request", "Missing code_verifier"), 500, "oidcAuthenticationFailed"),
            Arguments.of(oauth2("provider_specific_error", PRIVATE_DESCRIPTION), 500, "oidcAuthenticationFailed"),
            Arguments.of(oauth2("access_denied", PRIVATE_DESCRIPTION), 401, "oidcLoginRejected"),
            Arguments.of(oauth2("login_required", PRIVATE_DESCRIPTION), 401, "oidcLoginRejected"),
            Arguments.of(oauth2("invalid_grant", PRIVATE_DESCRIPTION), 401, "oidcLoginRejected"),
            Arguments.of(new OAuth2AuthenticationException(new OAuth2Error("invalid_grant"),
                new WebClientRequestException(new IllegalStateException(PRIVATE_DESCRIPTION), HttpMethod.POST,
                    URI.create("https://provider.example/token"), new HttpHeaders())), 401, "oidcLoginRejected"),
            Arguments.of(oauth2("client_registration_not_found", PRIVATE_DESCRIPTION), 400, "oidcInvalidLogin"),
            Arguments.of(oauth2("invalid_id_token", PRIVATE_DESCRIPTION), 500, "oidcConfigurationError"),
            Arguments.of(oauth2("invalid_token_response", PRIVATE_DESCRIPTION), 502, "oidcProviderUnavailable"),
            Arguments.of(new OAuth2AuthorizationException(new OAuth2Error("invalid_token_response"),
                new WebClientRequestException(new IllegalStateException(PRIVATE_DESCRIPTION), HttpMethod.POST,
                    URI.create("https://provider.example/token"), new HttpHeaders())), 503, "oidcProviderUnavailable"),
            Arguments.of(oauth2("invalid_state_parameter", PRIVATE_DESCRIPTION), 400, "oidcInvalidLogin"),
            Arguments.of(oauth2("invalid_nonce", PRIVATE_DESCRIPTION), 400, "oidcInvalidLogin"),
            Arguments.of(oauth2("invalid_client", PRIVATE_DESCRIPTION), 500, "oidcConfigurationError"),
            Arguments.of(oauth2("unauthorized_client", PRIVATE_DESCRIPTION), 500, "oidcConfigurationError"),
            Arguments.of(oauth2("missing_user_info_uri", PRIVATE_DESCRIPTION), 500, "oidcConfigurationError"),
            Arguments.of(oauth2("temporarily_unavailable", PRIVATE_DESCRIPTION), 503, "oidcProviderUnavailable"),
            Arguments.of(oauth2("server_error", PRIVATE_DESCRIPTION), 502, "oidcProviderUnavailable"),
            Arguments.of(new AuthenticationServiceException(PRIVATE_DESCRIPTION), 500, "oidcAuthenticationFailed"),
            Arguments.of(new TimeoutException(PRIVATE_DESCRIPTION), 503, "oidcProviderUnavailable"),
            Arguments.of(new WebClientRequestException(new ConnectException(PRIVATE_DESCRIPTION), HttpMethod.POST,
                URI.create("https://provider.example/token"), new HttpHeaders()), 503, "oidcProviderUnavailable"),
            Arguments.of(new WebClientRequestException(new SSLHandshakeException(PRIVATE_DESCRIPTION), HttpMethod.POST,
                URI.create("https://provider.example/token"), new HttpHeaders()), 502, "oidcProviderUnavailable"),
            Arguments.of(new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response"),
                new TimeoutException(PRIVATE_DESCRIPTION)), 503, "oidcProviderUnavailable"),
            Arguments.of(WebClientResponseException.create(503, PRIVATE_DESCRIPTION, null, null, null),
                503, "oidcProviderUnavailable"),
            Arguments.of(WebClientResponseException.create(504, PRIVATE_DESCRIPTION, null, null, null),
                503, "oidcProviderUnavailable"),
            Arguments.of(WebClientResponseException.create(500, PRIVATE_DESCRIPTION, null, null, null),
                502, "oidcProviderUnavailable")
        );
    }

    @ParameterizedTest
    @MethodSource("failures")
    void failedCallbackReturnsClassifiedErrorWithoutRetry(Exception failure, int status, String key) throws Exception {
        var exchange = callback();
        var filters = securityChain(failure, false);

        StepVerifier.create(filters.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertFailure(exchange, status, key);
        assertThat(OtelRequestContext.of(exchange).toString()).contains("auth.status", "ERROR");
    }

    @Test
    void converterFailureAlsoUsesOidcHandler() throws Exception {
        var exchange = callback();
        var filters = securityChain(new OAuth2AuthorizationException(new OAuth2Error("authorization_request_not_found")), true);

        StepVerifier.create(filters.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertFailure(exchange, 400, "oidcInvalidLogin");
    }

    @Test
    void actualInvalidReturnUrlKeepsItsExistingResponse() {
        var exchange = callback();
        var filters = securityChain(new InvalidForwardException("https://untrusted.example"), false);

        StepVerifier.create(webSecurity.invalidForwardExceptionHandlingFilter()
            .filter(exchange, current -> filters.filter(current, ignored -> Mono.empty()))).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(400);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("org.zowe.apiml.gateway.invalidOidcReturnUrl");
    }

    @Test
    void rejectedLoginReportsLogMessageToOpenTelemetry() {
        var exchange = callback();
        var filters = securityChain(oauth2("access_denied", PRIVATE_DESCRIPTION), false);

        StepVerifier.create(filters.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(OtelRequestContext.of(exchange).toString()).contains("ZWEAG720E OIDC login was unsuccessful");
    }

    @Test
    void nonOidcFailureIsPropagatedWithoutClearingCookies() {
        var exchange = callback();
        var failure = new IllegalStateException(PRIVATE_DESCRIPTION);

        StepVerifier.create(handler.filter(exchange, ignored -> Mono.error(failure)))
            .expectErrorMatches(error -> error == failure).verify();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getCookies()).isEmpty();
    }

    @Test
    void unknownRegistrationIdKeepsBadRequestStatus() {
        var exchange = callback();
        var failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client registration id");

        StepVerifier.create(handler.filter(exchange, ignored -> Mono.error(failure)))
            .expectErrorMatches(error -> error == failure).verify();
        assertThat(exchange.getResponse().getCookies()).isEmpty();
    }

    @Test
    void failureAfterResponseCommittedIsPropagated() {
        var exchange = callback();
        exchange.getResponse().setComplete().block();
        var failure = oauth2("access_denied", PRIVATE_DESCRIPTION);

        StepVerifier.create(handler.filter(exchange, ignored -> Mono.error(failure)))
            .expectErrorMatches(error -> error == failure).verify();
        assertThat(exchange.getResponse().getCookies()).isEmpty();
    }

    private WebFilterChainProxy securityChain(Exception failure, boolean failConversion) {
        var clients = new InMemoryReactiveOAuth2AuthorizedClientService(registrations);
        var http = ServerHttpSecurity.http();
        http.oauth2Login(spec -> spec.clientRegistrationRepository(registrations)
            .authenticationManager(authentication -> Mono.error(failure)));
        http.oauth2Client(spec -> spec.clientRegistrationRepository(registrations)
            .authorizedClientRepository(new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(clients)));
        var repository = mock(WebSecurity.ApimlServerAuthorizationRequestRepository.class);
        var saved = OAuth2AuthorizationRequest.authorizationCode().clientId("client")
            .authorizationUri("https://provider.example/authorize")
            .redirectUri("https://gateway.example/gateway/login/oauth2/code/test").state("state")
            .attributes(attributes -> attributes.put(OAuth2ParameterNames.REGISTRATION_ID, "test")).build();
        when(repository.removeAuthorizationRequest(any())).thenReturn(failConversion ? Mono.error(failure) : Mono.just(saved));
        var chain = webSecurity.oauth2WebFilterChain(http, Optional.of(clients), Optional.of(repository),
            Optional.of(new DefaultServerOAuth2AuthorizationRequestResolver(registrations)));
        return new WebFilterChainProxy(chain);
    }

    private MockServerWebExchange callback() {
        var request = MockServerHttpRequest.get(CALLBACK);
        COOKIES.forEach(name -> request.cookie(new HttpCookie(name, "temporary-value")));
        return MockServerWebExchange.from(request);
    }

    private void assertFailure(MockServerWebExchange exchange, int status, String key) throws Exception {
        var response = exchange.getResponse();
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        var requestId = response.getHeaders().getFirst("X-Request-ID");
        assertThat(requestId).isEqualTo(exchange.getRequest().getId());
        var body = response.getBodyAsString().block();
        assertThat(body).doesNotContain(PRIVATE_DESCRIPTION, "Missing code_verifier", "private-code", "temporary-value");
        var message = mapper.readTree(body).path("messages").get(0);
        assertThat(message.path("messageKey").asText()).isEqualTo("org.zowe.apiml.gateway." + key);
        assertThat(message.path("messageContent").asText()).contains(requestId);
        assertThat(response.getCookies().keySet()).containsExactlyInAnyOrderElementsOf(COOKIES);
        COOKIES.forEach(name -> {
            var cookie = response.getCookies().getFirst(name);
            assertThat(cookie.getMaxAge()).isZero();
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        });
    }

    private static OAuth2AuthenticationException oauth2(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
