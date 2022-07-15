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


@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationOnboardService {
    

    private final DiscoveryConfigUri discoveryConfigUri;

    @Qualifier("secureHttpClientWithoutKeystore")
    private final CloseableHttpClient closeableHttpClient;


    public boolean checkOnboarding(String serviceId) throws IOException {

    
        HttpGet httpget = constructHttpGet(serviceId);
        try {
            CloseableHttpResponse response = closeableHttpClient.execute(httpget);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        
        } catch (Exception e) {
            log.debug("Error Ocurred: " + e.getMessage());
            e.printStackTrace();
        } 
        return false;
    }


    public String retrieveMetaData(String serviceId) throws IOException {
        
        HttpGet httpget = constructHttpGet(serviceId);


        String swaggerUrl = "";
        try {
            CloseableHttpResponse response = closeableHttpClient.execute(httpget);
            String responseString = EntityUtils.toString(response.getEntity());
            
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(responseString));
            Document doc = builder.parse(src);

            swaggerUrl = doc.getElementsByTagName("apiml.apiInfo.api-v2.swaggerUrl").item(0).getTextContent();
            return swaggerUrl;
            

        } catch (Exception e) {
            log.debug("Error Ocurred: " + e.getMessage());
            e.printStackTrace();
        } 
        return swaggerUrl;
        
    }


    private List<String> getDiscoveryServiceUrls() {
        String[] discoveryUriLoStrings = discoveryConfigUri.getLocations();

        List<String> discoveryServiceUrls = new ArrayList<>();
        for (String location : discoveryUriLoStrings) {
            discoveryServiceUrls.add(location);
        }

        return discoveryServiceUrls;
    }

    private HttpGet constructHttpGet(String endPoint) {
        List<String> discoveryUrls = getDiscoveryServiceUrls();

        String url = String.format("%s" + "apps/" + "%s", discoveryUrls.get(0), endPoint);
        HttpGet httpget = new HttpGet(url);
        return httpget;
    }

}
