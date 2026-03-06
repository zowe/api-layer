/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.staticdef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.appinfo.InstanceInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.util.EurekaUtils;

import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.API_INFO;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.API_INFO_VERSION;

/**
 * Enriches z/OSMF service metadata with the actual version retrieved from the z/OSMF info endpoint.
 * <p>
 * When z/OSMF service is registered, this component calls the /zosmf/info endpoint
 * to retrieve the actual z/OSMF version and updates the service metadata accordingly.
 * This ensures the version is displayed correctly in the API Catalog UI.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ZosmfVersionEnricher {

    private static final String ZOSMF_SERVICE_ID = "ibmzosmf";
    private static final String ZOSMF_INFO_ENDPOINT = "/zosmf/info";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    private static final String THREE_STRING_MERGE_FORMAT = "%s.%s.%s";
    private static final String DEFAULT_GATEWAY_URL = "api-v1";

    @Qualifier("restTemplateWithoutKeystore")
    private final RestTemplate restTemplate;

    /**
     * DTO with base information about z/OSMF (version and realm/domain)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZosmfInfo {

        @JsonProperty("zosmf_version")
        private int version;

        @JsonProperty("zosmf_full_version")
        private String fullVersion;

        @JsonProperty("zosmf_saf_realm")
        private String safRealm;
    }

    /**
     * Listens for z/OSMF service registration events and enriches the metadata with version information.
     * This event listener runs after the standard metadata translation to ensure existing metadata is preserved.
     *
     * @param event the Eureka instance registration event
     */
    @EventListener
    @Order(100) // Run after other listeners
    public void onInstanceRegistered(EurekaInstanceRegisteredEvent event) {
        final InstanceInfo instanceInfo = event.getInstanceInfo();
        final String serviceId = EurekaUtils.getServiceIdFromInstanceId(instanceInfo.getInstanceId());

        if (!ZOSMF_SERVICE_ID.equalsIgnoreCase(serviceId)) {
            return;
        }

        log.debug("z/OSMF service registered, attempting to enrich version metadata");
        enrichZosmfVersion(instanceInfo);
    }

    /**
     * Calls the z/OSMF info endpoint and enriches the instance metadata with version information.
     *
     * @param instanceInfo the z/OSMF instance information
     */
    private void enrichZosmfVersion(InstanceInfo instanceInfo) {
        String zosmfUrl = buildZosmfInfoUrl(instanceInfo);
        
        try {
            ZosmfInfo zosmfInfo = fetchZosmfInfo(zosmfUrl);
            
            if (zosmfInfo != null && zosmfInfo.getFullVersion() != null) {
                updateMetadataWithVersion(instanceInfo.getMetadata(), zosmfInfo.getFullVersion());
                log.info("Successfully enriched z/OSMF metadata with version: {}", zosmfInfo.getFullVersion());
            } else {
                log.warn("z/OSMF info endpoint returned null or missing version information");
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve z/OSMF version from {}: {}. The version will not be displayed in UI.", 
                    zosmfUrl, e.getMessage());
            log.debug("Exception details:", e);
        }
    }

    /**
     * Builds the z/OSMF info endpoint URL from the instance information.
     *
     * @param instanceInfo the z/OSMF instance information
     * @return the full URL to the z/OSMF info endpoint
     */
    private String buildZosmfInfoUrl(InstanceInfo instanceInfo) {
        String baseUrl;
        if (instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            baseUrl = "https://" + instanceInfo.getHostName() + ":" + instanceInfo.getSecurePort();
        } else {
            baseUrl = "http://" + instanceInfo.getHostName() + ":" + instanceInfo.getPort();
        }
        return baseUrl + ZOSMF_INFO_ENDPOINT;
    }

    /**
     * Fetches the z/OSMF info from the info endpoint.
     *
     * @param url the z/OSMF info endpoint URL
     * @return the ZosmfInfo DTO or null if the call fails
     */
    private ZosmfInfo fetchZosmfInfo(String url) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");

        log.debug("Calling z/OSMF info endpoint: {}", url);
        
        ResponseEntity<ZosmfInfo> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ZosmfInfo.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            log.warn("Unexpected response from z/OSMF info endpoint: {}", response.getStatusCode());
            return null;
        }
    }

    /**
     * Updates the instance metadata with the z/OSMF version.
     * The version is stored in the apiInfo metadata following the standard format.
     *
     * @param metadata the instance metadata map
     * @param version the z/OSMF full version string
     */
    private void updateMetadataWithVersion(Map<String, String> metadata, String version) {
        // Find the gateway URL from existing metadata or use default
        String encodedGatewayUrl = findEncodedGatewayUrl(metadata);
        
        String versionKey = String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_VERSION);
        
        // Only update if version is not already set
        if (!metadata.containsKey(versionKey) || metadata.get(versionKey) == null) {
            metadata.put(versionKey, version);
            log.debug("Set z/OSMF version metadata: {} = {}", versionKey, version);
        }
    }

    /**
     * Finds the encoded gateway URL from the existing apiInfo metadata.
     *
     * @param metadata the instance metadata map
     * @return the encoded gateway URL or default value
     */
    private String findEncodedGatewayUrl(Map<String, String> metadata) {
        // Look for existing apiInfo gatewayUrl entries
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().startsWith(API_INFO) && entry.getKey().endsWith(".gatewayUrl")) {
                // Extract the encoded gateway URL part from the key
                // Key format: apiml.apiInfo.<encodedGatewayUrl>.gatewayUrl
                String key = entry.getKey();
                String[] parts = key.split("\\.");
                if (parts.length >= 3) {
                    return parts[2]; // The encoded gatewayUrl (e.g., "api-v1")
                }
            }
        }
        return DEFAULT_GATEWAY_URL;
    }
}
