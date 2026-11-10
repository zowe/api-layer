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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.oidc.ZaasOidcValidationResult;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class ZaasClientImpl implements ZaasClient, Closeable {

    private final TokenService tokens;
    private final PassTicketService passTickets;
    private final CloseableHttpClient httpClientWithoutCert;
    private final CloseableHttpClient httpClient;
    private final boolean ownsClients;

    public ZaasClientImpl(ConfigProperties configProperties) throws ZaasConfigurationException {
        if (!configProperties.isHttpOnly() && (configProperties.getKeyStorePath() == null)) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.KEY_STORE_NOT_PROVIDED);
        }

        if (configProperties.isHttpOnly()) {
            httpClient = new ZaasHttpClientProvider().getHttpClient();
            httpClientWithoutCert = new ZaasHttpClientProvider().getHttpClient();
        } else {
            httpClient = new ZaasHttpsClientProvider(configProperties).getHttpClient();
            httpClientWithoutCert = new ZaasHttpsClientProvider(configProperties.withoutKeyStore()).getHttpClient();
        }

        String baseUrl = getBaseUrl(configProperties);
        tokens = new ZaasJwtService(httpClientWithoutCert, baseUrl, configProperties);
        passTickets = new PassTicketServiceImpl(httpClient, baseUrl, configProperties);
        ownsClients = true;
    }

    /**
     * Creates a client that sends ZAAS requests through the supplied HTTP clients.
     * <p>
     * {@code httpClient} is used for pass ticket requests. {@code httpClientWithoutCert} is used for login, query,
     * logout and OIDC validation, which the default constructor sends without a client certificate so that ZAAS
     * authenticates the caller by credentials or token and not by certificate.
     * <p>
     * The caller must configure {@code httpClient} with a client certificate for HTTPS pass ticket requests
     * and configure {@code httpClientWithoutCert} without a client certificate. The supplied clients carry their own
     * TLS configuration, therefore no key store is required in {@code configProperties}. The URLs are still built
     * from the host, port, base URL and the {@code httpOnly} flag of {@code configProperties}.
     * <p>
     * Cookie management remains as configured on each supplied client and is the caller's responsibility.
     * The built-in clients disable automatic cookie management. With automatic cookie management enabled on
     * {@code httpClientWithoutCert}, a cached ZAAS authentication cookie can take precedence over an explicitly
     * supplied bearer token during logout, causing a different session to be invalidated. For stateless behavior,
     * build both supplied clients with {@code HttpClientBuilder.disableCookieManagement()}.
     * <p>
     * The caller owns the supplied clients: {@link #close()} does not close them.
     *
     * @param configProperties      ZAAS location and token settings
     * @param httpClient            client configured with a client certificate for HTTPS pass ticket requests
     * @param httpClientWithoutCert client without a client certificate, used for login, query, logout and OIDC validation
     */
    public ZaasClientImpl(ConfigProperties configProperties, CloseableHttpClient httpClient, CloseableHttpClient httpClientWithoutCert) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.httpClientWithoutCert = Objects.requireNonNull(httpClientWithoutCert, "httpClientWithoutCert must not be null");
        String baseUrl = getBaseUrl(configProperties);
        tokens = new ZaasJwtService(httpClientWithoutCert, baseUrl, configProperties);
        passTickets = new PassTicketServiceImpl(httpClient, baseUrl, configProperties);
        ownsClients = false;
    }

    private String getBaseUrl(ConfigProperties configProperties) {
        return String.format("%s://%s:%s%s", getScheme(configProperties.isHttpOnly()), configProperties.getApimlHost(), configProperties.getApimlPort(),
            configProperties.getApimlBaseUrl());
    }

    private Object getScheme(boolean httpOnly) {
        if (httpOnly) {
            return "http";
        } else {
            return "https";
        }
    }

    ZaasClientImpl(TokenService tokens, PassTicketService passTickets) {
        this.tokens = tokens;
        this.passTickets = passTickets;
        this.httpClientWithoutCert = null;
        this.httpClient = null;
        this.ownsClients = false;
    }

    @Override
    public String login(String userId, String password, String newPassword) throws ZaasClientException {
        char[] passwordChars = password == null ? null : password.toCharArray();
        char[] newPasswordChars = newPassword == null ? null : newPassword.toCharArray();
        try {
            return login(userId, passwordChars, newPasswordChars);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, (char) 0);
            }
            if (newPasswordChars != null) {
                Arrays.fill(newPasswordChars, (char) 0);
            }
        }
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        char[] passwordChars = password == null ? null : password.toCharArray();
        try {
            return login(userId, passwordChars);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, (char) 0);
            }
        }
    }

    @Override
    public String login(String userId, char[] password, char[] newPassword) throws ZaasClientException {
        if (userId == null || password == null || newPassword == null || userId.isEmpty() || password.length == 0 || newPassword.length == 0) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        }
        return tokens.login(userId, password, newPassword);
    }

    @Override
    public String login(String userId, char[] password) throws ZaasClientException {
        if (userId == null || password == null || userId.isEmpty() || password.length == 0) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        }
        return tokens.login(userId, password);
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER);
        }
        return tokens.login(authorizationHeader);

    }

    @Override
    public ZaasToken query(String token) throws ZaasClientException {
        return tokens.query(token);
    }

    @Override
    public ZaasToken query(HttpServletRequest request) throws ZaasClientException {
        return tokens.query(request);
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException, ZaasConfigurationException {
        if (Objects.isNull(applicationId) || applicationId.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.APPLICATION_NAME_NOT_FOUND);
        }
        if (Objects.isNull(jwtToken) || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED);
        }
        return passTickets.passTicket(jwtToken, applicationId);
    }

    @Override
    public void logout(String jwtToken) throws ZaasConfigurationException, ZaasClientException {
        tokens.logout(jwtToken);
    }

    @Override
    public void close() throws IOException {
        if (ownsClients) {
            httpClient.close();
            httpClientWithoutCert.close();
        }
    }

    @Override
    public ZaasOidcValidationResult validateOidc(String token) throws ZaasClientException {
        if (token == null || token.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED);
        }
        return tokens.validateOidc(token);
    }
}
