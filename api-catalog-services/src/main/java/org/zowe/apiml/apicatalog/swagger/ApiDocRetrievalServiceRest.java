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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.UnaryOperator;

import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

/**
 * Retrieves the API documentation for a registered service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiDocRetrievalServiceRest implements InitializingBean {

    private static final UnaryOperator<String> exceptionMessage = serviceId -> "No API Documentation was retrieved for the service " + serviceId + ".";

    @Qualifier("webClientClientCert")
    private final WebClient webClientClientCert;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    private boolean allowAnySwaggerUrl = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        allowAnySwaggerUrl = Boolean.getBoolean("ZWE_APIML_ALLOW_ANY_SWAGGER_URL");
    }

    public Mono<ApiDocInfo> retrieveApiDoc(ServiceInstance serviceInstance, ApiInfo apiInfo) {
        var serviceId = StringUtils.lowerCase(serviceInstance.getServiceId());
        log.debug("Retrieving API doc for '{} {}'", serviceId, apiInfo.getVersion());

        var apiDocUrl = apiInfo.getSwaggerUrl();

        if (!allowAnySwaggerUrl && !verifySwaggerUrl(serviceInstance, apiDocUrl)) {
            log.debug("URL {} does not match declared host: {} and/or port: {} in instance {}", apiDocUrl, serviceInstance.getHost(), serviceInstance.getPort(), serviceInstance.getInstanceId());
            return Mono.error(new ApiDocNotFoundException("Swagger URL validation failed"));
        }

        return getApiDocContentByUrl(serviceId, apiDocUrl)
            .map(content -> ApiDocInfo.builder().apiInfo(apiInfo).apiDocContent(content).build());
    }

    private boolean verifySwaggerUrl(ServiceInstance serviceInstance, String apiDocUrl) {
        try {
            var url = new URL(apiDocUrl);
            return StringUtils.isNotBlank(url.getHost())
                && url.getPort() > 0
                && url.getHost().equalsIgnoreCase(serviceInstance.getHost())
                && url.getPort() == serviceInstance.getPort();
        } catch (MalformedURLException e) {
            return false;
        }
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
        return webClientClientCert.get()
            .uri(apiDocUrl)
            .header(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .onStatus(httpStatusCode -> httpStatusCode.value() != SC_OK, response -> Mono.error(
                new ApiDocNotFoundException(
                    String.format("No API Documentation was retrieved due to %s server error: %d", serviceId, response.statusCode().value())
                )
            ))
            .bodyToMono(String.class)
            .onErrorResume(IOException.class, e -> {
                apimlLogger.log("org.zowe.apiml.apicatalog.apiDocHostCommunication", serviceId, e.getMessage());
                log.debug("Error retrieving api doc for '{}'", serviceId, e);
                return Mono.error(new ApiDocNotFoundException(
                    exceptionMessage.apply(serviceId) + " Root cause: " + e.getMessage(), e
                ));
            });
    }

}
