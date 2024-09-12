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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasToken;
import org.zowe.apiml.zaasclient.util.SimpleHttpResponse;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
class ZaasJwtService implements TokenService {

    private static final String BEARER_AUTHENTICATION_PREFIX = "Bearer";

    private final String loginEndpoint;
    private final String queryEndpoint;
    private final String logoutEndpoint;
    private final CloseableHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    ConfigProperties zassConfigProperties;

    public ZaasJwtService(CloseableHttpClient client, String baseUrl, ConfigProperties configProperties) {
        httpClient = client;
        loginEndpoint = baseUrl + "/login";
        queryEndpoint = baseUrl + "/query";
        logoutEndpoint = baseUrl + "/logout";
        zassConfigProperties = configProperties;
    }

    @Override
    public String login(String userId, char[] password, char[] newPassword) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithCredentials(userId, password, newPassword),
            this::processJwtTokenResponse,
            this::extractToken);
    }

    @Override
    public String login(String userId, char[] password) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithCredentials(userId, password, null),
            this::processJwtTokenResponse,
            this::extractToken);
    }

    private ClassicHttpRequest loginWithCredentials(String userId, char[] password, char[] newPassword) throws IOException {

        var httpPost = new HttpPost(loginEndpoint);
        String json = objectMapper.writeValueAsString(new Credentials(userId, password, newPassword));
        var entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        return httpPost;
    }

    private SimpleHttpResponse processJwtTokenResponse(ClassicHttpResponse response) throws ParseException, IOException {
        var headers = Arrays.stream(response.getHeaders(HttpHeaders.SET_COOKIE))
            .collect(Collectors.groupingBy((__) -> HttpHeaders.SET_COOKIE));
        if (response.getEntity() != null) {
            return new SimpleHttpResponse(response.getCode(), EntityUtils.toString(response.getEntity()), headers);
        } else {
            return new SimpleHttpResponse(response.getCode(), headers);
        }
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        return (String) doRequest(
            () -> loginWithHeader(authorizationHeader),
            this::processJwtTokenResponse,
            this::extractToken);
    }

    private ClassicHttpRequest loginWithHeader(String authorizationHeader) {
        HttpPost httpPost = new HttpPost(loginEndpoint);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return httpPost;
    }

    @Override
    public ZaasToken query(String jwtToken) throws ZaasClientException {
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "No token provided");
        }

        return (ZaasToken) doRequest(
            () -> queryWithJwtToken(jwtToken),
            SimpleHttpResponse::fromResponseWithBytesBodyOnSuccess,
            this::extractZaasToken);
    }

    @Override
    public ZaasToken query(@NonNull HttpServletRequest request) throws ZaasClientException {
        Optional<String> jwtToken = getJwtTokenFromRequest(request);
        return query(jwtToken.orElse(null));
    }

    @Override
    public void logout(String jwtToken) throws ZaasClientException {
        doLogoutRequest(() -> logoutJwtToken(jwtToken));
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

    private ClassicHttpRequest queryWithJwtToken(String jwtToken) {
        var httpGet = new HttpGet(queryEndpoint);
        httpGet.addHeader(HttpHeaders.COOKIE, zassConfigProperties.getTokenPrefix() + "=" + jwtToken);
        return httpGet;
    }

    private ClassicHttpRequest logoutJwtToken(String jwtToken) {
        HttpPost httpPost = new HttpPost(logoutEndpoint);
        if (jwtToken.startsWith(BEARER_AUTHENTICATION_PREFIX)) {
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, jwtToken);
        } else {
            httpPost.addHeader(HttpHeaders.COOKIE, zassConfigProperties.getTokenPrefix() + "=" + jwtToken);
        }
        return httpPost;
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

    private ZaasToken extractZaasToken(SimpleHttpResponse response) throws IOException, ZaasClientException {
        int statusCode = response.getCode();
        if (statusCode == 200) {
            ZaasToken token = objectMapper.readValue(response.getByteBody(), ZaasToken.class);

            if (token == null) {
                throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED, "Queried token is null");
            }
            if (token.isExpired()) {
                throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, "Queried token is expired");
            }

            return token;
        }

        if (statusCode == 401) {
            handleErrorMessage(response.getStringBody(), ZaasClientErrorCodes.EXPIRED_PASSWORD::equals);
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, "Queried token is invalid or expired");
        }
        throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, response.getStringBody());
    }

    private String extractToken(SimpleHttpResponse response) throws ZaasClientException, IOException {
        String token = "";
        int httpResponseCode = response.getCode();
        if (httpResponseCode == 204) {
            var cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE).stream().map(header -> HttpCookie.parse(header.getValue())).flatMap(List::stream).toList();
            var apimlAuthCookie = cookies.stream().filter(cookie -> cookie.getName().equals(zassConfigProperties.getTokenPrefix())).map(HttpCookie::getValue).findFirst();
            if (apimlAuthCookie.isPresent()) {
                token = apimlAuthCookie.get();
            }
            return token;
        }

        if (httpResponseCode == 401) {
            handleErrorMessage(response.getStringBody(), ZaasClientErrorCodes.EXPIRED_PASSWORD::equals);
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION, response.getStringBody());
        }
        if (httpResponseCode == 400) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD, response.getStringBody());
        }
        throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, response.getStringBody());
    }

    private void doLogoutRequest(OperationGenerator requestGenerator) throws ZaasClientException {
        var response = getSimpleResponse(requestGenerator, SimpleHttpResponse::fromResponseWithBytesBodyOnSuccess);

        if (response.getCode() == 401) {
            throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, response.getStringBody());
        } else if (!response.isSuccess()) {
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, response.getStringBody());
        }
    }

    private Object doRequest(
        OperationGenerator requestGenerator,
        HttpClientResponseHandler<SimpleHttpResponse> responseHandler,
        TokenExtractor token) throws ZaasClientException {

        try {
            return token.extract(getSimpleResponse(requestGenerator, responseHandler));
        } catch (ZaasClientException e) {
            throw e;
        } catch (IOException e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, e);

        }
    }

    private SimpleHttpResponse getSimpleResponse(OperationGenerator operationGenerator, HttpClientResponseHandler<SimpleHttpResponse> responseHandler)
        throws ZaasClientException {

        try {
            return httpClient.execute(operationGenerator.request(), responseHandler);
        } catch (IOException e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
        }
    }

    @Data
    @AllArgsConstructor
    static class Credentials {
        String username;
        char[] password;
        char[] newPassword;
    }

    interface TokenExtractor {
        Object extract(SimpleHttpResponse response) throws IOException, ZaasClientException;
    }

    interface OperationGenerator {
        ClassicHttpRequest request() throws IOException;
    }
}
