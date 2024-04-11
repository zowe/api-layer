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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.web.server.ServerHttpSecurity;
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
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Configuration
public class WebSecurity {

    public static final String COOKIE_NONCE = "oidc_nonce";
    public static final String COOKIE_STATE = "oidc_state";
    private static final Pattern CLIENT_REG_ID = Pattern.compile("^/login/oauth2/code/([^/]+)$");

    @Value("${apiml.security.x509.registry.allowedUsers:#{null}}")
    private String allowedUsers;

    private Predicate<String> usernameAuthorizationTester;
    public static final String DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";

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
    public SecurityWebFilterChain oauth2WebFilterChain(
        ServerHttpSecurity http,
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService,
        ApimlServerAuthorizationRequestRepository requestRepository,
        ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) {
        http
            .securityContextRepository(new ServerSecurityContextRepository() {

                @Override
                public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
                    return Mono.empty();
                }

                @Override
                public Mono<SecurityContext> load(ServerWebExchange exchange) {
                    return Mono.empty();
                }
            })
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("oauth2/authorization/**", "/login/oauth2/code/**"))
            .authorizeExchange(authorize -> authorize.anyExchange().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .authorizationRequestRepository(requestRepository)
                .authorizationRequestResolver(authorizationRequestResolver)
                .authenticationSuccessHandler((webFilterExchange, authentication) ->
                    reactiveOAuth2AuthorizedClientService.loadAuthorizedClient("okta", authentication.getName()).map(oAuth2AuthorizedClient -> {
                        webFilterExchange.getExchange().getResponse().addCookie(ResponseCookie.from("apimlAuthenticationToken", oAuth2AuthorizedClient.getAccessToken().getTokenValue()).build());
                        clearCookies(webFilterExchange);
                        return Mono.empty();
                    }).flatMap(x -> Mono.empty())
                )
                .authenticationFailureHandler((webFilterExchange, exception) -> {
                        clearCookies(webFilterExchange);
                        return Mono.empty();
                    }
                ))
            .oauth2Client(oAuth2ClientSpec -> oAuth2ClientSpec.authorizationRequestRepository(requestRepository));
        return http.build();
    }

    void clearCookies(WebFilterExchange webFilterExchange) {
        webFilterExchange.getExchange().getResponse().addCookie(ResponseCookie.from(COOKIE_NONCE).maxAge(0).path("/").httpOnly(true).secure(true).build());
        webFilterExchange.getExchange().getResponse().addCookie(ResponseCookie.from(COOKIE_STATE).maxAge(0).path("/").httpOnly(true).secure(true).build());
    }

    @Bean
    public ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(InMemoryReactiveClientRegistrationRepository inMemoryReactiveClientRegistrationRepository) {
        return new DefaultServerOAuth2AuthorizationRequestResolver(
            inMemoryReactiveClientRegistrationRepository);
    }

    @Bean
    public ApimlServerAuthorizationRequestRepository requestRepository(ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver) {
        return new ApimlServerAuthorizationRequestRepository(authorizationRequestResolver);
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

    @RequiredArgsConstructor
    static class ApimlServerAuthorizationRequestRepository implements ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {

        final ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver;

        @Override
        public Mono<OAuth2AuthorizationRequest> loadAuthorizationRequest(ServerWebExchange exchange) {
            var registrationId = getClientRegistrationId(exchange);
            return authorizationRequestResolver.resolve(exchange, registrationId).map(
                arr -> {
                    HttpCookie nonceCookie = exchange.getRequest().getCookies().getFirst(COOKIE_NONCE);
                    if (nonceCookie != null) {
                        return createAuthorizationRequest(exchange, arr);
                    }
                    return arr;
                }
            );
        }

        String getClientRegistrationId(ServerWebExchange exchange) {
            var path = exchange.getRequest().getPath().value();
            var matcher = CLIENT_REG_ID.matcher(path);
            if (matcher.matches()) {
                return matcher.group(1);
            }
            throw new IllegalStateException("Client registration ID was not found in the path: " + path);
        }

        public OAuth2AuthorizationRequest createAuthorizationRequest(ServerWebExchange exchange, OAuth2AuthorizationRequest original) {
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .attributes((attrs) ->
                    attrs.put(OAuth2ParameterNames.REGISTRATION_ID, original.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID)));
            applyNonce(builder, exchange.getRequest().getCookies().getFirst(COOKIE_NONCE).getValue());
            // @formatter:off
            builder.clientId(original.getClientId())
                .authorizationUri(original.getAuthorizationUri())
                .redirectUri(original.getRedirectUri())
                .scopes(original.getScopes())
                .state(exchange.getRequest().getCookies().getFirst(COOKIE_STATE).getValue());
            return builder.build();
        }
        private static void applyNonce(OAuth2AuthorizationRequest.Builder builder, String nonce) {
            String nonceHash = createHash(nonce);
            builder.attributes((attrs) -> attrs.put(OidcParameterNames.NONCE, nonce));
            builder.additionalParameters((params) -> params.put(OidcParameterNames.NONCE, nonceHash));
        }

        private static String createHash(String value) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        }

        @Override
        public Mono<Void> saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, ServerWebExchange exchange) {
            exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_NONCE, String.valueOf(authorizationRequest.getAttributes().get(OidcParameterNames.NONCE))).path("/").httpOnly(true).secure(true).build());
            exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_STATE, String.valueOf(authorizationRequest.getState())).path("/").httpOnly(true).secure(true).build());

            return Mono.empty();
        }

        @Override
        public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
            Mono<OAuth2AuthorizationRequest> requestMono = loadAuthorizationRequest(exchange);
            exchange.getResponse().getCookies().remove("nonce");

            return requestMono;
        }
    }
}
