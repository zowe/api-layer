/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.jackson.EurekaJsonJacksonCodec;
import com.netflix.discovery.shared.Applications;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.product.registry.ApplicationWrapper;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for instance retrieval from Eureka
 */
@Slf4j
@Service
public class InstanceRetrievalService {

    private final DiscoveryConfigProperties discoveryConfigProperties;
    private final CloseableHttpClient httpClient;

    private static final String APPS_ENDPOINT = "apps/";
    private static final String DELTA_ENDPOINT = "delta";
    private static final String UNKNOWN = "unknown";

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Autowired
    public InstanceRetrievalService(DiscoveryConfigProperties discoveryConfigProperties,
                                    CloseableHttpClient httpClient) {
        this.discoveryConfigProperties = discoveryConfigProperties;
        this.httpClient = httpClient;
    }

    /**
     * Retrieves {@link InstanceInfo} of particular service
     *
     * @param serviceId the service to search for
     * @return service instance
     */
    public InstanceInfo getInstanceInfo(@NotBlank(message = "Service Id must be supplied") String serviceId) {
        if (serviceId.equalsIgnoreCase(UNKNOWN)) {
            return null;
        }

        List<Pair<String, Pair<String, String>>> requestInfoList = constructServiceInfoQueryRequest(serviceId, false);
        // iterate over list of discovery services, return at first success
        for (Pair<String, Pair<String, String>> requestInfo : requestInfoList) {
            // call Eureka REST endpoint to fetch single or all Instances
            try {
                String responseBody = queryDiscoveryForInstances(requestInfo, serviceId);
                if (responseBody != null) {
                    return extractSingleInstanceFromApplication(serviceId, responseBody);
                }
            } catch (Exception e) {
                log.debug("Error getting instance info from {}, error message: {}", requestInfo.getLeft(), e.getMessage());
            }
        }
        String msg = "An error occurred when trying to get instance info for:  " + serviceId;
        throw new InstanceInitializationException(msg);
    }

    /**
     * Retrieve instances from the discovery service
     *
     * @param delta filter the registry information to the just updated infos
     * @return the Applications object that wraps all the registry information
     */
    public Applications getAllInstancesFromDiscovery(boolean delta) {

        List<Pair<String, Pair<String, String>>> requestInfoList = constructServiceInfoQueryRequest(null, delta);
        for (Pair<String, Pair<String, String>> requestInfo : requestInfoList) {
            try {
                String responseBody = queryDiscoveryForInstances(requestInfo, null);
                return extractApplications(responseBody);
            } catch (Exception e) {
                log.debug("Not able to contact discovery service: " + requestInfo.getKey(), e);
            }
        }
        //  call Eureka REST endpoint to fetch single or all Instances
        return null;
    }

    /**
     * Parse information from the response and extract the Applications object which contains all the registry information returned by eureka server
     *
     * @param responseBody the http response body
     * @return Applications object that wraps all the registry information
     */
    private Applications extractApplications(String responseBody) {
        Applications applications = null;
        ObjectMapper mapper = new EurekaJsonJacksonCodec().getObjectMapper(Applications.class);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            applications = mapper.readValue(responseBody, Applications.class);
        } catch (IOException e) {
            apimlLog.log("org.zowe.apiml.apicatalog.serviceRetrievalParsingFailed", e.getMessage());
        }

        return applications;
    }

    /**
     * Query Discovery
     *
     * @param requestInfo information used to query the discovery service
     * @return ResponseEntity<String> query response
     */
    private String queryDiscoveryForInstances(Pair<String, Pair<String, String>> requestInfo, String serviceId) throws IOException {
        HttpGet httpGet = new HttpGet(requestInfo.getLeft());
        for (Header header : createRequestHeader(requestInfo.getRight())) {
            httpGet.setHeader(header);
        }
        CloseableHttpResponse response = httpClient.execute(httpGet);
        final int statusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;
        final HttpEntity responseEntity = response.getEntity();
        String responseBody = "";
        if (responseEntity != null) {
            responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        }
        if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
            return responseBody;
        }

        apimlLog.log("org.zowe.apiml.apicatalog.serviceRetrievalRequestFailed",
            serviceId != null ? "'" + serviceId + "' "  : "",
            requestInfo.getLeft(),
            statusCode,
            response.getStatusLine() != null ? response.getStatusLine().getReasonPhrase() : responseBody
            );

        return null;
    }

    /**
     * @param serviceId    the service to search for
     * @param responseBody the fetch attempt response body
     * @return service instance
     */
    private InstanceInfo extractSingleInstanceFromApplication(String serviceId, String responseBody) {
        ApplicationWrapper application = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            application = mapper.readValue(responseBody, ApplicationWrapper.class);
        } catch (IOException e) {
            log.debug("Could not extract service: " + serviceId + " info from discovery --" + e.getMessage(), e);
        }


        if (application != null
            && application.getApplication() != null
            && application.getApplication().getInstances() != null
            && !application.getApplication().getInstances().isEmpty()) {
            return application.getApplication().getInstances().get(0);
        } else {
            return null;
        }
    }

    /**
     * Construct a tuple used to query the discovery service
     *
     * @param serviceId optional service id
     * @return request information
     */
    private List<Pair<String, Pair<String, String>>> constructServiceInfoQueryRequest(String serviceId, boolean getDelta) {
        String[] discoveryServiceUrls = discoveryConfigProperties.getLocations();
        List<Pair<String, Pair<String, String>>> discoveryPairs = new ArrayList<>(discoveryServiceUrls.length);
        for (String discoveryUrl : discoveryServiceUrls) {
            String discoveryServiceLocatorUrl = discoveryUrl + APPS_ENDPOINT;
            if (getDelta) {
                discoveryServiceLocatorUrl += DELTA_ENDPOINT;
            } else {
                if (serviceId != null) {
                    discoveryServiceLocatorUrl += serviceId.toLowerCase();
                }
            }

            String eurekaUsername = discoveryConfigProperties.getEurekaUserName();
            String eurekaUserPassword = discoveryConfigProperties.getEurekaUserPassword();

            Pair<String, String> discoveryServiceCredentials = Pair.of(eurekaUsername, eurekaUserPassword);

            log.debug("Eureka credentials retrieved for user: {} {}",
                eurekaUsername,
                (!eurekaUserPassword.isEmpty() ? "*******" : "NO PASSWORD")
            );

            log.debug("Checking instance info from: " + discoveryServiceLocatorUrl);
            discoveryPairs.add(Pair.of(discoveryServiceLocatorUrl, discoveryServiceCredentials));
        }
        return discoveryPairs;
    }

    /**
     * Create HTTP headers
     *
     * @return HTTP Headers
     */
    private List<Header> createRequestHeader(Pair<String, String> credentials) {
        List<Header> headers = new ArrayList<>();
        if (credentials != null && credentials.getLeft() != null && credentials.getRight() != null) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((credentials.getLeft() + ":"
                + credentials.getRight()).getBytes());
            headers.add(new BasicHeader(HttpHeaders.AUTHORIZATION, basicToken));
        }
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }
}
