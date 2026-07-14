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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.firewall.StrictServerWebExchangeFirewall;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.savedrequest.CookieServerRequestCache;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.gateway.config.oidc.ClientConfiguration;
import org.zowe.apiml.gateway.controllers.GatewayExceptionHandler;
import org.zowe.apiml.gateway.filters.proxyheaders.AdditionalRegistrationGatewayRegistry;
import org.zowe.apiml.gateway.filters.proxyheaders.X509AndGwAwareXForwardedHeadersFilter;
import org.zowe.apiml.gateway.filters.security.AuthExceptionHandlerReactive;
import org.zowe.apiml.gateway.filters.security.BasicAuthFilter;
import org.zowe.apiml.gateway.filters.security.TokenAuthFilter;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.CustomHstsServerHttpHeadersWriter;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;
import org.zowe.apiml.security.common.util.X509Util;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_FULL_URL;
import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_SHORT_URL;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;


@Configuration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(SafSecurityConfigurationProperties.class)
public class WebSecurity {

    public static final String CONTEXT_PATH = "/" + CoreService.GATEWAY.getServiceId();
    public static final String REGISTRY_PATH = CONTEXT_PATH + "/api/v1/registry";
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

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Value("${apiml.security.enableStrictUrlValidation:false}")
    private boolean isStrictUrlValidationEnabled;

    @Value("${apiml.service.externalUrl}")
    private String externalUrl;

    private final ClientConfiguration clientConfiguration;

    private final TokenProvider tokenProvider;
    private final BasicAuthProvider basicAuthProvider;

    private final ApplicationContext applicationContext;

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
    SecurityWebFilterChain oauth2WebFilterChain(
        ServerHttpSecurity http,
        Optional<ReactiveOAuth2AuthorizedClientService> reactiveOAuth2AuthorizedClientService,
        Optional<ApimlServerAuthorizationRequestRepository> requestRepository,
        Optional<ServerOAuth2AuthorizationRequestResolver> authorizationRequestResolver
    ) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return http
            .headers(headers -> headers
                .hsts(ServerHttpSecurity.HeaderSpec.HstsSpec::disable)
                .writer(new CustomHstsServerHttpHeadersWriter())
                .frameOptions(spec -> spec.mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN)))
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

    private String sanitizeRelativeUri(URI uri) {
        /*
         * Reject:
         *   //attacker.example
         *   attacker.example/path
         *   gateway:foo
         *
         * Only root-relative paths are allowed.
         */
        if (uri.getRawAuthority() != null
            || uri.getRawUserInfo() != null) {
            return CONTEXT_PATH;
        }

        String path = uri.getRawPath();

        if (StringUtils.isEmpty(path)
            || !path.startsWith("/")
            || path.startsWith("//")) {
            return CONTEXT_PATH;
        }

        return toRelativeLocation(uri);
    }

    private boolean isSameOrigin(URI candidate, URI configuredExternalUri) {
        if (candidate.getScheme() == null
            || candidate.getHost() == null
            || configuredExternalUri.getScheme() == null
            || configuredExternalUri.getHost() == null
            || candidate.getRawUserInfo() != null) {
            return false;
        }

        return candidate.getScheme().equalsIgnoreCase(configuredExternalUri.getScheme())
            && candidate.getHost().equalsIgnoreCase(configuredExternalUri.getHost())
            && effectivePort(candidate) == effectivePort(configuredExternalUri);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }

        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }

        return -1;
    }

    // defense-in-depth
    private String toRelativeLocation(URI uri) {
        String path = StringUtils.defaultIfEmpty(uri.getRawPath(), "/");

        StringBuilder location = new StringBuilder(path);

        if (uri.getRawQuery() != null) {
            location.append('?').append(uri.getRawQuery());
        }

        if (uri.getRawFragment() != null) {
            location.append('#').append(uri.getRawFragment());
        }

        return location.toString();
    }

    private String sanitizeReturnUrl(String candidate) {
        if (StringUtils.isBlank(candidate)) {
            return CONTEXT_PATH;
        }

        String lowercaseCandidate = candidate.toLowerCase(Locale.ROOT);

        // Backslashes can be interpreted as slashes by some clients.
        // Also reject encoded backslashes and CR/LF characters.
        if (candidate.contains("\\")
            || lowercaseCandidate.contains("%5c")
            || lowercaseCandidate.contains("%0d")
            || lowercaseCandidate.contains("%0a")) {
            return CONTEXT_PATH;
        }

        try {
            URI candidateUri = new URI(candidate);

            if (!candidateUri.isAbsolute()) {
                return sanitizeRelativeUri(candidateUri);
            }

            if (StringUtils.isBlank(externalUrl)) {
                return CONTEXT_PATH;
            }
            URI configuredExternalUri = new URI(externalUrl);

            if (!isSameOrigin(candidateUri, configuredExternalUri)) {
                return CONTEXT_PATH;
            }

            /*
             * Even for an accepted absolute same-origin URL, return only its
             * path/query/fragment. This guarantees that Location never contains
             * a user-controlled authority.
             */
            return toRelativeLocation(candidateUri);
        } catch (URISyntaxException | IllegalArgumentException e) {
            return CONTEXT_PATH;
        }
    }
    public Mono<Object> updateCookies(WebFilterExchange webFilterExchange, OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        ServerWebExchange exchange = webFilterExchange.getExchange();

        exchange.getResponse().addCookie(defaultCookieAttr(ResponseCookie.from(COOKIE_AUTH_NAME, oAuth2AuthorizedClient.getAccessToken().getTokenValue())).build());

        HttpCookie locationCookie = exchange.getRequest().getCookies().getFirst(COOKIE_RETURN_URL);

        String location = HAS_NO_VALUE.test(locationCookie)
            ? CONTEXT_PATH
            : sanitizeReturnUrl(locationCookie.getValue());

        redirect(exchange.getResponse(), location);
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
    ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
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
    ApimlServerAuthorizationRequestRepository requestRepository(Optional<ServerOAuth2AuthorizationRequestResolver> authorizationRequestResolver) {
        if (!clientConfiguration.isConfigured()) {
            return null;
        }
        return new ApimlServerAuthorizationRequestRepository(
            authorizationRequestResolver
                .orElseThrow(() -> new NoSuchBeanDefinitionException(ServerOAuth2AuthorizationRequestResolver.class))
        );
    }

    @Bean
    ReactiveClientRegistrationRepository clientRegistrationRepository() {
        if (!clientConfiguration.isConfigured()) {
            return registrationId -> null;
        }
        return new InMemoryReactiveClientRegistrationRepository(this.getClientRegistrations());
    }

    @Bean
    ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository(
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
    ReactiveOAuth2AuthorizedClientManager gatewayReactiveOAuth2AuthorizedClientManager(
        Optional<ReactiveClientRegistrationRepository> clientRegistrationRepository,
        Optional<ReactiveOAuth2AuthorizedClientService> authorizedClientService
    ) {
        if (!clientConfiguration.isConfigured()) {
            return registrationId -> null;
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
            ).toList();
    }

    public ServerHttpSecurity defaultSecurityConfig(ServerHttpSecurity http) {
        var gatewayExceptionHandler = applicationContext.getBean("gatewayExceptionHandler", GatewayExceptionHandler.class);

        return http
            .headers(headers -> headers
                .hsts(hsts -> hsts.disable())
                .writer(new CustomHstsServerHttpHeadersWriter())
                .frameOptions(spec -> spec.mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN)))
            .x509(x509 -> x509
                .principalExtractor(X509Util.x509PrincipalExtractor())
                .authenticationManager(X509Util.x509ReactiveAuthenticationManager())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec.authenticationEntryPoint(
                gatewayExceptionHandler::handleAuthenticationException)
            );
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityWebFilterChain defaultSecurityWebFilterChain(ServerHttpSecurity http) {
        return defaultSecurityConfig(http).build();
    }

    @Bean
    @Order(1)
    @ConditionalOnMissingBean(name = "modulithConfig")
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, AuthConfigurationProperties authConfigurationProperties, AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        return defaultSecurityConfig(http)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                REGISTRY_PATH,
                REGISTRY_PATH + "/**",
                SERVICES_SHORT_URL,
                SERVICES_SHORT_URL + "/**",
                SERVICES_FULL_URL,
                SERVICES_FULL_URL + "/**",
                "/application/**"
            ))
            .authorizeExchange(authorizeExchangeSpec -> {
                    if (!isHealthEndpointProtected) {
                        authorizeExchangeSpec
                            .pathMatchers("/application/info", "/application/version", "/application/health")
                            .permitAll();
                    } else {
                        authorizeExchangeSpec
                            .pathMatchers("/application/info", "/application/version")
                            .permitAll();
                    }
                }
            )
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .anyExchange().authenticated()
            )
            .addFilterAfter(new TokenAuthFilter(tokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicAuthFilter(basicAuthProvider), SecurityWebFiltersOrder.AUTHENTICATION)
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
            exchange.getResponse().addCookie(createCookie(COOKIE_RETURN_URL, getSafeReturnUrl(exchange)));
            exchange.getResponse().addCookie(createCookie(COOKIE_STATE, authorizationRequest.getState()));
            return Mono.empty();
        }

        String getReturnUrl(ServerWebExchange exchange) {
            return Optional.ofNullable(exchange.getRequest().getQueryParams().getFirst("returnUrl"))
                .orElse(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN));
        }

        // Only a same-origin relative path or an absolute URL matching apiml.service.externalUrl is trusted;
        // anything else falls back to the gateway's own root to prevent open-redirect/phishing via returnUrl.
        private String getSafeReturnUrl(ServerWebExchange exchange) {
            return sanitizeReturnUrl(getReturnUrl(exchange));
        }

        @Override
        public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
            Mono<OAuth2AuthorizationRequest> requestMono = loadAuthorizationRequest(exchange);
            exchange.getResponse().getCookies().remove(COOKIE_NONCE);
            return requestMono;
        }

    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebFilter writeableHeaders() {
        return (exchange, chain) -> {
            var writeableHeaders = new HttpHeaders(exchange.getRequest().getHeaders());
            ServerHttpRequestDecorator writeableRequest = new ServerHttpRequestDecorator(
                exchange.getRequest()) {
                @Override
                public HttpHeaders getHeaders() {
                    return writeableHeaders;
                }
            };
            ServerWebExchange writeableExchange = exchange.mutate()
                .request(writeableRequest)
                .build();
            return chain.filter(writeableExchange);
        };
    }

    @Bean
    StrictServerWebExchangeFirewall httpFirewall() {
        var strictFirewall = new StrictServerWebExchangeFirewall();
        if (isStrictUrlValidationEnabled) {
            return strictFirewall;
        }

        StrictServerWebExchangeFirewall firewall = new ApimlStrictServerWebExchangeFirewall(strictFirewall);
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedDoubleSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        firewall.setAllowSemicolon(true);
        return firewall;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cloud.gateway.x-forwarded.enabled", matchIfMissing = true)
    XForwardedHeadersFilter xForwardedHeadersFilter(
        @Value("${apiml.security.forwardHeader.trustedProxies:#{null}}") String trustedProxies,
        HttpsConfig httpsConfig,
        AdditionalRegistrationGatewayRegistry additionalRegistrationGatewayRegistry
    ) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        return new X509AndGwAwareXForwardedHeadersFilter(httpsConfig, trustedProxies, additionalRegistrationGatewayRegistry);
    }

    @RequiredArgsConstructor
    static class ApimlStrictServerWebExchangeFirewall extends StrictServerWebExchangeFirewall {

        private static final String[] BASE_PATH_MICROSERVICES = {
            "/gateway",
            "/application",
            "/images",
            "/v3/api-docs"
        };

        private static final String[] BASE_PATHS_MODULITH = ArrayUtils.addAll(BASE_PATH_MICROSERVICES, new String[]{
            "/apicatalog",
            "/cachingservice"
        });

        @Value("${server.port}")
        private int gatewayPort;

        @Autowired
        private ApplicationInfo applicationInfo;

        private final StrictServerWebExchangeFirewall nonRoutingFirewall;

        boolean isPathToRoute(ServerHttpRequest request, String[] prefixes) {
            var path = request.getPath().value();
            // homepage
            if (Strings.CS.equals(path, "/")) {
                return false;
            }
            for (String prefix : prefixes) {
                if (Strings.CS.equals(path, prefix)) {
                    return false;
                }
                if (Strings.CS.startsWith(path, prefix + "/")) {
                    return false;
                }
            }
            return true;
        }

        boolean isPathToRoute(ServerHttpRequest request) {
            if (applicationInfo.isModulith()) {
                // check if the request is to DS on the internal port
                if (request.getLocalAddress().getPort() != gatewayPort) {
                    return false;
                }

                return isPathToRoute(request, BASE_PATHS_MODULITH);
            }

            return isPathToRoute(request, BASE_PATH_MICROSERVICES);
        }


        @Override
        public Mono<ServerWebExchange> getFirewalledExchange(ServerWebExchange exchange) {
            // in case of Gateway and a request to routing use a configured values
            if (isPathToRoute(exchange.getRequest())) {
                return super.getFirewalledExchange(exchange);
            }

            return nonRoutingFirewall.getFirewalledExchange(exchange);
        }

    }

}
