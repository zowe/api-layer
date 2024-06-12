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
import org.zowe.apiml.util.config.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
        String defaultScheme = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();

        HttpGet request = new HttpGet(
            getUri(defaultScheme, host, port, endpoint)
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

    private static URI getUri(String scheme, String host, int port, String endpoint, NameValuePair...arguments) {
        URI uri = null;
        try {
            uri = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(endpoint)
                .addParameters(Arrays.asList(arguments))
                .build();
        } catch (URISyntaxException e) {
            log.error("Can't create URI for endpoint '{}'", endpoint);
            e.printStackTrace();
        }

        return uri;
    }

    public static URI getUriFromService(ServiceConfiguration serviceConfiguration, String endpoint, NameValuePair...arguments) {
        String scheme = serviceConfiguration.getScheme();
        String host = serviceConfiguration.getHost();
        int port = serviceConfiguration.getPort();

        StringTokenizer hostnameTokenizer = new StringTokenizer(host, ",");
        host = hostnameTokenizer.nextToken();

        return getUri(scheme, host, port, endpoint, arguments);
    }

    public static URI getUriFromGateway(String endpoint, NameValuePair...arguments) {
        return getUriFromService(ConfigReader.environmentConfiguration().getGatewayServiceConfiguration(), endpoint, arguments);
    }

    public static URI getUriFromZaas(String endpoint, NameValuePair...arguments) {
        return getUriFromService(ConfigReader.environmentConfiguration().getZaasConfiguration(), endpoint, arguments);
    }

}
