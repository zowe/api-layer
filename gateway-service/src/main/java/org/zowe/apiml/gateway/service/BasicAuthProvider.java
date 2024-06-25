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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.util.Collection;

import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class BasicAuthProvider extends AbstractAuthProviderFilter<ClientResponse.Headers> {

    private final AuthConfigurationProperties authConfigurationProperties;

    public BasicAuthProvider(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, AuthConfigurationProperties authConfigurationProperties) {
        super(webClient, instanceInfoService);
        this.authConfigurationProperties = authConfigurationProperties;
    }

    public String getEndpointPath() {
        return "/zaas/api/v1/auth/login";
    }

    @Override
    protected Mono<ClientResponse.Headers> processResponse(WebClient.RequestHeadersSpec<?> rhs) {
        return rhs
            .exchangeToMono(clientResp -> switch (clientResp.statusCode().value()) {
                case SC_UNAUTHORIZED: case SC_NO_CONTENT:
                    yield Mono.just(clientResp.headers());
                default:
                    yield Mono.empty();
            });
    }

    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String headerValue) {
        return super.createRequest(instance)
            .headers(httpHeaders -> httpHeaders.set(HttpHeaders.AUTHORIZATION, headerValue));
    }

    public Mono<String> getToken(String authHeader) {
        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        return getZaasInstances().flatMap(instances ->
                invoke(
                    instances,
                    instance -> createRequest(instance, authHeader)
                )
            )
            .map(headers -> headers.header(HttpHeaders.SET_COOKIE).stream()
                .map(HttpCookie::parse)
                .flatMap(Collection::stream)
                .filter(cookie -> StringUtils.equals(cookieName, cookie.getName()))
                .findFirst()
                .map(HttpCookie::getValue).orElse("")
            );
    }

}
