/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.apache.hc.core5.http.HttpStatus.*;
import static org.zowe.apiml.gateway.x509.ForwardClientCertFilterFactory.CLIENT_CERT_HEADER;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "modulithConfig")
public class ZaasSchemeTransformRest implements ZaasSchemeTransform {

    private static final String HEADER_SERVICE_ID = "X-Service-Id";
    private static final String SERVICE_IS_UNAVAILABLE_MESSAGE = "There are no instance of ZAAS available";

    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    private final RobinRoundIterator<ServiceInstance> robinRound = new RobinRoundIterator<>();

    private final ReactiveDiscoveryClient discoveryClient;
    @Qualifier("webClientClientCert")
    private final WebClient webClient;

    private <R> Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>> requestWithHa(
        Class<R> responseClass,
        Iterator<ServiceInstance> serviceInstanceIterator,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator,
        AtomicReference<Optional<Exception>> mostCriticalException // to be accessible and updatable in all lambdas below
    ) {
        // selected instance of ZAAS to invoke
        var zaasInstance = serviceInstanceIterator.next();

        // this lambda creates a chain of call over all instances. It also remembers the most critical exception to
        // be thrown in case all instances fail
        Function<Exception, Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>>> callNext = exception -> {
            // select the most critical exception to remember (ZaasInternalErrorException is more important one)
            exception = mostCriticalException.get().filter(ZaasInternalErrorException.class::isInstance).orElse(exception);
            mostCriticalException.set(Optional.of(exception));

            if (serviceInstanceIterator.hasNext()) {
                return requestWithHa(responseClass, serviceInstanceIterator, requestCreator, mostCriticalException);
            } else {
                return Mono.error(exception);
            }
        };

        return requestCreator.apply(zaasInstance)
            .exchangeToMono(clientResp -> switch (clientResp.statusCode().value()) {
                case SC_UNAUTHORIZED -> Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(clientResp.headers(), null));
                case SC_OK -> clientResp.bodyToMono(responseClass).map(zaasResponseBody -> new AbstractAuthSchemeFactory.AuthorizationResponse<>(clientResp.headers(), zaasResponseBody));
                case SC_INTERNAL_SERVER_ERROR -> callNext.apply(new ZaasInternalErrorException(zaasInstance, "An internal exception occurred in ZAAS service. Check its configuration of instance " + zaasInstance.getInstanceId() + "."));
                default -> callNext.apply(new ServiceNotAccessibleException(SERVICE_IS_UNAVAILABLE_MESSAGE));
            })
            .doOnError(t -> log.debug("Error on calling ZAAS service instance {}: {}", zaasInstance.getInstanceId(), t.getMessage()))
            .onErrorResume(e -> callNext.apply(new ServiceNotAccessibleException(SERVICE_IS_UNAVAILABLE_MESSAGE)));
    }

    private <R> Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>> call(Class<R> responseClass, Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestBuilder) {
        return discoveryClient.getInstances(CoreService.ZAAS.getServiceId())
            .collectList()
            .flatMap(zaasInstances -> {
                Iterator<ServiceInstance> i = robinRound.getIterator(zaasInstances);
                if (!i.hasNext()) {
                    return Mono.error(new ServiceNotAccessibleException(SERVICE_IS_UNAVAILABLE_MESSAGE));
                }

                return requestWithHa(responseClass, i, requestBuilder,  new AtomicReference<>(Optional.empty()))
                    .switchIfEmpty(Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,null)));
            });
    }

    private WebClient.RequestHeadersSpec<?> createRequest(RequestCredentials requestCredentials, String uri, String jsonBody) {
        var requestBuild = webClient.post().uri(uri);

        Optional.ofNullable(requestCredentials.getHeaders()).orElse(Collections.emptyMap())
            .forEach(requestBuild::header);
        Optional.ofNullable(requestCredentials.getCookies()).orElse(Collections.emptyMap())
            .forEach(requestBuild::cookie);

        if (StringUtils.isNotEmpty(requestCredentials.getX509Certificate())) {
            requestBuild.header(CLIENT_CERT_HEADER, requestCredentials.getX509Certificate());
        }
        if (StringUtils.isNotEmpty(requestCredentials.getServiceId())) {
            requestBuild.header(HEADER_SERVICE_ID, requestCredentials.getServiceId());
        }

        if (StringUtils.isNotEmpty(requestCredentials.getApplId())) {
            requestBuild.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return requestBuild.bodyValue(jsonBody);
        }

        return requestBuild;
    }

    private String getUrl(String pattern, ServiceInstance instance) {
        return String.format(pattern, instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<TicketResponse>> passticket(RequestCredentials requestCredentials) {
        try {
            var jsonBody = WRITER.writeValueAsString(new TicketRequest(requestCredentials.getApplId()));
            return call(
                TicketResponse.class,
                instance -> createRequest(
                    requestCredentials,
                    getUrl("%s://%s:%d/%s/scheme/ticket", instance),
                    jsonBody
                )
            );
        } catch (JsonProcessingException jpe) {
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(
                new ErrorHeaders("Invalid client certificate in request. Error message: " + jpe.getMessage()),null)
            );
        }
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> safIdt(RequestCredentials requestCredentials) {
        try {
            String jsonBody = WRITER.writeValueAsString(new TicketRequest(requestCredentials.getApplId()));
            return call(
                ZaasTokenResponse.class,
                instance -> createRequest(
                    requestCredentials,
                    getUrl("%s://%s:%d/%s/scheme/safIdt", instance),
                    jsonBody
                )
            );

        } catch (JsonProcessingException jpe) {
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(
                new ErrorHeaders("Invalid client certificate in request. Error message: " + jpe.getMessage()),null)
            );
        }
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zosmf(RequestCredentials requestCredentials) {
        return call(
            ZaasTokenResponse.class,
            instance -> createRequest(
                requestCredentials,
                getUrl("%s://%s:%d/%s/scheme/zosmf", instance),
                null
            )
        );
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zoweJwt(RequestCredentials requestCredentials) {
        return call(
            ZaasTokenResponse.class,
            instance -> createRequest(
                requestCredentials,
                getUrl("%s://%s:%d/%s/scheme/zoweJwt", instance),
                null
            )
        );
    }

}
