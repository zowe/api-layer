/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * service class offered methods for checking onboarding information and also retrieve metadata from
 * provided serviceid.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationOnboardService {


    private final DiscoveryConfigUri discoveryConfigUri;

    @Qualifier("secureHttpClientWithoutKeystore")
    private final CloseableHttpClient closeableHttpClient;

    /**
     * Accept serviceId and check if the service is onboarded to the API Mediation Layer
     * @param serviceId accept serviceId to check
     * @return return true if the service is known by Eureka otherwise false.
     * @throws IOException
     */
    public boolean checkOnboarding(String serviceId) throws IOException {

        Boolean check = false;
        HttpGet httpget = constructHttpGet(serviceId);
        try {
            CloseableHttpResponse response = closeableHttpClient.execute(httpget);
            check = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;

        } catch (Exception e) {
            log.debug("Error Ocurred: " + e.getMessage());
        } 
        return check;
    }

    /**
     * Accept serviceId and check if the 
     * @param serviceId accept serviceId to check
     * @return return swagger Url if the metadata can be retrieved, otherwise an empty string.
     * @throws IOException
     */
    public String retrieveMetaData(String serviceId) throws IOException {

        HttpGet httpget = constructHttpGet(serviceId);


        String swaggerUrl = "";
        try {
            CloseableHttpResponse response = closeableHttpClient.execute(httpget);
            String responseString = EntityUtils.toString(response.getEntity());

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setExpandEntityReferences(false);
            builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(responseString));
            Document doc = builder.parse(src);

            swaggerUrl = doc.getElementsByTagName("apiml.apiInfo.api-v2.swaggerUrl").item(0).getTextContent();

        } catch (Exception e) {
            log.debug("Error Ocurred: " + e.getMessage());
        } 
        return swaggerUrl;

    }

    /**
     * private method for getting valid discovery service url
     * @return the valid discovery service url
     */
    private List<String> getDiscoveryServiceUrls() {
        String[] discoveryUriLoStrings = discoveryConfigUri.getLocations();

        List<String> discoveryServiceUrls = new ArrayList<>();
        for (String location : discoveryUriLoStrings) {
            discoveryServiceUrls.add(location + "apps/");
        }

        return discoveryServiceUrls;
    }

    /**
     * private method for construct an HttpGet object using provided
     * @param endPoint serviceId that needs to be add to the end to construct discovery service url.
     * @return the HttpGet object which can be executed by HttpClient
     */
    private HttpGet constructHttpGet(String endPoint) {
        List<String> discoveryUrls = getDiscoveryServiceUrls();

        String url = String.format("%s" + "%s", discoveryUrls.get(0), endPoint);
        return new HttpGet(url);
    }

}