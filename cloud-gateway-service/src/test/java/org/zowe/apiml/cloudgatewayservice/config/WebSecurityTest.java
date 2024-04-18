/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSecurityTest {

    private ReactiveUserDetailsService reactiveUserDetailsService;

    @Nested
    class WhenListOfAllowedUserDefined {
        WebSecurity webSecurity;

        @BeforeEach
        void setUp() {
            webSecurity = new WebSecurity();
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
            var location = "localhost:10023";
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
            WebSecurity webSecurity = new WebSecurity();
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
}
