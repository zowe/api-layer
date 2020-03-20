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

import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.zowe.apiml.zaasclient.client.HttpsClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Stream;

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
    public ZaasToken query(String token) {
        return null;
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException {
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost("https://" + configProperties.getApimlHost() + ":" + configProperties.getApimlPort() + configProperties.getApimlBaseUrl() + "/ticket");
            String json = "{\"applicationName\":\"ZOWEAPPL\"}";
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Cookie", jwtToken);
            response = httpsClient.getHttpsClientWithKeyStoreAndTrustStore().execute(httpPost);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                String data = "";
                while ((data = reader.readLine()) != null) {
                    System.out.println(data);
                }
            }
            System.out.println(response);
        } catch (IOException ioe) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        } finally {
            finallyClose(response);
        }
        return "";
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
