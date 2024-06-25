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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.zowe.apiml.security.common.token.QueryResponse;
import reactor.core.publisher.Mono;

@Component
public class TokenProvider extends AbstractAuthProviderFilter<QueryResponse> {

    public TokenProvider(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService) {
        super(webClient, instanceInfoService);
    }

    public String getEndpointPath() {
        return "/zaas/api/v1/auth/query";
    }

    @Override
    protected Mono<QueryResponse> processResponse(WebClient.RequestHeadersSpec<?> rhs) {
        return rhs
            .retrieve()
            .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.empty())
            .bodyToMono(QueryResponse.class)
            .onErrorResume(exception -> exception instanceof WebClientResponseException.Unauthorized ? Mono.just(new QueryResponse()) : Mono.error(exception));
    }

    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String token) {
        return super.createRequest(instance)
            .headers(httpHeaders -> httpHeaders.set(HttpHeaders.COOKIE, "apimlAuthenticationToken=" + token));
    }

    public Mono<QueryResponse> validateToken(String token) {
        return getZaasInstances().flatMap(instances ->
            invoke(
                instances,
                instance -> createRequest(instance, token)
            )
        );
    }

}
