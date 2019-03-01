/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.api;


import com.ca.mfaas.enable.services.LocalApiDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger.web.InMemorySwaggerResourcesProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * Controller for handling retrieval of API doc via the gateway
 */
@RestController
@RequestMapping("/")
public class ApiDocController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ApiDocController.class);
    private final boolean apiDocEnabled;
    private LocalApiDocService localApiDocService;
    private InMemorySwaggerResourcesProvider swaggerResourcesProvider;
    private String swaggerLocation;

    /**
     * API Doc retrieval controller
     * Autowire in dependencies to contoller
     *
     * @param apiDocEnabled      does the service have API Documentation
     * @param swaggerLocation    optional parameter to tell the controller where to load a static swagger file
     * @param localApiDocService retrieve the API doc locally and not through the gateway
     */
    @Autowired
    public ApiDocController(
        @Value("${eureka.instance.metadata-map.mfaas.discovery.enableApiDoc:true}") boolean apiDocEnabled,
        @Value("${eureka.instance.metadata-map.mfaas.api-info.swagger.location:}") String swaggerLocation,
        LocalApiDocService localApiDocService) {
        this.apiDocEnabled = apiDocEnabled;
        this.localApiDocService = localApiDocService;
        this.swaggerLocation = swaggerLocation;
    }

    /**
     * Retrieve the API doc for the given group (or default to the first alphanumeric version tag)
     *
     * @param apiDocGroup the group to retrieve
     * @return the API doc for a group
     * @throws IOException when retrieving the doc fails
     */
    @GetMapping(value = "/api-doc", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getApiDoc(@RequestParam(value = "group", required = false) String apiDocGroup) throws IOException {
        if (swaggerLocation == null || swaggerLocation.isEmpty()) {
            return this.localApiDocService.getApiDoc(apiDocGroup);
        } else {
            return loadApiDocumentationFromStaticResourceFileAsJson();
        }
    }

    /**
     * Is API Doc enabled for the implementing service
     *
     * @return true if enabled
     */
    @GetMapping(value = "/api-doc/enabled", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public boolean isApiDocEnabled() {
        return this.apiDocEnabled;
    }

    /**
     * Load the swagger/api doc info from a local resource file
     *
     * @return the swagger as a String
     * @throws IOException when reading the file fails
     */
    @SuppressWarnings("Duplicates")
    private String loadApiDocumentationFromStaticResourceFileAsJson() throws IOException {
        log.debug("Loading Api Documentation from static resource: " + swaggerLocation);
        try {
            if (!swaggerLocation.startsWith(CLASSPATH_URL_PREFIX)) {
                swaggerLocation = CLASSPATH_URL_PREFIX + swaggerLocation.trim();
            }
            File file = ResourceUtils.getFile(swaggerLocation);
            if (!file.exists()) {
                throw new IOException("Cannot find Api Documentation (swagger) file: " + swaggerLocation);
            }
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            log.error("An exception occurred when attempting to retrieve swagger file: "
                + swaggerLocation + ". " + e.getMessage(), e);
            throw e;
        }
    }

    public LocalApiDocService getLocalApiDocService() {
        return this.localApiDocService;
    }

    public void setLocalApiDocService(LocalApiDocService localApiDocService) {
        this.localApiDocService = localApiDocService;
    }

    public InMemorySwaggerResourcesProvider getSwaggerResourcesProvider() {
        return this.swaggerResourcesProvider;
    }

    public void setSwaggerResourcesProvider(InMemorySwaggerResourcesProvider swaggerResourcesProvider) {
        this.swaggerResourcesProvider = swaggerResourcesProvider;
    }

    public String getSwaggerLocation() {
        return this.swaggerLocation;
    }

    public void setSwaggerLocation(String swaggerLocation) {
        this.swaggerLocation = swaggerLocation;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.enable.api.ApiDocController)) return false;
        final com.ca.mfaas.enable.api.ApiDocController other = (com.ca.mfaas.enable.api.ApiDocController) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isApiDocEnabled() != other.isApiDocEnabled()) return false;
        final java.lang.Object this$localApiDocService = this.localApiDocService;
        final java.lang.Object other$localApiDocService = other.localApiDocService;
        if (this$localApiDocService == null ? other$localApiDocService != null : !this$localApiDocService.equals(other$localApiDocService))
            return false;
        final java.lang.Object this$swaggerResourcesProvider = this.swaggerResourcesProvider;
        final java.lang.Object other$swaggerResourcesProvider = other.swaggerResourcesProvider;
        if (this$swaggerResourcesProvider == null ? other$swaggerResourcesProvider != null : !this$swaggerResourcesProvider.equals(other$swaggerResourcesProvider))
            return false;
        final java.lang.Object this$swaggerLocation = this.swaggerLocation;
        final java.lang.Object other$swaggerLocation = other.swaggerLocation;
        if (this$swaggerLocation == null ? other$swaggerLocation != null : !this$swaggerLocation.equals(other$swaggerLocation))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.enable.api.ApiDocController;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isApiDocEnabled() ? 79 : 97);
        final java.lang.Object $localApiDocService = this.localApiDocService;
        result = result * PRIME + ($localApiDocService == null ? 43 : $localApiDocService.hashCode());
        final java.lang.Object $swaggerResourcesProvider = this.swaggerResourcesProvider;
        result = result * PRIME + ($swaggerResourcesProvider == null ? 43 : $swaggerResourcesProvider.hashCode());
        final java.lang.Object $swaggerLocation = this.swaggerLocation;
        result = result * PRIME + ($swaggerLocation == null ? 43 : $swaggerLocation.hashCode());
        return result;
    }

    public String toString() {
        return "ApiDocController(apiDocEnabled=" + this.isApiDocEnabled() + ", localApiDocService=" + this.localApiDocService + ", swaggerResourcesProvider=" + this.swaggerResourcesProvider + ", swaggerLocation=" + this.swaggerLocation + ")";
    }
}
