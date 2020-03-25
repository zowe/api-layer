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
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.passTicket.ZaasClientTicketRequest;
import org.zowe.apiml.zaasclient.passTicket.ZaasPassTicketResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class TokenServiceImpl implements TokenService {

    private ConfigProperties configProperties;
    private HttpsClient httpsClient;

    @Override
    public void init(ConfigProperties configProperties) {
        this.configProperties = configProperties;
        this.httpsClient = new HttpsClient(configProperties);
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        String token = "";

        if (userId == null || password == null)
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);

        try {
            client = httpsClient.getHttpsClientWithTrustStore();
            HttpPost httpPost = new HttpPost("https://" + configProperties.getApimlHost() + ":" + configProperties.getApimlPort() + configProperties.getApimlBaseUrl() + "/login");
            String json = "{\"username\":\"" + userId + "\",\"password\":\"" + password + "\"}";
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/json");
            response = client.execute(httpPost);
            token = extractToken(response);
        } catch (ZaasClientException zce) {
            throw zce;
        } catch (IOException ioe) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        } finally {
            finallyClose(response);
        }
        return token;
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        CloseableHttpResponse response = null;
        String token = "";
        CloseableHttpClient client = null;

        if (authorizationHeader == null || authorizationHeader.isEmpty())
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER);

        try {
            HttpPost httpPost = new HttpPost("https://" + configProperties.getApimlHost() + ":" + configProperties.getApimlPort() + configProperties.getApimlBaseUrl() + "/login");
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
            response = httpsClient.getHttpsClientWithTrustStore().execute(httpPost);
            token = extractToken(response);
        } catch (ZaasClientException zce) {
            throw zce;
        } catch (IOException ioe) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        } finally {
            finallyClose(response);
        }
        return token;
    }

    @Override
    public ZaasToken query(String token) throws ZaasClientException {

        CloseableHttpResponse response = null;
        ZaasToken zaasToken = null;

        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie(COOKIE_PREFIX, token);
        cookie.setDomain(configProperties.getApimlHost());
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        try {
            HttpGet httpGet = new HttpGet("https://" + configProperties.getApimlHost() + ":" + configProperties.getApimlPort() + configProperties.getApimlBaseUrl() + "/query");
            response = httpsClient.getHttpsClientWithTrustStore(cookieStore).execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                zaasToken = new ObjectMapper().readValue(response.getEntity().getContent(), ZaasToken.class);
            } else {
                log.error(EntityUtils.toString(response.getEntity()));
                throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION);
            }
        } catch (IOException e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        } finally {
            finallyClose(response);
        }
        return zaasToken;
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException {
        CloseableHttpResponse response = null;
        CloseableHttpClient closeableHttpsClient = null;
        ZaasPassTicketResponse zaasPassTicketResponse = null;
        ZaasClientTicketRequest zaasClientTicketRequest = new ZaasClientTicketRequest();
        ObjectMapper mapper = new ObjectMapper();
        if (Objects.isNull(applicationId) || applicationId.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.APPLICATION_NAME_NOT_FOUND);
        }
        if (Objects.isNull(jwtToken) || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED);
        }
        try {
            closeableHttpsClient = httpsClient.getHttpsClientWithKeyStoreAndTrustStore();
            zaasClientTicketRequest.setApplicationName(applicationId);

            HttpPost httpPost = new HttpPost("https://" + configProperties.getApimlHost() + ":" +
                configProperties.getApimlPort() + configProperties.getApimlBaseUrl() + "/ticket");
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(zaasClientTicketRequest)));
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Cookie", COOKIE_PREFIX + "=" + jwtToken);

            response = closeableHttpsClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 500) {
                throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
            } else if (response.getStatusLine().getStatusCode() == 401) {
                throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION);
            } else if (response.getStatusLine().getStatusCode() == 200) {
                zaasPassTicketResponse = new ObjectMapper().readValue(response.getEntity().getContent(), ZaasPassTicketResponse.class);
            }
        } catch (IOException ioe) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, ioe.getMessage());
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e.getMessage());
        } finally {
            finallyClose(response);
        }
        return zaasPassTicketResponse.getTicket();
    }

    private void finallyClose(CloseableHttpResponse response) {
        try {
            if (response != null)
                response.close();
            if (httpsClient != null)
                httpsClient.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    private String extractToken(CloseableHttpResponse response) throws ZaasClientException {
        String token = "";
        int httpResponseCode = response.getStatusLine().getStatusCode();
        if (httpResponseCode == 204) {
            HeaderElement[] elements = response.getHeaders("Set-Cookie")[0].getElements();
            Optional<HeaderElement> apimlAuthCookie = Stream.of(elements).filter(element -> element.getName().equals("apimlAuthenticationToken")).findFirst();
            if (apimlAuthCookie.isPresent())
                token = apimlAuthCookie.get().getValue();
        } else if (httpResponseCode == 401) {
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION);
        } else if (httpResponseCode == 400) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        } else {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        }
        return token;
    }
}
