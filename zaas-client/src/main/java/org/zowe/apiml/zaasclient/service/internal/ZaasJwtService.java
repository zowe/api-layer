/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.service.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.http.cookie.SM;

@Slf4j
class ZaasJwtService implements TokenService {
    private static final String TOKEN_PREFIX = "apimlAuthenticationToken";

    private final String loginEndpoint;
    private final String queryEndpoint;
    private final String logoutEndpoint;
    private final CloseableClientProvider httpClientProvider;

    public ZaasJwtService(CloseableClientProvider client, String baseUrl) {
        this.httpClientProvider = client;

        loginEndpoint = baseUrl + "/login";
        queryEndpoint = baseUrl + "/query";
        logoutEndpoint = baseUrl + "/logout";
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithCredentials(userId, password),
            this::extractToken);
    }

    private ClientWithResponse loginWithCredentials(String userId, String password) throws ZaasConfigurationException, IOException  {
        CloseableHttpClient client = httpClientProvider.getHttpClient();
        HttpPost httpPost = new HttpPost(loginEndpoint);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new Credentials(userId, password));
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        return new ClientWithResponse(client, client.execute(httpPost));
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithHeader(authorizationHeader),
            this::extractToken);
    }

    private ClientWithResponse loginWithHeader(String authorizationHeader) throws ZaasConfigurationException, IOException {
        CloseableHttpClient client = httpClientProvider.getHttpClient();
        HttpPost httpPost = new HttpPost(loginEndpoint);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return new ClientWithResponse(client, client.execute(httpPost));
    }

    @Override
    public ZaasToken query(String jwtToken) throws ZaasClientException {
        return (ZaasToken) doRequest(
            () -> queryWithJwtToken(jwtToken),
            this::extractZaasToken);
    }

    @Override
    public void logout(String jwtToken) throws ZaasClientException {
        doRequest(() -> {
                try {
                    return logoutJwtToken(jwtToken);
                } catch (ZaasClientException e) {
                    throw e;
                }
            }
        );
    }

    private ClientWithResponse queryWithJwtToken(String jwtToken) throws ZaasConfigurationException, IOException {
        CloseableHttpClient client = httpClientProvider.getHttpClient();
        HttpGet httpGet = new HttpGet(queryEndpoint);
        httpGet.addHeader(SM.COOKIE, TOKEN_PREFIX + "=" + jwtToken);
        return new ClientWithResponse(client, client.execute(httpGet));
    }

    private ClientWithResponse logoutJwtToken(String jwtToken) throws ZaasConfigurationException, IOException, ZaasClientException {
        CloseableHttpClient client;
        client = httpClientProvider.getHttpClient();
        HttpPost httpPost = new HttpPost(logoutEndpoint);
        httpPost.addHeader("Cookie", TOKEN_PREFIX + "=" + jwtToken);
        return getClientWithResponse(client, httpPost);
    }

    private ClientWithResponse getClientWithResponse(CloseableHttpClient client, HttpPost httpPost) throws IOException, ZaasClientException {
        ClientWithResponse clientWithResponse = new ClientWithResponse(client, client.execute(httpPost));
        int httpResponseCode = clientWithResponse.getResponse().getStatusLine().getStatusCode();
        if (httpResponseCode == 204) {
            return clientWithResponse;
        }
        else {
            String obtainedMessage = EntityUtils.toString(clientWithResponse.getResponse().getEntity());
            if (httpResponseCode == 401) {
                throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION, obtainedMessage);
            } else if (httpResponseCode == 400) {
                throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD, obtainedMessage);
            } else {
                throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, obtainedMessage);
            }
        }
    }

    private void finallyClose(CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            log.warn("It wasn't possible to close the resources. " + e.getMessage());
        }
    }

    private ZaasToken extractZaasToken(CloseableHttpResponse response) throws IOException, ZaasClientException {
        if (response.getStatusLine().getStatusCode() == 200) {
            return new ObjectMapper().readValue(response.getEntity().getContent(), ZaasToken.class);
        } else {
            throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, EntityUtils.toString(response.getEntity()));
        }
    }

    private String extractToken(CloseableHttpResponse response) throws ZaasClientException, IOException {
        String token = "";
        int httpResponseCode = response.getStatusLine().getStatusCode();
        if (httpResponseCode == 204) {
            HeaderElement[] elements = response.getHeaders(SM.SET_COOKIE)[0].getElements();
            Optional<HeaderElement> apimlAuthCookie = Stream.of(elements)
                .filter(element -> element.getName().equals(TOKEN_PREFIX))
                .findFirst();
            if (apimlAuthCookie.isPresent()) {
                token = apimlAuthCookie.get().getValue();
            }
        } else {
            String obtainedMessage = EntityUtils.toString(response.getEntity());
            if (httpResponseCode == 401) {
                throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION, obtainedMessage);
            } else if (httpResponseCode == 400) {
                throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD, obtainedMessage);
            } else {
                throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, obtainedMessage);
            }
        }
        return token;
    }

    private void doRequest(Operation request) throws ZaasClientException {
        ClientWithResponse clientWithResponse = new ClientWithResponse();
        try {

            clientWithResponse = request.request();
    } catch (IOException e) {
        throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
    } catch (Exception e) {
        throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, e);
    } finally {
        finallyClose(clientWithResponse.getResponse());
    }
    }

    private Object doRequest(Operation request, Token token) throws ZaasClientException {
        ClientWithResponse clientWithResponse = new ClientWithResponse();

        try {

            clientWithResponse = request.request();

            return token.extract(clientWithResponse.getResponse());
        } catch (ZaasClientException e) {
            throw e;
        } catch (IOException e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, e);
        } finally {
            finallyClose(clientWithResponse.getResponse());
        }
    }

    @Data
    @AllArgsConstructor
    static class Credentials {
        String username;
        String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ClientWithResponse {
        CloseableHttpClient client;
        CloseableHttpResponse response;
    }

    interface Token {
        Object extract(CloseableHttpResponse response) throws IOException, ZaasClientException;
    }

    interface Operation {
        ClientWithResponse request() throws ZaasConfigurationException, IOException, ZaasClientException;
    }
}
