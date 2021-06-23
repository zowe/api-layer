/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class HttpRequestUtils {

    private HttpRequestUtils() {}

    /**
     * Execute the GET request to the endpoint and check the response for a return code
     * @param endpoint execute thus
     * @param returnCode check for this
     * @return response
     * @throws IOException oops
     */
    public static HttpResponse getResponse(String endpoint, int returnCode) throws IOException {
        int port = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
        String host = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();

        return getResponse(endpoint, returnCode, port, host);
    }

    public static HttpResponse getResponse(String endpoint, int returnCode, int port, String host) throws IOException {
        HttpGet request = new HttpGet(
            getUriFromGateway(endpoint, port, host, Collections.emptyList())
        );

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

    public static URI getUriFromGateway(String endpoint, int port, String host, List<NameValuePair> arguments) {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String scheme = gatewayServiceConfiguration.getScheme();
        URI uri = null;
        try {
            uri = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(endpoint)
                .addParameters(arguments)
                .build();
        } catch (URISyntaxException e) {
            log.error("Can't create URI for endpoint '{}'", endpoint);
            e.printStackTrace();
        }

        return uri;
    }

    public static URI getUriFromGateway(String endpoint, List<NameValuePair> arguments) {
        return getUriFromGateway(endpoint, ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort(), ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost(), arguments);
    }

    public static URI getUriFromGateway(String endpoint) {
        return getUriFromGateway(endpoint, ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort(), ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost(), Collections.emptyList());
    }

    public static URI getUriFromDiscovery(String endpoint, String host) throws URISyntaxException {
        DiscoveryServiceConfiguration discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();

        return new URIBuilder()
            .setScheme(discoveryServiceConfiguration.getScheme())
            .setHost(host)
            .setPort(discoveryServiceConfiguration.getPort())
            .setPath(endpoint)
            .build();
    }
}
