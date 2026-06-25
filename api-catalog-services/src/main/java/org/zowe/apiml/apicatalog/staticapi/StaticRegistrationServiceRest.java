/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.staticapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnMissingBean(name = "modulithConfig")
public class StaticRegistrationServiceRest implements StaticRegistrationService {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    @Value("${apiml.discovery.userid:#{null}}")
    private String discoveryUserid;

    @Value("${apiml.discovery.password:#{null}}")
    private String discoveryPassword;

    @Qualifier("webClientClientCert")
    private final WebClient webClientClientCert;

    @Value("${server.attlsClient.enabled:false}")
    private boolean isClientAttlsEnabled;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.service.discoveryServiceUrls}")
    private String[] discoveryUrls;


    void setAuthorization(HttpHeaders headers) {
        if (StringUtils.isEmpty(discoveryUserid) || StringUtils.isEmpty(discoveryPassword)) {
            log.warn("Eureka userid or password not set");
        } else {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((discoveryUserid + ":" + discoveryPassword).getBytes());
            headers.add(HttpHeaders.AUTHORIZATION, basicToken);
        }
    }

    @Override
    public Mono<StaticAPIResponse> refresh() {
        return Flux.fromIterable(getDiscoveryServiceUrls())
            .flatMap(uri -> webClientClientCert
                .post().uri(uri)
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .headers(headers -> {
                    boolean isHttp = uri.startsWith("http://");
                    boolean clientCertificateUnavailable = isHttp || !verifySslCertificatesOfServices;
                    if (clientCertificateUnavailable && !isClientAttlsEnabled) {
                        setAuthorization(headers);
                    }
                })
                .exchangeToMono(response -> response
                    .bodyToMono(String.class)
                    .flatMap(body -> (response.statusCode().is2xxSuccessful() || StringUtils.isNotBlank(body)) ?
                        Mono.just(new StaticAPIResponse(response.statusCode().value(), body)) : Mono.empty()
                    )
                )
                .doOnError(IOException.class, e -> log.debug("Error refreshing static APIs from {}, error message: {}", uri, e.getMessage()))
            )
            .switchIfEmpty(Flux.just(new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service")))
            .next();
    }

    private List<String> getDiscoveryServiceUrls() {
        List<String> discoveryServiceUrls = new ArrayList<>();
        for (String location : discoveryUrls) {
            location = location.replace("/eureka", "");
            location = location.endsWith("/") ? location : location + "/";
            discoveryServiceUrls.add(location + REFRESH_ENDPOINT);
        }
        return discoveryServiceUrls;
    }

}
