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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.gateway.filters.RobinRoundIterator;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
public abstract class AbstractAuthProviderFilter<T> {

    protected static final RobinRoundIterator<ServiceInstance> robinRound = new RobinRoundIterator<>();

    protected final WebClient webClient;
    protected final InstanceInfoService instanceInfoService;

    protected abstract  Mono<T> processResponse(WebClient.RequestHeadersSpec<?> rhs);

    protected abstract String getEndpointPath();

    protected Mono<List<ServiceInstance>> getZaasInstances() {
        return instanceInfoService.getServiceInstance("zaas");
    }

    private Mono<T> requestWithHa(
        Iterator<ServiceInstance> serviceInstanceIterator,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        return processResponse(requestCreator.apply(serviceInstanceIterator.next()))
            .switchIfEmpty(serviceInstanceIterator.hasNext() ?
                requestWithHa(serviceInstanceIterator, requestCreator) : Mono.empty()
            );
    }

    public String getEndpointUrl(ServiceInstance instance) {
        return UriComponentsBuilder.newInstance()
            .scheme(instance.getScheme())
            .host(instance.getHost())
            .port(instance.getPort())
            .path(getEndpointPath())
            .toUriString();
    }

    protected Mono<T> invoke(
        List<ServiceInstance> serviceInstances,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        Iterator<ServiceInstance> i = robinRound.getIterator(serviceInstances);
        if (!i.hasNext()) {
            throw new ServiceNotAccessibleException("No ZAAS is available");
        }

        return requestWithHa(i, requestCreator);
    }

}
