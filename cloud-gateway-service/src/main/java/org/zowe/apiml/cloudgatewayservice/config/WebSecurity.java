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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
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

    public static final String AUTH_PREFIX = "/auth";

    public static final String COOKIE_NONCE = "oidc_nonce";
    public static final String COOKIE_STATE = "oidc_state";
    public static final String COOKIE_RETURN_URL = "oidc_return_url";
    private static final Pattern CLIENT_REG_ID = Pattern.compile("^" + AUTH_PREFIX + "/oauth2/login/code/([^/]+)$");
    private static final Predicate<HttpCookie> HAS_NO_VALUE = cookie -> cookie == null || StringUtils.isEmpty(cookie.getValue());
    private static final List<String> COOKIES = Arrays.asList(COOKIE_NONCE, COOKIE_STATE, COOKIE_RETURN_URL);

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
    public SecurityWebFilterChain oauth2WebFilterChain(
        ServerHttpSecurity http,
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService,
        ApimlServerAuthorizationRequestRepository requestRepository,
        ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) {
        return http
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
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(AUTH_PREFIX + "/oauth2/authorization/**", AUTH_PREFIX + "/oauth2/login/code/**"))
            .authorizeExchange(authorize -> authorize.anyExchange().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .authenticationMatcher(new PathPatternParserServerWebExchangeMatcher(AUTH_PREFIX + "/oauth2/login/callback/{registrationId}"))
                .authorizationRequestRepository(requestRepository)
                .authorizationRequestResolver(authorizationRequestResolver)
                .authenticationSuccessHandler((webFilterExchange, authentication) ->
                    reactiveOAuth2AuthorizedClientService.loadAuthorizedClient("okta", authentication.getName()).map(oAuth2AuthorizedClient -> {
                        webFilterExchange.getExchange().getResponse().addCookie(ResponseCookie.from("apimlAuthenticationToken", oAuth2AuthorizedClient.getAccessToken().getTokenValue()).build());
                        var location = webFilterExchange.getExchange().getRequest().getCookies().getFirst(COOKIE_RETURN_URL);
                        if (!HAS_NO_VALUE.test(location)) {
                            redirect(webFilterExchange.getExchange().getResponse(), location.getValue());
                        }

                        clearCookies(webFilterExchange);
                        return Mono.empty();
                    }).flatMap(x -> Mono.empty())
                )
                .authenticationFailureHandler((webFilterExchange, exception) -> {
                        var clientRegistrationId = getClientRegistrationId(webFilterExchange.getExchange());
                        clearCookies(webFilterExchange);
                        redirect(webFilterExchange.getExchange().getResponse(), AUTH_PREFIX + "/oauth2/authorization/" + clientRegistrationId);
                        return Mono.empty();
                    }
                ))
            .oauth2Client(oAuth2ClientSpec -> oAuth2ClientSpec.authorizationRequestRepository(requestRepository))
            .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint(AUTH_PREFIX + "/oauth2/login")))
            .build();
    }

    void redirect(ServerHttpResponse response, String location) {
        response.getHeaders().set(HttpHeaders.LOCATION, location);
        response.setStatusCode(HttpStatusCode.valueOf(302));
    }

    void clearCookies(WebFilterExchange webFilterExchange) {
        COOKIES.forEach(cookie -> webFilterExchange.getExchange().getResponse().addCookie(ResponseCookie.from(cookie).maxAge(0).path("/").httpOnly(true).secure(true).build()));
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

    static String getClientRegistrationId(ServerWebExchange exchange) {
        var path = exchange.getRequest().getPath().value();
        var matcher = CLIENT_REG_ID.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Client registration ID was not found in the path: " + path);
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

        public OAuth2AuthorizationRequest createAuthorizationRequest(ServerWebExchange exchange, OAuth2AuthorizationRequest original) {
            var nonceCookie = exchange.getRequest().getCookies().getFirst(COOKIE_NONCE);
            var stateCookie = exchange.getRequest().getCookies().getFirst(COOKIE_STATE);
            if (HAS_NO_VALUE.test(nonceCookie) && HAS_NO_VALUE.test(stateCookie)) {
                return original;
            }
            var nonce = nonceCookie.getValue();
            String nonceHash = createHash(nonce);

            return OAuth2AuthorizationRequest.authorizationCode()
                .attributes((attrs) -> {
                        attrs.put(OAuth2ParameterNames.REGISTRATION_ID, original.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID));
                        attrs.put(OidcParameterNames.NONCE, nonce);
                    }
                )
                .additionalParameters((params) -> params.put(OidcParameterNames.NONCE, nonceHash))
                .clientId(original.getClientId())
                .authorizationUri(original.getAuthorizationUri())
                .redirectUri(original.getRedirectUri())
                .scopes(original.getScopes())
                .state(stateCookie.getValue())
                .build();
        }

        private static String createHash(String value) {
            try {
                var md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Mono<Void> saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, ServerWebExchange exchange) {
            exchange.getResponse().addCookie(
                createCookie(COOKIE_NONCE, String.valueOf(authorizationRequest.getAttributes().get(OidcParameterNames.NONCE)))
            );
            exchange.getResponse().addCookie(createCookie(COOKIE_RETURN_URL, getReturnUrl(exchange)));
            exchange.getResponse().addCookie(createCookie(COOKIE_STATE, authorizationRequest.getState()));
            return Mono.empty();
        }

        String getReturnUrl(ServerWebExchange exchange) {
            return Optional.ofNullable(exchange.getRequest().getQueryParams().getFirst("returnUrl"))
                .orElse(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN));
        }


        ResponseCookie createCookie(String name, String value) {
            return ResponseCookie.from(name, value).path("/").httpOnly(true).secure(true).build();
        }


        @Override
        public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
            Mono<OAuth2AuthorizationRequest> requestMono = loadAuthorizationRequest(exchange);
            exchange.getResponse().getCookies().remove("nonce");
            return requestMono;
        }
    }
}
