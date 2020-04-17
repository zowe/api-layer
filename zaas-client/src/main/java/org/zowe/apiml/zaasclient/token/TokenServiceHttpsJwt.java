/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.zaasclient.client.HttpsClient;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class TokenServiceHttpsJwt implements TokenService {
    private static final String COOKIE_PREFIX = "apimlAuthenticationToken";
    private final String loginEndpoint;
    private final String queryEndpoint;
    private final String host;

    private HttpsClient httpsClient;

    public TokenServiceHttpsJwt(HttpsClient client, String baseUrl, String host) {
        this.httpsClient = client;
        this.host = host;

        loginEndpoint = baseUrl + "/login";
        queryEndpoint = baseUrl + "/query";
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithCredentials(userId, password),
            this::extractToken);
    }

    private CloseableHttpResponse loginWithCredentials(String userId, String password) throws Exception {
        CloseableHttpClient client = httpsClient.getHttpsClientWithTrustStore();
        HttpPost httpPost = new HttpPost(loginEndpoint);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new Credentials(userId, password));
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);

        httpPost.setHeader("Content-type", "application/json");
        return client.execute(httpPost);
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithHeader(authorizationHeader),
            this::extractToken);
    }

    private CloseableHttpResponse loginWithHeader(String authorizationHeader) throws Exception {
        CloseableHttpClient client = httpsClient.getHttpsClientWithTrustStore();
        HttpPost httpPost = new HttpPost(loginEndpoint);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return client.execute(httpPost);
    }

    @Override
    public ZaasToken query(String jwtToken) throws ZaasClientException {
        return (ZaasToken) doRequest(
            () -> queryWithJwtToken(jwtToken),
            this::extractZaasToken);
    }

    private CloseableHttpResponse queryWithJwtToken(String jwtToken) throws Exception {
        BasicCookieStore cookieStore = prepareCookieWithToken(jwtToken);
        CloseableHttpClient client = httpsClient.getHttpsClientWithTrustStore(cookieStore);
        HttpGet httpGet = new HttpGet(queryEndpoint);
        return client.execute(httpGet);
    }

    private BasicCookieStore prepareCookieWithToken(String jwtToken) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie(COOKIE_PREFIX, jwtToken);
        cookie.setDomain(host);
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
        return cookieStore;
    }

    private void finallyClose(CloseableHttpResponse response) {
        try {
            if (response != null)
                response.close();
            if (httpsClient != null)
                httpsClient.close();
        } catch (IOException e) {
            log.warn("It wasn't possible to close the resources. " + e.getMessage());
        }
    }

    private ZaasToken extractZaasToken(CloseableHttpResponse response) throws IOException, ZaasClientException {
        if (response.getStatusLine().getStatusCode() == 200) {
            return new ObjectMapper().readValue(response.getEntity().getContent(), ZaasToken.class);
        } else {
            log.error(EntityUtils.toString(response.getEntity()));
            throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION);
        }
    }

    private String extractToken(CloseableHttpResponse response) throws ZaasClientException, IOException {
        String token = "";
        int httpResponseCode = response.getStatusLine().getStatusCode();
        if (httpResponseCode == 204) {
            HeaderElement[] elements = response.getHeaders("Set-Cookie")[0].getElements();
            Optional<HeaderElement> apimlAuthCookie = Stream.of(elements)
                .filter(element -> element.getName().equals(COOKIE_PREFIX))
                .findFirst();
            if (apimlAuthCookie.isPresent()) {
                token = apimlAuthCookie.get().getValue();
            }
        } else if (httpResponseCode == 401) {
            log.error(EntityUtils.toString(response.getEntity()));
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION);
        } else if (httpResponseCode == 400) {
            log.error(EntityUtils.toString(response.getEntity()));
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        } else {
            log.error(EntityUtils.toString(response.getEntity()));
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        }
        return token;
    }

    private Object doRequest(Operation request, Token token) throws ZaasClientException {
        CloseableHttpResponse response = null;

        try {
            response = request.request();

            return token.extract(response);
        } catch (ZaasClientException e) {
            throw e;
        } catch (IOException e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        } finally {
            finallyClose(response);
        }
    }

    @Data
    @AllArgsConstructor
    static class Credentials {
        String username;
        String password;
    }

    interface Token {
        Object extract(CloseableHttpResponse response) throws IOException, ZaasClientException;
    }

    interface Operation {
        CloseableHttpResponse request() throws Exception;
    }
}
