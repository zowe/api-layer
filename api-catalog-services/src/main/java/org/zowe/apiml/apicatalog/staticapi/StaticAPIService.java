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

import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.instance.InstanceRetrievalService;
import org.zowe.apiml.apicatalog.services.status.model.ServiceNotFoundException;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.util.EurekaUtils;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class StaticAPIService {

    private static final String REFRESH_ENDPOINT = "/discovery/api/v1/staticApi";

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Qualifier("restTemplateWithKeystore")
    private final RestTemplate restTemplate;

    private final InstanceRetrievalService instanceRetrievalService;

    public StaticAPIResponse refresh() {
        String discoveryServiceUrl = getDiscoveryServiceUrl();
        HttpEntity<?> entity = getHttpEntity(discoveryServiceUrl);
        ResponseEntity<String> restResponse = restTemplate.exchange(discoveryServiceUrl,
            HttpMethod.POST, entity, String.class);
        return new StaticAPIResponse(restResponse.getStatusCode().value(), restResponse.getBody());
    }

    private HttpEntity<?> getHttpEntity(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpHeaders httpHeaders = new HttpHeaders();
        if (isHttp) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((eurekaUserid + ":" + eurekaPassword).getBytes());
            httpHeaders.add("Authorization", basicToken);
        }

        return new HttpEntity<>(null, httpHeaders);
    }

    private String getDiscoveryServiceUrl() {
        try {
            InstanceInfo discoveryInstance = instanceRetrievalService.getInstanceInfo(CoreService.DISCOVERY.getServiceId());
            if (discoveryInstance == null) {
                throw new ServiceNotFoundException("Discovery service could not be found");
            }

            return EurekaUtils.getUrl(discoveryInstance) + REFRESH_ENDPOINT;
        }  catch (InstanceInitializationException ie) {
            throw new ServiceNotFoundException("Discovery service instance could not be initialized");
        }
    }
}
