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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaticAPIService {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    @Value("${apiml.discovery.userid:#{null}}")
    private String discoveryUserid;

    @Value("${apiml.discovery.password:#{null}}")
    private String discoveryPassword;

    @Qualifier("secureHttpClientWithKeystore")
    private final CloseableHttpClient httpClient;

    @Value("${server.attlsClient.enabled:false}")
    private boolean isClientAttlsEnabled;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.service.discoveryServiceUrls}")
    private String[] discoveryUrls;

    public StaticAPIResponse refresh() {
        List<String> discoveryServiceUrls = getDiscoveryServiceUrls();
        for (int i = 0; i < discoveryServiceUrls.size(); i++) {

            String discoveryServiceUrl = discoveryServiceUrls.get(i);

            HttpPost post = getHttpRequest(discoveryServiceUrl);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                final HttpEntity responseEntity = response.getEntity();
                String responseBody = "";
                if (responseEntity != null) {
                    responseBody = new BufferedReader(new InputStreamReader(responseEntity.getContent())).lines().collect(Collectors.joining("\n"));
                }
                // Return response if successful response or if none have been successful and this is the last URL to try
                if (isSuccessful(response) || i == discoveryServiceUrls.size() - 1) {
                    return new StaticAPIResponse(response.getStatusLine().getStatusCode(), responseBody);
                }

            } catch (Exception e) {
                log.debug("Error refreshing static APIs from {}, error message: {}", discoveryServiceUrl, e.getMessage());
            }
        }

        return new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service");
    }

    private boolean isSuccessful(CloseableHttpResponse response) {
        return response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299;
    }

    void setAuthorization(AbstractHttpMessage request) {
        if (StringUtils.isEmpty(discoveryUserid) || StringUtils.isEmpty(discoveryPassword)) {
            log.warn("Eureka userid or password not set");
        } else {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((discoveryUserid + ":" + discoveryPassword).getBytes());
            request.addHeader(HttpHeaders.AUTHORIZATION, basicToken);
        }
    }

    private HttpPost getHttpRequest(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpPost post = new HttpPost(discoveryServiceUrl);
        post.addHeader("Accept", "application/json");
        boolean clientCertificateUnavailable = isHttp || !verifySslCertificatesOfServices;
        if (clientCertificateUnavailable && !isClientAttlsEnabled) {
            setAuthorization(post);
        }
        return post;
    }

    private List<String> getDiscoveryServiceUrls() {

        List<String> discoveryServiceUrls = new ArrayList<>();
        for (String location : discoveryUrls) {
            discoveryServiceUrls.add(location.replace("/eureka", "") + REFRESH_ENDPOINT);
        }
        return discoveryServiceUrls;
    }

}
