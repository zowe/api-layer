/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers;

import com.netflix.appinfo.InstanceInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.instance.InstanceRetrievalService;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.util.EurekaUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

/**
 * Controller for static api refresh, acting as Discovery service proxy for UI
 */
@RestController
public class ApiStaticRefreshController {

    private final InstanceRetrievalService instanceRetrievalService;
    private final RestTemplate restTemplate;

    private static final String REFRESH_ENDPOINT = "/discovery/api/v1/staticApi";

    // integration-tests/README.md ## Manual testing of Discovery Service in HTTP mode
    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    public ApiStaticRefreshController(@Qualifier("restTemplateWithKeystore") RestTemplate restTemplate, InstanceRetrievalService instanceRetrievalService) {
        this.instanceRetrievalService = instanceRetrievalService;
        this.restTemplate = restTemplate;
    }

    //TODO this url should change
    @PostMapping(
        value = "/discovery/api/v1/staticApi")
    public String refreshStaticApis(HttpServletResponse response) {

        InstanceInfo discoveryInstance = null;
        try {
            discoveryInstance = instanceRetrievalService.getInstanceInfo(CoreService.DISCOVERY.getServiceId());

            if (discoveryInstance != null) {
                String url = EurekaUtils.getUrl(discoveryInstance) + REFRESH_ENDPOINT;
                HttpEntity<?> entity = new HttpEntity<>(null, url.startsWith("http://") ? createBasicAuthHeader(eurekaUserid, eurekaPassword) : null);
                ResponseEntity<String> restResponse = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                response.setStatus(restResponse.getStatusCode().value());
                return restResponse.getBody();
            } else {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                return "Discovery service not available";
                //TODO logger + message
            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return e.getMessage();
            //TODO logger + message
        }
    }

    private MultiValueMap createBasicAuthHeader(String username, String password) {
        MultiValueMap headerMap = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes()).getBytes();
        String authHeader = "Basic " + new String(encodedAuth);
        headerMap.put("Authorization", authHeader);
        return headerMap;
    }

}
