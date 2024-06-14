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
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.config.oidc.ClientConfiguration;
import org.zowe.apiml.gateway.service.TokenProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSecurityTest {

    private ReactiveUserDetailsService reactiveUserDetailsService;
    @Autowired
    private TokenProvider tokenProvider;

    @Nested
    class WhenListOfAllowedUserDefined {
        WebSecurity webSecurity;

        @BeforeEach
        void setUp() {
            webSecurity = new WebSecurity(new ClientConfiguration(), tokenProvider);
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
                        .collect(Collectors.toList()))
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
                        .collect(Collectors.toList()))
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
            var webSecurity = new WebSecurity(new ClientConfiguration(), tokenProvider);
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
                        .collect(Collectors.toList()))
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
        var requestRepository = new WebSecurity(null, tokenProvider).new ApimlServerAuthorizationRequestRepository(resolver);
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
}
