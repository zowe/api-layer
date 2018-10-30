/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils.http;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Slf4j
public class HttpRequestUtils {

    private HttpRequestUtils() {}

    /**
     * Execute the endpoint and check the response for a return code
     * @param endpoint execute thus
     * @param returnCode check for this
     * @return response
     * @throws URISyntaxException oops
     * @throws IOException oops
     */
    public static HttpResponse getResponse(String endpoint, int returnCode) throws IOException {
        HttpGet request = getRequest(endpoint);

        // When
        HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }

    public static HttpResponse response(HttpUriRequest request, int returnCode) throws IOException {
        // When
        HttpResponse response = HttpClientUtils.client().execute(request);
        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));
        return response;
    }

    public static HttpGet getRequest(String endpoint) {
        URI uri = getUriFromGateway(endpoint);

        return new HttpGet(uri);
    }

    public static URI getUriFromGateway(String endpoint) {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String scheme = gatewayServiceConfiguration.getScheme();
        String host = gatewayServiceConfiguration.getHost();
        int port = gatewayServiceConfiguration.getPort();
        URI uri = null;
        try {
            uri = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(endpoint)
                .build();
        } catch (URISyntaxException e) {
            log.error("Can't create URI for endpoint '{}'", endpoint);
            e.printStackTrace();
        }

        return uri;
    }
}
