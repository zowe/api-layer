/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.security.client.handler.RestResponseHandler;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Core class of security client
 * provides facility for performing login and validating JWT token
 */
@Service
@RequiredArgsConstructor
public class GatewaySecurityService {
    private static final String MESSAGE_KEY_STRING = "messageKey\":\"";

    private final GatewayClient gatewayClient;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final CloseableHttpClient closeableHttpClient;
    private final RestResponseHandler responseHandler;

    /**
     * Logs into the gateway with username and password, and retrieves valid JWT token
     *
     * @param username Username
     * @param password Password
     * @return Valid JWT token for the supplied credentials
     */
    public Optional<String> login(String username, String password, String newPassword) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayLoginEndpoint());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode loginRequest = mapper.createObjectNode();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        if (StringUtils.isNotEmpty(newPassword)) {
            loginRequest.put("newPassword", newPassword);
        }
        try {
            HttpPost post = new HttpPost(uri);
            String json = mapper.writeValueAsString(loginRequest);
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = closeableHttpClient.execute(post);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                final HttpEntity responseEntity = response.getEntity();
                String responseBody = null;
                if (responseEntity != null) {
                    responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                }
                ErrorType errorType = getErrorType(responseBody);
                responseHandler.handleErrorType(response, errorType,
                    "Cannot access Gateway service. Uri '{}' returned: {}", uri);
                return Optional.empty();
            }
            return extractToken(response.getFirstHeader(HttpHeaders.SET_COOKIE).getValue());
        } catch (IOException e) {
            responseHandler.handleException(e);
        }
        return Optional.empty();
    }

    /**
     * Verifies JWT token validity and returns JWT token data
     *
     * @param token JWT token to be validated
     * @return JWT token data as {@link QueryResponse}
     */
    public QueryResponse query(String token) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), authConfigurationProperties.getGatewayQueryEndpoint());
        String cookie = String.format("%s=%s", authConfigurationProperties.getCookieProperties().getCookieName(), token);


        try {
            HttpGet get = new HttpGet(uri);
            get.addHeader(HttpHeaders.COOKIE, cookie);
            CloseableHttpResponse response = closeableHttpClient.execute(get);
            final HttpEntity responseEntity = response.getEntity();
            String responseBody = null;
            if (responseEntity != null) {
                responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            }
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                ErrorType errorType = getErrorType(responseBody);
                responseHandler.handleErrorType(response, errorType,
                    "Cannot access Gateway service. Uri '{}' returned: {}", uri);
                return null;
            }
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseBody, QueryResponse.class);
        } catch (IOException e) {
            responseHandler.handleException(e);
        }
        return null;
    }

    private ErrorType getErrorType(String detailMessage) {
        if (detailMessage == null) {
            return ErrorType.AUTH_GENERAL;
        }

        int indexOfMessageKey = detailMessage.indexOf(MESSAGE_KEY_STRING);
        if (indexOfMessageKey < 0) {
            return ErrorType.AUTH_GENERAL;
        }

        // substring from `messageKey":"` to next `"` - this is the messageKey value
        String messageKeyToEndOfExceptionMessage = detailMessage.substring(indexOfMessageKey + MESSAGE_KEY_STRING.length());
        String messageKey = messageKeyToEndOfExceptionMessage.substring(0, messageKeyToEndOfExceptionMessage.indexOf("\""));

        try {
            return ErrorType.fromMessageKey(messageKey);
        } catch (IllegalArgumentException e) {
            return ErrorType.AUTH_GENERAL;
        }
    }

    private Optional<String> extractToken(String cookies) {
        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();

        if (cookies == null || cookies.isEmpty() || !cookies.contains(cookieName)) {
            return Optional.empty();
        } else {
            int end = cookies.indexOf(';');
            String cookie = (end > 0) ? cookies.substring(0, end) : cookies;
            return Optional.of(cookie.replace(cookieName + "=", ""));
        }
    }
}
