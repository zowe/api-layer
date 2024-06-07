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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.CookieServerRequestCache;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.config.oidc.ClientConfiguration;
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

import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_URL;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;


@Configuration
@RequiredArgsConstructor
public class WebSecurity {

    public static final String CONTEXT_PATH = "/" + CoreService.GATEWAY.getServiceId();
    public static final String REGISTRY_PATH = CONTEXT_PATH + "/api/v1/registry/**";

    public static final String COOKIE_NONCE = "oidc_nonce";
    public static final String COOKIE_STATE = "oidc_state";
    public static final String COOKIE_RETURN_URL = "oidc_return_url";
    private static final Pattern CLIENT_REG_ID = Pattern.compile("^" + CONTEXT_PATH + "/login/oauth2/code/([^/]+)$");
    private static final Predicate<HttpCookie> HAS_NO_VALUE = cookie -> cookie == null || StringUtils.isEmpty(cookie.getValue());
    private static final List<String> COOKIES = Arrays.asList(COOKIE_NONCE, COOKIE_STATE, COOKIE_RETURN_URL);

    public static final String OAUTH_2_AUTHORIZATION = CONTEXT_PATH + "/oauth2/authorization/**";
    public static final String OAUTH_2_AUTHORIZATION_BASE_URI = CONTEXT_PATH + "/oauth2/authorization/";
    public static final String OAUTH_2_AUTHORIZATION_URI = CONTEXT_PATH + "/oauth2/authorization/{registrationId}";
    public static final String OAUTH_2_REDIRECT_URI = CONTEXT_PATH + "/login/oauth2/code/**";
    public static final String OAUTH_2_REDIRECT_LOGIN_URI = CONTEXT_PATH + "/login/oauth2/code/{registrationId}";

    @Value("${apiml.security.oidc.cookie.sameSite:Lax}")
    public String sameSite;

    @Value("${apiml.security.x509.registry.allowedUsers:#{null}}")
    private String allowedUsers;

    private final ClientConfiguration clientConfiguration;

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

    private ResponseCookie.ResponseCookieBuilder defaultCookieAttr(ResponseCookie.ResponseCookieBuilder builder) {
        return builder.path("/").sameSite(sameSite).httpOnly(true).secure(true);
    }

    private ResponseCookie createCookie(String name, String value) {
        return defaultCookieAttr(ResponseCookie.from(name, value)).build();
    }

    /**
     * Security chain for oauth2 client. To enable this chain, please refer to Zowe OIDC configuration.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain oauth2WebFilterChain(
        ServerHttpSecurity http,
        Optional<ReactiveOAuth2AuthorizedClientService> reactiveOAuth2AuthorizedClientService,
        Optional<ApimlServerAuthorizationRequestRepository> requestRepository,
        Optional<ServerOAuth2AuthorizationRequestResolver> authorizationRequestResolver
    ) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return http
            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(OAUTH_2_AUTHORIZATION, OAUTH_2_REDIRECT_URI))
            .authorizeExchange(authorize -> authorize.anyExchange().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .authenticationMatcher(new PathPatternParserServerWebExchangeMatcher(OAUTH_2_REDIRECT_LOGIN_URI))
                .authorizationRequestRepository(
                    requestRepository.orElseThrow(() -> new NoSuchBeanDefinitionException(ApimlServerAuthorizationRequestRepository.class))
                )
                .authorizationRequestResolver(
                    authorizationRequestResolver.orElseThrow(() -> new NoSuchBeanDefinitionException(ServerOAuth2AuthorizationRequestResolver.class))
                )
                .authenticationSuccessHandler((webFilterExchange, authentication) ->
                    reactiveOAuth2AuthorizedClientService
                        .orElseThrow(() -> new NoSuchBeanDefinitionException(ReactiveOAuth2AuthorizedClientService.class))
                        .loadAuthorizedClient(getClientRegistrationId(webFilterExchange.getExchange()), authentication.getName())
                        .map(oAuth2AuthorizedClient -> updateCookies(webFilterExchange, oAuth2AuthorizedClient)
                        ).flatMap(x -> Mono.empty())
                )
                .authenticationFailureHandler((webFilterExchange, exception) -> {
                        var clientRegistrationId = getClientRegistrationId(webFilterExchange.getExchange());
                        clearCookies(webFilterExchange);
                        redirect(webFilterExchange.getExchange().getResponse(), OAUTH_2_AUTHORIZATION_BASE_URI + clientRegistrationId);
                        return Mono.empty();
                    }
                ))
            .oauth2Client(oAuth2ClientSpec -> oAuth2ClientSpec.authorizationRequestRepository(
                requestRepository.orElseThrow(() -> new NoSuchBeanDefinitionException(ApimlServerAuthorizationRequestRepository.class))
            ))
            .requestCache(requestCacheSpec -> requestCacheSpec.requestCache(new CookieServerRequestCache()))
            .build();
    }

    public Mono<Object> updateCookies(WebFilterExchange webFilterExchange, OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        webFilterExchange.getExchange().getResponse().addCookie(defaultCookieAttr(ResponseCookie.from(COOKIE_AUTH_NAME, oAuth2AuthorizedClient.getAccessToken().getTokenValue())).build());
        var location = webFilterExchange.getExchange().getRequest().getCookies().getFirst(COOKIE_RETURN_URL);
        if (!HAS_NO_VALUE.test(location)) {
            redirect(webFilterExchange.getExchange().getResponse(), location.getValue());
        }
        clearCookies(webFilterExchange);
        return Mono.empty();
    }

    private void redirect(ServerHttpResponse response, String location) {
        response.getHeaders().set(HttpHeaders.LOCATION, location);
        response.setStatusCode(HttpStatusCode.valueOf(302));
    }

    private void clearCookies(WebFilterExchange webFilterExchange) {
        COOKIES.forEach(cookie -> webFilterExchange.getExchange().getResponse().addCookie(defaultCookieAttr(ResponseCookie.from(cookie).maxAge(0)).build()));
    }

    @Bean
    ReactiveOAuth2AuthorizedClientService authorizedClientService(
        Optional<ReactiveClientRegistrationRepository> clientRegistrationRepository) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return new InMemoryReactiveOAuth2AuthorizedClientService(
            clientRegistrationRepository
                .orElseThrow(() -> new NoSuchBeanDefinitionException(ReactiveClientRegistrationRepository.class))
        );
    }

    @Bean
    public ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
        Optional<InMemoryReactiveClientRegistrationRepository> inMemoryReactiveClientRegistrationRepository
    ) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return new DefaultServerOAuth2AuthorizationRequestResolver(
            inMemoryReactiveClientRegistrationRepository
                .orElseThrow(() -> new NoSuchBeanDefinitionException(InMemoryReactiveClientRegistrationRepository.class)),
            new PathPatternParserServerWebExchangeMatcher(OAUTH_2_AUTHORIZATION_URI)
        );
    }

    @Bean
    public ApimlServerAuthorizationRequestRepository requestRepository(Optional<ServerOAuth2AuthorizationRequestResolver> authorizationRequestResolver) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return new ApimlServerAuthorizationRequestRepository(
            authorizationRequestResolver
                .orElseThrow(() -> new NoSuchBeanDefinitionException(ServerOAuth2AuthorizationRequestResolver.class))
        );
    }

    @Bean
    public ReactiveClientRegistrationRepository clientRegistrationRepository() {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return new InMemoryReactiveClientRegistrationRepository(this.getClientRegistrations());
    }

    @Bean
    public ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository(
        Optional<ReactiveOAuth2AuthorizedClientService> clientService
    ) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(
            clientService.orElseThrow(() -> new NoSuchBeanDefinitionException(ReactiveOAuth2AuthorizedClientService.class))
        );
    }

    @Bean
    @ConditionalOnBean(ReactiveClientRegistrationRepository.class)
    public ReactiveOAuth2AuthorizedClientManager gatewayReactiveOAuth2AuthorizedClientManager(
        Optional<ReactiveClientRegistrationRepository> clientRegistrationRepository,
        Optional<ReactiveOAuth2AuthorizedClientService> authorizedClientService
    ) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }

        var authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
            .builder().authorizationCode().refreshToken().build();
        var authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository.orElseThrow(() -> new NoSuchBeanDefinitionException(ReactiveClientRegistrationRepository.class)),
            authorizedClientService.orElseThrow(() -> new NoSuchBeanDefinitionException(ReactiveOAuth2AuthorizedClientService.class))
        );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    private List<ClientRegistration> getClientRegistrations() {
        return clientConfiguration.getConfigurations().values().stream()
            .map(c -> ClientRegistration.withRegistrationId(c.getId())
                .clientId(c.getRegistration().getClientId())
                .clientSecret(c.getRegistration().getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(c.getRegistration().getRedirectUri())
                .scope(c.getRegistration().getScope())
                .authorizationUri(c.getProvider().getAuthorizationUri())
                .tokenUri(c.getProvider().getTokenUri())
                .userInfoUri(c.getProvider().getUserInfoUri())
                .userNameAttributeName(c.getProvider().getUserNameAttribute())
                .jwkSetUri(c.getProvider().getJwkSetUri())
                .clientName(c.getId())
                .build()
            ).collect(Collectors.toList());
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
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
                    .pathMatchers(REGISTRY_PATH, SERVICES_URL + "/**").authenticated()
                    .anyExchange().permitAll()
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
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
    class ApimlServerAuthorizationRequestRepository implements ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {

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
            if (HAS_NO_VALUE.test(nonceCookie) || HAS_NO_VALUE.test(stateCookie)) {
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

        @Override
        public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
            Mono<OAuth2AuthorizationRequest> requestMono = loadAuthorizationRequest(exchange);
            exchange.getResponse().getCookies().remove(COOKIE_NONCE);
            return requestMono;
        }

    }

}
