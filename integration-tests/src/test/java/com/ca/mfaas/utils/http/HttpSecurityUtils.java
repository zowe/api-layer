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

import com.ca.mfaas.utils.config.ApiCatalogServiceConfiguration;
import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;

import java.io.IOException;
import java.net.URI;

public class HttpSecurityUtils {
    private static final String API_CATALOG_LOGIN_ENDPOINT = "/api/v1/apicatalog/auth/login";

    private HttpSecurityUtils() {}

    public static String getCookieForApiCatalog() throws IOException {
        ApiCatalogServiceConfiguration apiCatalogServiceConfiguration = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration();
        String user = apiCatalogServiceConfiguration.getUser();
        String password = apiCatalogServiceConfiguration.getPassword();
        URI uri = HttpRequestUtils.getUriFromGateway(API_CATALOG_LOGIN_ENDPOINT);

        return getCookie(uri, user, password);
    }

    public static String getCookie(URI loginUrl, String user, String password) throws IOException {
        HttpPost request = new HttpPost(loginUrl);
        HttpClient client = HttpClientUtils.client();
        String credentials = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", user, password);
        StringEntity payload = new StringEntity(credentials);
        request.setEntity(payload);
        request.setHeader("Content-type", "application/json");

        HttpResponse response = client.execute(request);
        return response.getFirstHeader("Set-Cookie").getValue();
    }

    public static HttpResponse getLoginResponse(String loginUrl, String user, String password) throws IOException {
        URI uri = HttpRequestUtils.getUriFromGateway(loginUrl);
        return getLoginResponse(uri, user, password);
    }

    public static HttpResponse getLoginResponse(URI loginUrl, String user, String password) throws IOException {
        HttpPost request = new HttpPost(loginUrl);
        HttpClient client = HttpClientUtils.client();
        String credentials = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", user, password);
        StringEntity payload = new StringEntity(credentials);
        request.setEntity(payload);
        request.setHeader("Content-type", "application/json");

        return client.execute(request);
    }

    public static String getCookie(String loginUrl, String user, String password) throws IOException {
        URI uri = HttpRequestUtils.getUriFromGateway(loginUrl);

        return getCookie(uri, user, password);
    }

    public static HttpRequest addBasicAuthorizationHeader(HttpRequest request) throws AuthenticationException {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String user = serviceConfiguration.getUser();
        String password = serviceConfiguration.getPassword();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
        request.addHeader(new BasicScheme().authenticate(credentials, request, null));
        return request;
    }

    public static HttpRequest addToken(HttpRequest request, String token) {
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    public static HttpRequest addCookie(HttpRequest request, String cookie) {
        request.addHeader("Cookie", cookie);
        return request;
    }
}
