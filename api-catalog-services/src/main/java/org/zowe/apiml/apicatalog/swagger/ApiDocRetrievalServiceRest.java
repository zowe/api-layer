/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

/**
 * Retrieves the API documentation for a registered service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiDocRetrievalServiceRest {

    private static final UnaryOperator<String> exceptionMessage = serviceId -> "No API Documentation was retrieved for the service " + serviceId + ".";

    @Qualifier("secureHttpClientWithoutKeystore")
    private final CloseableHttpClient secureHttpClientWithoutKeystore;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    public Mono<ApiDocInfo> retrieveApiDoc(ServiceInstance serviceInstance, ApiInfo apiInfo) {
        String serviceId = StringUtils.lowerCase(serviceInstance.getServiceId());
        log.debug("Retrieving API doc for '{} {}'", serviceId, apiInfo.getVersion());

        String apiDocUrl = apiInfo.getSwaggerUrl();

        return getApiDocContentByUrl(serviceId, apiDocUrl)
            .map(content -> ApiDocInfo.builder().apiInfo(apiInfo).apiDocContent(content).build());
    }

    /**
     * Get ApiDoc content by Url
     *
     * @param serviceId the unique service id
     * @param apiDocUrl the url of apidoc
     * @return the information about ApiDoc content as application/json
     * @throws ApiDocNotFoundException if the response is error
     */
    private Mono<String> getApiDocContentByUrl(@NonNull String serviceId, String apiDocUrl) {
        HttpGet httpGet = new HttpGet(apiDocUrl);
        httpGet.setHeader(org.apache.http.HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // TODO: refactor with reactive client
        return Mono.defer(() -> {
            try {
                return Mono.just(secureHttpClientWithoutKeystore.execute(httpGet, response -> {
                        String responseBody = "";
                        var responseEntity = response.getEntity();
                        if (responseEntity != null) {
                            responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                        }

                        if (HttpStatus.SC_OK == response.getCode()) {
                            return responseBody;
                        } else {
                            throw new ApiDocNotFoundException(
                                String.format("No API Documentation was retrieved due to %s server error: %d %s", serviceId, response.getCode(), responseBody)
                            );
                        }
                    }
                ));
            } catch (IOException e) {
                apimlLogger.log("org.zowe.apiml.apicatalog.apiDocHostCommunication", serviceId, e.getMessage());
                log.debug("Error retrieving api doc for '{}'", serviceId, e);
                return Mono.error(new ApiDocNotFoundException(
                    exceptionMessage.apply(serviceId) + " Root cause: " + e.getMessage(), e
                ));
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

}
