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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.zowe.apiml.gateway.filters.RobinRoundIterator;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@Component
public class TokenProvider {

    private final WebClient webClient;
    private final InstanceInfoService instanceInfoService;

    public TokenProvider(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService) {
        this.webClient = webClient;
        this.instanceInfoService = instanceInfoService;
    }

    private static final RobinRoundIterator<ServiceInstance> robinRound = new RobinRoundIterator<>();

    public Mono<QueryResponse> validateToken(String token) {
        return getZaasInstances().flatMap(instances ->
            invoke(
                instances,
                instance -> createRequest(instance, token)
            )
        );

    }

    protected Mono<QueryResponse> invoke(
        List<ServiceInstance> serviceInstances,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        Iterator<ServiceInstance> i = robinRound.getIterator(serviceInstances);
        if (!i.hasNext()) {
            throw new IllegalArgumentException("No ZAAS is available");
        }

        return requestWithHa(i, requestCreator);
    }

    private Mono<QueryResponse> requestWithHa(
        Iterator<ServiceInstance> serviceInstanceIterator,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        return requestCreator.apply(serviceInstanceIterator.next())
            .retrieve()
            .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.empty())
            .bodyToMono(getResponseClass())
            .onErrorResume(exception -> exception instanceof WebClientResponseException.Unauthorized ? Mono.just(new QueryResponse()) : Mono.error(exception))
            .switchIfEmpty(serviceInstanceIterator.hasNext() ?
                requestWithHa(serviceInstanceIterator, requestCreator) : Mono.empty()
            );
    }

    protected Class<QueryResponse> getResponseClass() {
        return QueryResponse.class;
    }

    public String getEndpointUrl(ServiceInstance instance) {
        return String.format("%s://%s:%d/%s/api/v1/auth/query", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String token) {
        String tokensUrl = getEndpointUrl(instance);
        return webClient.get().uri(tokensUrl).headers(httpHeaders -> httpHeaders.set(HttpHeaders.COOKIE, "apimlAuthenticationToken=" + token));
    }

    private Mono<List<ServiceInstance>> getZaasInstances() {
        return instanceInfoService.getServiceInstance("zaas");
    }

    public Authentication getAuthentication(String user, String token) {
        var auth = new TokenAuthentication(user, token);
        auth.setAuthenticated(true);
        return auth;
    }
}
