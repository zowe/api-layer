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

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Configuration
public class WebSecurity {

    @Value("${apiml.security.x509.registry.allowedUsers:#{null}}")
    private String allowedUsers;

    private Predicate<String> usernameAuthorizationTester;

    @PostConstruct
    void initScopes() {
        boolean authorizeAnyUsers = "*".equals(allowedUsers);

        Set<String> users = Optional.ofNullable(allowedUsers)
            .map(line -> line.split("[,;]"))
            .map(Arrays::asList)
            .orElse(Collections.emptyList())
            .stream().map(String::trim)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        usernameAuthorizationTester = user -> authorizeAnyUsers || users.contains(StringUtils.lowerCase(user));
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain oauth2WebFilterChain(ServerHttpSecurity http, InMemoryReactiveClientRegistrationRepository inMemoryReactiveClientRegistrationRepository, ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService, ApimlServerAuthorizationRequestRepository requestRepository) {
        http
            .securityContextRepository(new ServerSecurityContextRepository() {
                AtomicReference<SecurityContext> securityContextAtomicReference = new AtomicReference<>();

                @Override
                public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
                    System.out.println("prve" + exchange.getRequest().getCookies().get("nonce"));
                    securityContextAtomicReference.set(context);
                    return Mono.empty();
                }

                @Override
                public Mono<SecurityContext> load(ServerWebExchange exchange) {

                    return Mono.justOrEmpty(securityContextAtomicReference.get());
                }
            })
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("oauth2/authorization/**", "/login/oauth2/code/**"))
            .authorizeExchange(authorize -> authorize.anyExchange().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .authorizationRequestRepository(requestRepository)
                .authorizationRequestResolver(this.authorizationRequestResolver(inMemoryReactiveClientRegistrationRepository))
                .authenticationSuccessHandler(new ServerAuthenticationSuccessHandler() {
                    @Override
                    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
                        return reactiveOAuth2AuthorizedClientService.loadAuthorizedClient("okta", authentication.getName()).map(oAuth2AuthorizedClient -> {
                            webFilterExchange.getExchange().getResponse().addCookie(ResponseCookie.from("apimlAuthenticationToken", oAuth2AuthorizedClient.getAccessToken().getTokenValue()).build());
                            System.out.println(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
                            return Mono.empty();
                        }).flatMap(x -> Mono.empty());
                    }
                }))
            .oauth2Client(oAuth2ClientSpec -> oAuth2ClientSpec.authorizationRequestRepository(requestRepository));
        return http.build();
    }

    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(InMemoryReactiveClientRegistrationRepository inMemoryReactiveClientRegistrationRepository) {
        ServerWebExchangeMatcher authorizationRequestMatcher =
            new PathPatternParserServerWebExchangeMatcher(
                "/login/oauth2/authorization/{registrationId}");

        return new DefaultServerOAuth2AuthorizationRequestResolver(
            inMemoryReactiveClientRegistrationRepository, authorizationRequestMatcher);
    }

    @Bean
    public ApimlServerAuthorizationRequestRepository requestRepository() {
        return new ApimlServerAuthorizationRequestRepository();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
            .x509(x509 ->
                x509
                    .principalExtractor(new SubjectDnX509PrincipalExtractor())
                    .authenticationManager(authentication -> {
                        authentication.setAuthenticated(true);
                        return Mono.just(authentication);
                    })
            )
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .pathMatchers("/" + CoreService.CLOUD_GATEWAY.getServiceId() + "/api/v1/registry/**").authenticated()
                    .anyExchange().permitAll()
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    @Bean
    @Primary
    ReactiveUserDetailsService userDetailsService() {

        return username -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (usernameAuthorizationTester.test(username)) {
                authorities.add(new SimpleGrantedAuthority("REGISTRY"));
            }
            UserDetails userDetails = User.withUsername(username).authorities(authorities).password("").build();
            return Mono.just(userDetails);
        };
    }

    static class ApimlServerAuthorizationRequestRepository implements ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        AtomicReference<OAuth2AuthorizationRequest> reference = new AtomicReference<>();

        @Override
        public Mono<OAuth2AuthorizationRequest> loadAuthorizationRequest(ServerWebExchange exchange) {
            System.out.println("druhe" + exchange.getRequest().getCookies().get("nonce"));
            return Mono.empty();
        }

        @Override
        public Mono<Void> saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, ServerWebExchange exchange) {
            exchange.getResponse().addCookie(ResponseCookie.from("nonce", String.valueOf(authorizationRequest.getAdditionalParameters().get("nonce"))).build());
            reference.set(authorizationRequest);
            return Mono.empty();
        }

        @Override
        public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
            exchange.getResponse().getCookies().remove("nonce");
            return Mono.justOrEmpty(reference.getAndSet(null));
        }
    }
}
