/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.gateway.filters.RobinRoundIterator;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class BasicAuthProvider extends TokenProvider {

    private final WebClient webClient;
    private final InstanceInfoService instanceInfoService;
    private final AuthConfigurationProperties authConfigurationProperties;

    public BasicAuthProvider(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, AuthConfigurationProperties authConfigurationProperties) {
        super(webClient, instanceInfoService);
        this.webClient = webClient;
        this.instanceInfoService = instanceInfoService;
        this.authConfigurationProperties = authConfigurationProperties;
    }

    private static final RobinRoundIterator<ServiceInstance> robinRound = new RobinRoundIterator<>();

    public Mono<ClientResponse.Headers> authenticateUser(String authHeader) {
        return getZaasInstances().flatMap(instances ->
            invokeS(
                instances,
                instance -> createRequest(instance, authHeader)
            )
        );

    }

    protected Mono<ClientResponse.Headers> invokeS(
        List<ServiceInstance> serviceInstances,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        Iterator<ServiceInstance> i = robinRound.getIterator(serviceInstances);
        if (!i.hasNext()) {
            throw new IllegalArgumentException("No ZAAS is available");
        }

        return requestWithHa(i, requestCreator);
    }

    private Mono<ClientResponse.Headers> requestWithHa(
        Iterator<ServiceInstance> serviceInstanceIterator,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        return requestCreator.apply(serviceInstanceIterator.next())
            .exchangeToMono(clientResp -> switch (clientResp.statusCode().value()) {
                case SC_UNAUTHORIZED: case SC_NO_CONTENT:
                    yield Mono.just(clientResp.headers());
                default:
                    yield Mono.empty();
            })
            .switchIfEmpty(serviceInstanceIterator.hasNext() ?
                requestWithHa(serviceInstanceIterator, requestCreator) : Mono.empty()
            );
    }

    public String getEndpointUrl(ServiceInstance instance) {
        return String.format("%s://%s:%d/%s/api/v1/auth/login", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String headerValue) {
        var loginUrl = getEndpointUrl(instance);
        return webClient.post().uri(loginUrl).headers(httpHeaders -> httpHeaders.set(HttpHeaders.AUTHORIZATION, headerValue));
    }

    private Mono<List<ServiceInstance>> getZaasInstances() {
        return instanceInfoService.getServiceInstance("zaas");
    }

    public Mono<String> getToken(String authHeader) {
        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        return authenticateUser(authHeader)
            .map(headers -> headers.header(HttpHeaders.SET_COOKIE).stream()
                .map(HttpCookie::parse)
                .flatMap(Collection::stream)
                .filter(cookie -> StringUtils.equals(cookieName, cookie.getName()))
                .findFirst()
                .map(HttpCookie::getValue).orElse("")
            );
    }

    public Authentication getAuthentication(String token, String authHeader) {
        var loginRequest = LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(authHeader));
        var auth = new TokenAuthentication(loginRequest.get().getUsername(), token);
        auth.setAuthenticated(true);
        return auth;
    }

}
