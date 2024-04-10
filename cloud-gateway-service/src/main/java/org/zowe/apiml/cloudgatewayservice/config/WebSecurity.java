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

import io.netty.handler.codec.http.cookie.CookieHeaderNames;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final char PATH_DELIMITER = '/';
    private static final StringKeyGenerator DEFAULT_SECURE_KEY_GENERATOR = new Base64StringKeyGenerator(
        Base64.getUrlEncoder().withoutPadding(), 96);
    private static final StringKeyGenerator DEFAULT_STATE_GENERATOR = new Base64StringKeyGenerator(
        Base64.getUrlEncoder());

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
        InMemoryReactiveClientRegistrationRepository inMemoryReactiveClientRegistrationRepository,
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService,
        ApimlServerAuthorizationRequestRepository requestRepository,
        ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) {
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
                .authorizationRequestResolver(authorizationRequestResolver)
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

    @Bean
    public ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(InMemoryReactiveClientRegistrationRepository inMemoryReactiveClientRegistrationRepository) {
        ServerWebExchangeMatcher authorizationRequestMatcher =
             ServerWebExchangeMatchers.pathMatchers(
                "/login/oauth2/codesdfs/{registrationId}");

        return new DefaultServerOAuth2AuthorizationRequestResolver(
            inMemoryReactiveClientRegistrationRepository, authorizationRequestMatcher);
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
            return authorizationRequestResolver.resolve(exchange,"okta").map(
                arr -> {
                    HttpCookie nonceCookie = exchange.getRequest().getCookies().getFirst("nonce");
                    if (nonceCookie != null) {
                        arr.getAdditionalParameters().put("nonce", createHash(nonceCookie.getValue()));
                        arr.getAttributes().put("nonce", nonceCookie.getValue());
                    }
                    return arr;
                }
            );
        }

//        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("okta")
//            .userNameAttributeName(IdTokenClaimNames.SUB)
//            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//            .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
//            .authorizationUri("https://dev-95727686.okta.com/oauth2/v1/authorize")
////                .providerConfigurationMetadata(configurationMetadata)
//            .tokenUri("https://dev-95727686.okta.com/oauth2/v1/token")
//            .issuerUri("https://dev-95727686.okta.com/oauth2/default")
//            .clientName("https://dev-95727686.okta.com/oauth2/default").build();
//        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
//            .attributes((attrs) ->
//                attrs.put(OAuth2ParameterNames.REGISTRATION_ID, clientRegistration.getRegistrationId()));
//        String redirectUriStr = expandRedirectUri(exchange.getRequest(), clientRegistration);
//        applyNonce(builder, exchange.getRequest().getCookies().get("nonce").get(0).getValue());
//        // @formatter:off
//            builder.clientId(clientRegistration.getClientId())
//                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
//            .redirectUri(redirectUriStr)
//                .scopes(clientRegistration.getScopes())
//            .state(DEFAULT_STATE_GENERATOR.generateKey());

        private static String expandRedirectUri(ServerHttpRequest request, ClientRegistration clientRegistration) {
            Map<String, String> uriVariables = new HashMap<>();
            uriVariables.put("registrationId", clientRegistration.getRegistrationId());
            // @formatter:off
            UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI())
                .replacePath(request.getPath().contextPath().value())
                .replaceQuery(null)
                .fragment(null)
                .build();
            // @formatter:on
            String scheme = uriComponents.getScheme();
            uriVariables.put("baseScheme", (scheme != null) ? scheme : "");
            String host = uriComponents.getHost();
            uriVariables.put("baseHost", (host != null) ? host : "");
            // following logic is based on HierarchicalUriComponents#toUriString()
            int port = uriComponents.getPort();
            uriVariables.put("basePort", (port == -1) ? "" : ":" + port);
            String path = uriComponents.getPath();
            if (org.springframework.util.StringUtils.hasLength(path)) {
                if (path.charAt(0) != PATH_DELIMITER) {
                    path = PATH_DELIMITER + path;
                }
            }
            uriVariables.put("basePath", (path != null) ? path : "");
            uriVariables.put("baseUrl", uriComponents.toUriString());
            String action = "";
            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())) {
                action = "login";
            }
            uriVariables.put("action", action);
            // @formatter:off
            return UriComponentsBuilder.fromUriString(clientRegistration.getRedirectUri())
                .buildAndExpand(uriVariables)
                .toUriString();
            // @formatter:on
        }

        private static void applyNonce(OAuth2AuthorizationRequest.Builder builder, String nonce) {

            String nonceHash = createHash(nonce);
            builder.attributes((attrs) -> attrs.put(OidcParameterNames.NONCE, nonce));
            builder.additionalParameters((params) -> params.put(OidcParameterNames.NONCE, nonceHash));


        }

        private static String createHash(String value) {
            MessageDigest md = null;
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
            exchange.getResponse().addCookie(ResponseCookie.from("nonce", String.valueOf(authorizationRequest.getAttributes().get("nonce"))).sameSite("Lax").path("/").httpOnly(true).secure(true).build());

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
