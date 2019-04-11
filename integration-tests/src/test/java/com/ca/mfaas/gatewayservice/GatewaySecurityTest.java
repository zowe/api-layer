/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import com.ca.mfaas.utils.http.HttpRequestUtils;
import com.ca.mfaas.utils.http.HttpSecurityUtils;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sun.jndi.toolkit.url.Uri;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class GatewaySecurityTest {
    private static final String PROTECTED_ENDPOINT = "/application/";
    private static final String LOGIN_ENDPOINT = "/api/v1/gateway/auth/login";
    private static final String LOGOUT_ENDPOINT = "/api/v1/gateway/auth/logout";
    private static final String AUTHENTICATION_COOKIE = "apimlAuthenticationToken";

    @Test
    public void accessProtectedEndpointWithoutCredentials() throws IOException {
        HttpRequestUtils.getResponse(PROTECTED_ENDPOINT, SC_UNAUTHORIZED);
    }

    @Test
    public void loginToGatewayAndAccessProtectedEndpoint() throws IOException {
        String cookie = HttpSecurityUtils.getCookieForGateway();
        HttpGet request = HttpRequestUtils.getRequest(PROTECTED_ENDPOINT);
        request = (HttpGet) HttpSecurityUtils.addCookie(request, cookie);
        HttpRequestUtils.response(request, SC_OK);
    }

    @Test
    public void loginToGatewayAndCheckForCookie() throws IOException {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String user = gatewayServiceConfiguration.getUser();
        String password = gatewayServiceConfiguration.getPassword();
        HttpResponse response = HttpSecurityUtils.getLoginResponse(LOGIN_ENDPOINT, user, password);
        String cookiesString = response.getFirstHeader("Set-Cookie").getValue();
        List<HttpCookie> cookies = HttpCookie.parse(cookiesString);
        HttpCookie cookie = cookies.stream().filter(c -> c.getName().equals(AUTHENTICATION_COOKIE)).findAny().orElse(null);

        assertTrue(cookie.isHttpOnly());
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(86400L));
    }

    @Test
    public void loginWithInvalidUser() throws IOException {
        String user = "invalid";
        String password = "invalid";
        HttpResponse response = HttpSecurityUtils.getLoginResponse(LOGIN_ENDPOINT, user, password);

        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        String content = jsonContext.read("$.messages[0].messageNumber");

        assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
        assertThat(content, equalTo("SEC0005"));
    }

    @Test
    @Ignore //'expire' user does not have access - zosmf token expires itself
    public void accessProtectedEndpointWithExpiredToken() throws IOException, InterruptedException {
        String user = "expire";
        String password = "expire";
        URI uri = HttpRequestUtils.getUriFromGateway(LOGIN_ENDPOINT);
        String cookie = HttpSecurityUtils.getCookie(uri, user, password);
        TimeUnit.SECONDS.sleep(3);
        HttpGet request = HttpRequestUtils.getRequest(PROTECTED_ENDPOINT);
        request = (HttpGet) HttpSecurityUtils.addCookie(request, cookie);
        HttpResponse response = HttpRequestUtils.response(request, SC_UNAUTHORIZED);

        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        String content = jsonContext.read("$.messages[0].messageNumber");

        assertThat(content, equalTo("SEC0004"));
    }

    @Test
    public void accessProtectedEndpointWithInvalidToken() throws IOException {
        String cookie = HttpSecurityUtils.getCookieForGateway();
        String subst = cookie.substring(30, 50);
        cookie = cookie.replace(subst, "not");
        HttpGet request = HttpRequestUtils.getRequest(PROTECTED_ENDPOINT);
        request = (HttpGet) HttpSecurityUtils.addCookie(request, cookie);
        HttpResponse response = HttpRequestUtils.response(request, SC_UNAUTHORIZED);

        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        String content = jsonContext.read("$.messages[0].messageNumber");

        assertThat(content, equalTo("SEC0003"));
    }

    @Test
    public void logout() throws IOException {
        String cookie = HttpSecurityUtils.getCookieForGateway();
        HttpGet request = HttpRequestUtils.getRequest(LOGOUT_ENDPOINT);
        String requestCookie = String.format("%s=%s", AUTHENTICATION_COOKIE, cookie);
        request.setHeader("Cookie", requestCookie);
        HttpResponse response = HttpRequestUtils.response(request, SC_OK);
        String responseCookiesString = response.getFirstHeader("Set-Cookie").getValue();
        List<HttpCookie> cookies = HttpCookie.parse(responseCookiesString);
        HttpCookie responseCookie = cookies.stream().filter(c -> c.getName().equals(AUTHENTICATION_COOKIE)).findAny().orElse(null);

        assertThat(responseCookie.getValue(), is(""));
        assertThat(responseCookie.hasExpired(), is(true));
    }
}
