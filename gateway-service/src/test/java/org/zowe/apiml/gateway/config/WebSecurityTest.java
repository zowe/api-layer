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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.config.oidc.ClientConfiguration;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import org.zowe.apiml.gateway.service.TokenProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSecurityTest {

    private ReactiveUserDetailsService reactiveUserDetailsService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private BasicAuthProvider basicAuthProvider;
    @Nested
    class WhenListOfAllowedUserDefined {
        WebSecurity webSecurity;

        @BeforeEach
        void setUp() {
            webSecurity = new WebSecurity(new ClientConfiguration(), tokenProvider, basicAuthProvider, applicationContext);
            ReflectionTestUtils.setField(webSecurity, "allowedUsers", "registryUser,registryAdmin");
            webSecurity.initScopes();
            reactiveUserDetailsService = webSecurity.userDetailsService();
        }

        @Test
        void shouldAddRegistryAuthorityToAllowedUser() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("registryUser");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("registryUser");
                    assertThat(details.getAuthorities()).hasSize(1);
                    assertThat(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                        .containsExactly("REGISTRY");
                })
                .verifyComplete();
        }

        @Test
        void whenAccessTokenIsAvailable_thenAddItAsCookie() {
            var token = "thisIsToken";
            var location = "localhost:10010";
            var webFilterExchange = mock(WebFilterExchange.class);
            var exchange = mock(ServerWebExchange.class);
            when(webFilterExchange.getExchange()).thenReturn(exchange);

            var serverHttpResponse = new MockServerHttpResponse();
            when(exchange.getResponse()).thenReturn(serverHttpResponse);

            var serverHttpRequest = mock(ServerHttpRequest.class);
            when(exchange.getRequest()).thenReturn(serverHttpRequest);
            var cookieMap = new LinkedMultiValueMap<String, HttpCookie>();
            cookieMap.add(WebSecurity.COOKIE_RETURN_URL, new HttpCookie(WebSecurity.COOKIE_RETURN_URL, location));
            when(serverHttpRequest.getCookies()).thenReturn(cookieMap);
            var oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
            var accessToken = mock(OAuth2AccessToken.class);
            when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(accessToken);
            when(accessToken.getTokenValue()).thenReturn(token);

            webSecurity.updateCookies(webFilterExchange, oAuth2AuthorizedClient);
            assertEquals(token, serverHttpResponse.getCookies().getFirst("apimlAuthenticationToken").getValue());
            assertEquals("", serverHttpResponse.getCookies().getFirst(WebSecurity.COOKIE_NONCE).getValue());
            assertEquals("", serverHttpResponse.getCookies().getFirst(WebSecurity.COOKIE_STATE).getValue());
            assertEquals(location, serverHttpResponse.getHeaders().getFirst(HttpHeaders.LOCATION));
        }

        @Test
        void shouldAddRegistryAuthorityToAllowedUserIgnoringCase() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("registryadmin");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("registryadmin");
                    assertThat(details.getAuthorities()).hasSize(1);
                    assertThat(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                        .containsExactly("REGISTRY");
                })
                .verifyComplete();
        }

        @Test
        void shouldNotAddRegistryAuthorityToUnknownUser() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("unknownUser");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("unknownUser");
                    assertThat(details.getAuthorities()).isEmpty();
                })
                .verifyComplete();
        }
    }

    @Nested
    class WhenAnyUsersWildcardDefined {
        @BeforeEach
        void setUp() {
            var webSecurity = new WebSecurity(new ClientConfiguration(), tokenProvider, basicAuthProvider, applicationContext);
            ReflectionTestUtils.setField(webSecurity, "allowedUsers", "*");
            webSecurity.initScopes();
            reactiveUserDetailsService = webSecurity.userDetailsService();
        }

        @Test
        void shouldAddRegistryAuthorityToAnyUser() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("guest");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("guest");
                    assertThat(details.getAuthorities()).hasSize(1);
                    assertThat(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                        .containsExactly("REGISTRY");
                })
                .verifyComplete();
        }
    }

    @Test
    void givenValuesInCookies_thenSetTheseValuesInTheAuthorizationRequest() {
        var resolver = mock(ServerOAuth2AuthorizationRequestResolver.class);
        var oauth2AuthReqBuilder = OAuth2AuthorizationRequest.authorizationCode();
        var attrs = new HashMap<String, Object>();
        attrs.put("nonce", "nonceValue");
        oauth2AuthReqBuilder.attributes(attrs).authorizationUri("auth-uri").clientId("okta");
        var oauth2AuthReq = oauth2AuthReqBuilder.build();
        var monoResp = Mono.just(oauth2AuthReq);
        when(resolver.resolve(any(), any())).thenReturn(monoResp);
        var requestRepository = new WebSecurity(null, tokenProvider, basicAuthProvider, applicationContext).new ApimlServerAuthorizationRequestRepository(resolver);
        var exchange = mock(ServerWebExchange.class);
        var request = mock(ServerHttpRequest.class);
        when(exchange.getRequest()).thenReturn(request);
        var cookieMap = new LinkedMultiValueMap<String, HttpCookie>();
        cookieMap.add(WebSecurity.COOKIE_NONCE, new HttpCookie(WebSecurity.CONTEXT_PATH, "nonceValue"));
        cookieMap.add(WebSecurity.COOKIE_STATE, new HttpCookie(WebSecurity.COOKIE_STATE, "stateValue"));
        when(request.getCookies()).thenReturn(cookieMap);
        var requestPath = mock(RequestPath.class);
        when(requestPath.value()).thenReturn("/gateway/login/oauth2/code/okta");
        when(request.getPath()).thenReturn(requestPath);

        var response = requestRepository.loadAuthorizationRequest(exchange);
        AtomicReference<OAuth2AuthorizationRequest> requestRef = new AtomicReference<>();
        response.subscribe(requestRef::set);
        assertEquals("nonceValue", requestRef.get().getAttributes().get(OidcParameterNames.NONCE));
        assertEquals("stateValue", requestRef.get().getState());
    }

    @Test
    void oauth2WebFilterChain_whenClientConfigurationNotConfigured_shouldReturnNull() {
        ServerHttpSecurity http = mock(ServerHttpSecurity.class);
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);
        when(mockClientConfig.isConfigured()).thenReturn(false);

        SecurityWebFilterChain filterChain = webSecurity.oauth2WebFilterChain(http,
            Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(filterChain).isNull();
    }

    @Test
    void oauth2WebFilterChain_whenClientConfigurationConfigured_shouldConfigureSecurityChain() {
        ServerHttpSecurity http = mock(ServerHttpSecurity.class);
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);
        when(mockClientConfig.isConfigured()).thenReturn(true);

        ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchangeSpec = mock(ServerHttpSecurity.AuthorizeExchangeSpec.class);
        ServerHttpSecurity.AuthorizeExchangeSpec.Access access = mock(ServerHttpSecurity.AuthorizeExchangeSpec.Access.class);
        ServerHttpSecurity.OAuth2LoginSpec oauth2LoginSpec = mock(ServerHttpSecurity.OAuth2LoginSpec.class);
        ServerHttpSecurity.HeaderSpec headerSpec = mock(ServerHttpSecurity.HeaderSpec.class);

        when(http.headers(any())).thenReturn(http);
        when(http.securityContextRepository(any())).thenReturn(http);
        when(http.securityMatcher(any())).thenReturn(http);

        doAnswer(invocation -> {
            Customizer<ServerHttpSecurity.AuthorizeExchangeSpec> customizer = invocation.getArgument(0);
            customizer.customize(authorizeExchangeSpec);
            return http;
        }).when(http).authorizeExchange(any(Customizer.class));

        when(authorizeExchangeSpec.anyExchange()).thenReturn(access);
        when(access.authenticated()).thenReturn(authorizeExchangeSpec);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.oauth2Client(any())).thenReturn(http);
        when(http.requestCache(any())).thenReturn(http);
        when(http.build()).thenReturn(mock(SecurityWebFilterChain.class));

        SecurityWebFilterChain filterChain = webSecurity.oauth2WebFilterChain(http,
            Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(filterChain).isNotNull();
        verify(http).headers(any());
        verify(http).authorizeExchange(any(Customizer.class));
        verify(http).build();
    }

    @Test
    void oauth2WebFilterChain_whenClientConfigurationConfigured_shouldConfigureSecurityChain2() {

        ServerHttpSecurity http = mock(ServerHttpSecurity.class);
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);

        ReactiveOAuth2AuthorizedClientService clientService = mock(ReactiveOAuth2AuthorizedClientService.class);
        WebSecurity.ApimlServerAuthorizationRequestRepository requestRepository = mock(WebSecurity.ApimlServerAuthorizationRequestRepository.class);
        ServerOAuth2AuthorizationRequestResolver authResolver = mock(ServerOAuth2AuthorizationRequestResolver.class);

        ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchangeSpec = mock(ServerHttpSecurity.AuthorizeExchangeSpec.class);
        ServerHttpSecurity.AuthorizeExchangeSpec.Access access = mock(ServerHttpSecurity.AuthorizeExchangeSpec.Access.class);
        ServerHttpSecurity.OAuth2LoginSpec oauth2LoginSpec = mock(ServerHttpSecurity.OAuth2LoginSpec.class);
        ServerHttpSecurity.OAuth2ClientSpec oauth2ClientSpec = mock(ServerHttpSecurity.OAuth2ClientSpec.class);
        ServerHttpSecurity.RequestCacheSpec requestCacheSpec = mock(ServerHttpSecurity.RequestCacheSpec.class);

        when(mockClientConfig.isConfigured()).thenReturn(true);

        when(http.headers(any())).thenReturn(http);

        when(http.securityContextRepository(any())).thenReturn(http);
        when(http.securityMatcher(any())).thenReturn(http);

        doAnswer(invocation -> {
            Customizer<ServerHttpSecurity.AuthorizeExchangeSpec> customizer = invocation.getArgument(0);
            customizer.customize(authorizeExchangeSpec);
            return http;
        }).when(http).authorizeExchange(any(Customizer.class));
        when(authorizeExchangeSpec.anyExchange()).thenReturn(access);
        when(access.authenticated()).thenReturn(authorizeExchangeSpec);

        when(http.oauth2Login(any())).thenAnswer(invocation -> {
            Customizer<ServerHttpSecurity.OAuth2LoginSpec> customizer = invocation.getArgument(0);
            customizer.customize(oauth2LoginSpec);
            return http;
        });
        when(oauth2LoginSpec.authenticationMatcher(any())).thenReturn(oauth2LoginSpec);
        when(oauth2LoginSpec.authorizationRequestRepository(any())).thenReturn(oauth2LoginSpec);
        when(oauth2LoginSpec.authorizationRequestResolver(any())).thenReturn(oauth2LoginSpec);
        when(oauth2LoginSpec.authenticationSuccessHandler((ServerAuthenticationSuccessHandler) any())).thenReturn(oauth2LoginSpec);
        when(oauth2LoginSpec.authenticationFailureHandler(any())).thenReturn(oauth2LoginSpec);

        when(http.oauth2Client(any())).thenAnswer(invocation -> {
            Customizer<ServerHttpSecurity.OAuth2ClientSpec> customizer = invocation.getArgument(0);
            customizer.customize(oauth2ClientSpec);
            return http;
        });

        when(http.requestCache(any())).thenAnswer(invocation -> {
            Customizer<ServerHttpSecurity.RequestCacheSpec> customizer = invocation.getArgument(0);
            customizer.customize(requestCacheSpec);
            return http;
        });

        when(http.build()).thenReturn(mock(SecurityWebFilterChain.class));

        SecurityWebFilterChain filterChain = webSecurity.oauth2WebFilterChain(http,
            Optional.of(clientService),
            Optional.of(requestRepository),
            Optional.of(authResolver));

        assertThat(filterChain).isNotNull();

        verify(http).headers(any());
        verify(http).securityContextRepository(any(NoOpServerSecurityContextRepository.class));
        verify(http).securityMatcher(any());
        verify(http).authorizeExchange(any());
        verify(authorizeExchangeSpec).anyExchange();
        verify(access).authenticated();
        verify(http).oauth2Login(any());
        verify(oauth2LoginSpec).authenticationMatcher(any());
        verify(oauth2LoginSpec).authorizationRequestRepository(any());
        verify(oauth2LoginSpec).authorizationRequestResolver(any());
        verify(oauth2LoginSpec).authenticationSuccessHandler((ServerAuthenticationSuccessHandler) any());
        verify(oauth2LoginSpec).authenticationFailureHandler(any());
        verify(http).oauth2Client(any());
        verify(http).requestCache(any());
        verify(http).build();
    }

    @Test
    void gatewayReactiveOAuth2AuthorizedClientManager_whenDependenciesProvided_shouldCreateClientManager() {
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        ReactiveClientRegistrationRepository clientRegistrationRepository = mock(ReactiveClientRegistrationRepository.class);
        ReactiveOAuth2AuthorizedClientService authorizedClientService = mock(ReactiveOAuth2AuthorizedClientService.class);

        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);
        when(mockClientConfig.isConfigured()).thenReturn(true);

        ReactiveOAuth2AuthorizedClientManager clientManager = webSecurity.gatewayReactiveOAuth2AuthorizedClientManager(
                Optional.of(clientRegistrationRepository), Optional.of(authorizedClientService));

        assertThat(clientManager).isNotNull();
    }

    @Test
    void authorizationRequestResolver_whenClientConfigurationNotConfigured_shouldReturnNull() {
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);

        when(mockClientConfig.isConfigured()).thenReturn(false);

        ServerOAuth2AuthorizationRequestResolver result = webSecurity.authorizationRequestResolver(Optional.empty());

        assertThat(result).isNull();
    }

    @Test
    void authorizationRequestResolver_whenClientConfigurationConfigured_shouldResolveRequests() {
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);

        InMemoryReactiveClientRegistrationRepository clientRegistrationRepository = mock(InMemoryReactiveClientRegistrationRepository.class);

        when(mockClientConfig.isConfigured()).thenReturn(true);

        ServerOAuth2AuthorizationRequestResolver result = webSecurity.authorizationRequestResolver(Optional.of(clientRegistrationRepository));

        assertThat(result).isInstanceOf(DefaultServerOAuth2AuthorizationRequestResolver.class);
    }

    @Test
    void authorizedClientService_whenClientConfigurationConfigured_shouldCreateClientService () {
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        ReactiveClientRegistrationRepository clientRegistrationRepository = mock(ReactiveClientRegistrationRepository.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);

        when(mockClientConfig.isConfigured()).thenReturn(true);

        ReactiveOAuth2AuthorizedClientService result = webSecurity.authorizedClientService(Optional.of(clientRegistrationRepository));

        assertThat(result).isInstanceOf(InMemoryReactiveOAuth2AuthorizedClientService.class);
    }

    @Test
    void authorizedClientService_whenClientConfigurationNotConfigured_shouldReturnNull () {
        ClientConfiguration mockClientConfig = mock(ClientConfiguration.class);
        WebSecurity webSecurity = new WebSecurity(mockClientConfig, tokenProvider, basicAuthProvider, applicationContext);

        when(mockClientConfig.isConfigured()).thenReturn(false);

        ReactiveOAuth2AuthorizedClientService result = webSecurity.authorizedClientService(Optional.empty());

        assertThat(result).isNull();
    }

    @Test
    void loadAuthorizationRequest_whenValidCookiesProvided_shouldLoadAuthorizationRequest () {
        var resolver = mock(ServerOAuth2AuthorizationRequestResolver.class);
        var oauth2AuthReqBuilder = OAuth2AuthorizationRequest.authorizationCode();
        oauth2AuthReqBuilder.authorizationUri("auth-uri").clientId("test-client");
        var oauth2AuthReq = oauth2AuthReqBuilder.build();
        when(resolver.resolve(any(), eq("test-client"))).thenReturn(Mono.just(oauth2AuthReq));

        var requestRepository = new WebSecurity(null, tokenProvider, basicAuthProvider, applicationContext)
            .new ApimlServerAuthorizationRequestRepository(resolver);

        var exchange = mock(ServerWebExchange.class);
        var request = mock(ServerHttpRequest.class);
        var requestPath = mock(RequestPath.class);

        when(requestPath.value()).thenReturn("/gateway/login/oauth2/code/test-client");
        when(request.getPath()).thenReturn(requestPath);

        var cookieMap = new LinkedMultiValueMap<String, HttpCookie>();
        cookieMap.add(WebSecurity.COOKIE_NONCE, new HttpCookie(WebSecurity.COOKIE_NONCE, "nonceValue"));
        cookieMap.add(WebSecurity.COOKIE_STATE, new HttpCookie(WebSecurity.COOKIE_STATE, "stateValue"));
        when(request.getCookies()).thenReturn(cookieMap);

        when(exchange.getRequest()).thenReturn(request);

        var resultMono = requestRepository.loadAuthorizationRequest(exchange);

        StepVerifier.create(resultMono)
            .assertNext(requests -> {
                assertThat(requests.getAttributes().get(OidcParameterNames.NONCE)).isEqualTo("nonceValue");
                assertThat(requests.getState()).isEqualTo("stateValue");
            })
            .verifyComplete();

    }

    @Test
    void saveAuthorizationRequest_whenCalled_shouldSaveCookies () {
        var resolver = mock(ServerOAuth2AuthorizationRequestResolver.class);
        var requestRepository = new WebSecurity(null, tokenProvider, basicAuthProvider, applicationContext).new ApimlServerAuthorizationRequestRepository(resolver);

        var request = mock(ServerHttpRequest.class);
        var headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());

        var exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(request);
        var response = mock(ServerHttpResponse.class);
        when(exchange.getResponse()).thenReturn(response);

        var cookies = new LinkedMultiValueMap<String, ResponseCookie>();
        doAnswer(invocation -> {
            var cookie = invocation.getArgument(0, ResponseCookie.class);
            cookies.add(cookie.getName(), cookie);
            return null;
        }).when(response).addCookie(any(ResponseCookie.class));

        var oauth2AuthReq = OAuth2AuthorizationRequest.authorizationCode()
            .clientId("test-client")
            .state("test-state")
            .authorizationUri("https://test.auth.server/oauth/authorize")
            .attributes(attrs -> attrs.put(OidcParameterNames.NONCE, "test-nonce"))
            .build();

        requestRepository.saveAuthorizationRequest(oauth2AuthReq, exchange).block();

        assertThat(cookies.getFirst(WebSecurity.COOKIE_NONCE).getValue()).isEqualTo("test-nonce");
        assertThat(cookies.getFirst(WebSecurity.COOKIE_STATE).getValue()).isEqualTo("test-state");

    }
}
