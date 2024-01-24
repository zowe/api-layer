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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
class ZaasJwtService implements TokenService {

    private static final String BEARER_AUTHENTICATION_PREFIX = "Bearer";

    private final String loginEndpoint;
    private final String queryEndpoint;
    private final String logoutEndpoint;
    private final CloseableClientProvider httpClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    ConfigProperties zassConfigProperties;

    public ZaasJwtService(CloseableClientProvider client, String baseUrl, ConfigProperties configProperties) {
        this.httpClientProvider = client;

        loginEndpoint = baseUrl + "/login";
        queryEndpoint = baseUrl + "/query";
        logoutEndpoint = baseUrl + "/logout";
        zassConfigProperties = configProperties;
    }

    @Override
    public String login(String userId, char[] password, char[] newPassword) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithCredentials(userId, password, newPassword),
            this::extractToken);
    }

    @Override
    public String login(String userId, char[] password) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithCredentials(userId, password, null),
            this::extractToken);
    }

    private ClientWithResponse loginWithCredentials(String userId, char[] password, char[] newPassword) throws ZaasConfigurationException, IOException {
        var client = httpClientProvider.getHttpClient();
        var httpPost = new HttpPost(loginEndpoint);
        String json = objectMapper.writeValueAsString(new Credentials(userId, password, newPassword));
        var entity = new StringEntity(json);
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
        var client = httpClientProvider.getHttpClient();
        HttpPost httpPost = new HttpPost(loginEndpoint);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return new ClientWithResponse(client, client.execute(httpPost));
    }

    @Override
    public ZaasToken query(String jwtToken) throws ZaasClientException {
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided");
        }

        return (ZaasToken) doRequest(() -> queryWithJwtToken(jwtToken), this::extractZaasToken);
    }

    @Override
    public ZaasToken query(@NonNull HttpServletRequest request) throws ZaasClientException {
        Optional<String> jwtToken = getJwtTokenFromRequest(request);
        return query(jwtToken.orElse(null));
    }

    @Override
    public void logout(String jwtToken) throws ZaasClientException {
        doRequest(() -> logoutJwtToken(jwtToken));
    }

    /**
     * Get the JWT token from the authorization header in the http request
     * <p>
     * Order:
     * 1. Cookie
     * 2. Authorization header
     *
     * @param request the http request
     * @return the JWT token
     */
    private Optional<String> getJwtTokenFromRequest(@NonNull HttpServletRequest request) {
        Optional<String> fromCookie = getJwtTokenFromCookie(request);
        return fromCookie.isPresent() ?
            fromCookie : extractJwtTokenFromAuthorizationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    private Optional<String> getJwtTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(zassConfigProperties.getTokenPrefix()))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(Cookie::getValue);
    }

    private Optional<String> extractJwtTokenFromAuthorizationHeader(String header) {
        if (header != null && header.startsWith(BEARER_AUTHENTICATION_PREFIX)) {
            header = header.replaceFirst(BEARER_AUTHENTICATION_PREFIX, "").trim();
            if (header.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(header);
        }

        return Optional.empty();
    }

    private ClientWithResponse queryWithJwtToken(String jwtToken) throws ZaasConfigurationException, IOException {
        var client = httpClientProvider.getHttpClient();
        var httpGet = new HttpGet(queryEndpoint);
        httpGet.addHeader(HttpHeaders.COOKIE, zassConfigProperties.getTokenPrefix() + "=" + jwtToken);
        return new ClientWithResponse(client, client.execute(httpGet));
    }

    private ClientWithResponse logoutJwtToken(String jwtToken) throws ZaasConfigurationException, IOException, ZaasClientException {
        var client = httpClientProvider.getHttpClient();
        clearZaasClientCookies();
        HttpPost httpPost = new HttpPost(logoutEndpoint);
        if (jwtToken.startsWith(BEARER_AUTHENTICATION_PREFIX)) {
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, jwtToken);
        } else {
            httpPost.addHeader(HttpHeaders.COOKIE, zassConfigProperties.getTokenPrefix() + "=" + jwtToken);
        }
        return getClientWithResponse(client, httpPost);
    }

    private void clearZaasClientCookies() {
        if (httpClientProvider instanceof ZaasHttpsClientProvider) {
            ((ZaasHttpsClientProvider) httpClientProvider).clearCookieStore();
        }
    }

    private ClientWithResponse getClientWithResponse(CloseableHttpClient client, HttpPost httpPost) throws IOException, ZaasClientException {
        ClientWithResponse clientWithResponse = new ClientWithResponse(client, client.execute(httpPost,r->r));
        int httpResponseCode = clientWithResponse.getResponse().getCode();
        if (httpResponseCode == 204) {
            return clientWithResponse;
        } else {
            String obtainedMessage = EntityUtils.toString(clientWithResponse.);
            if (httpResponseCode == 401) {
                throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, obtainedMessage);
            } else {
                throw new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, obtainedMessage);
            }
        }
    }

    private void finallyClose(ClassicHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            log.warn("It wasn't possible to close the resources. " + e.getMessage());
        }
    }

    private void handleErrorMessage(JsonNode message, Predicate<ZaasClientErrorCodes> condition) throws ZaasClientException {
        JsonNode messageNumberNode = message.get("messageNumber");
        if ((messageNumberNode != null) && (messageNumberNode.getNodeType() == JsonNodeType.STRING)) {
            String messageNumber = messageNumberNode.asText();
            ZaasClientErrorCodes zaasClientErrorCode = ZaasClientErrorCodes.byErrorNumber(messageNumber);
            if (condition.test(zaasClientErrorCode)) {
                throw new ZaasClientException(zaasClientErrorCode, zaasClientErrorCode.getMessage());
            }
        }
    }

    private void handleErrorMessage(String errorMessage, Predicate<ZaasClientErrorCodes> condition) throws ZaasClientException, IOException {
        if (errorMessage == null) return;

        JsonNode jsonNode = objectMapper.readTree(errorMessage);
        JsonNode messages = jsonNode.get("messages");
        if ((messages != null) && (messages.getNodeType() == JsonNodeType.ARRAY)) {
            ArrayNode messagesArray = (ArrayNode) messages;
            for (JsonNode message : messagesArray) {
                handleErrorMessage(message, condition);
            }
        }
    }

    private ZaasToken extractZaasToken(CloseableHttpResponse response) throws IOException, ZaasClientException, ParseException {
        int statusCode = response.getCode();
        if (statusCode == 200) {
            ZaasToken token = objectMapper.readValue(response.getEntity().getContent(), ZaasToken.class);

            if (token == null) {
                throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "Queried token is null");
            }
            if (token.isExpired()) {
                throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, "Queried token is expired");
            }

            return token;
        }

        String obtainedMessage = EntityUtils.toString(response.getEntity());
        if (statusCode == 401) {
            handleErrorMessage(obtainedMessage, ZaasClientErrorCodes.EXPIRED_PASSWORD::equals);
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, "Queried token is invalid or expired");
        }
        throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, obtainedMessage);
    }

    private String extractToken(CloseableHttpResponse response) throws ZaasClientException, IOException {
        String token = "";
        int httpResponseCode = response.getCode();
        if (httpResponseCode == 204) {
            var elements = response.getHeaders(HttpHeaders.COOKIE)[0].getElements();
            Optional<HeaderElement> apimlAuthCookie = Stream.of(elements)
                .filter(element -> element.getName().equals(zassConfigProperties.getTokenPrefix()))
                .findFirst();
            if (apimlAuthCookie.isPresent()) {
                token = apimlAuthCookie.get().getValue();
            }
            return token;
        }

        String obtainedMessage = EntityUtils.toString(response.getEntity());
        if (httpResponseCode == 401) {
            handleErrorMessage(obtainedMessage, ZaasClientErrorCodes.EXPIRED_PASSWORD::equals);
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION, obtainedMessage);
        }
        if (httpResponseCode == 400) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD, obtainedMessage);
        }
        throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, obtainedMessage);
    }

    private void doRequest(Operation request) throws ZaasClientException {
        ClientWithResponse clientWithResponse = new ClientWithResponse();
        try {
            clientWithResponse = request.request();
        } catch (IOException | ZaasConfigurationException e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
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
        char[] password;
        char[] newPassword;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ClientWithResponse {
        CloseableHttpClient client;
        ClassicHttpResponse response;
    }

    interface Token {
        Object extract(ClassicHttpResponse response) throws IOException, ZaasClientException;
    }

    interface Operation {
        ClientWithResponse request() throws ZaasConfigurationException, IOException, ZaasClientException;
    }
}
