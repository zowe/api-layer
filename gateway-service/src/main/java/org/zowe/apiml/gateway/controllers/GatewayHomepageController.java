/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.product.version.BuildInfo;
import org.zowe.apiml.product.version.BuildInfoDetails;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.List;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

/**
 * Main page for Gateway, displaying status of Apiml services and build version information
 */
@Controller
public class GatewayHomepageController {

    private static final String SUCCESS_ICON_NAME = "success";

    private final DiscoveryClient discoveryClient;
    private final AuthConfigurationProperties authConfigurationProperties;

    private BuildInfo buildInfo;
    private String buildString;

    @Autowired
    public GatewayHomepageController(DiscoveryClient discoveryClient,
                                     AuthConfigurationProperties authConfigurationProperties) {
       this(discoveryClient, authConfigurationProperties, new BuildInfo());
    }

    public GatewayHomepageController(DiscoveryClient discoveryClient,
                                     AuthConfigurationProperties authConfigurationProperties,
                                     BuildInfo buildInfo) {
        this.discoveryClient = discoveryClient;
        this.authConfigurationProperties = authConfigurationProperties;
        this.buildInfo = buildInfo;

        initializeBuildInfos();
    }

    @GetMapping("/")
    public String home(Model model) {
        initializeCatalogAttributes(model);
        initializeDiscoveryAttributes(model);
        initializeAuthenticationAttributes(model);

        model.addAttribute("buildInfoText", buildString);
        return "home";
    }

    private void initializeBuildInfos() {
        BuildInfoDetails buildInfoDetails = buildInfo.getBuildInfoDetails();
        buildString = "Build information is not available";
        if (!buildInfoDetails.getVersion().equalsIgnoreCase("unknown")) {
            buildString = String.format("Version %s build # %s", buildInfoDetails.getVersion(), buildInfoDetails.getNumber());
        }
    }

    private void initializeDiscoveryAttributes(Model model) {
        String discoveryStatusText = null;
        String discoveryIconName = null;

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("discovery");
        if (serviceInstances != null) {
            int discoveryCount = serviceInstances.size();
            switch (discoveryCount) {
                case 0:
                    discoveryStatusText = "The Discovery Service is not running";
                    discoveryIconName = "danger";
                    break;
                case 1:
                    discoveryStatusText = "The Discovery Service is running";
                    discoveryIconName = SUCCESS_ICON_NAME;
                    break;
                default:
                    discoveryStatusText = discoveryCount + " Discovery Service instances are running";
                    discoveryIconName = SUCCESS_ICON_NAME;
                    break;
            }
        }

        model.addAttribute("discoveryStatusText", discoveryStatusText);
        model.addAttribute("discoveryIconName", discoveryIconName);
    }

    private void initializeAuthenticationAttributes(Model model) {
        String authStatusText = "The Authentication service is not running";
        String authIconName = "warning";
        boolean authUp = authorizationServiceUp();

        if (authUp) {
            authStatusText = "The Authentication service is running";
            authIconName = SUCCESS_ICON_NAME;
        }

        model.addAttribute("authStatusText", authStatusText);
        model.addAttribute("authIconName", authIconName);
    }

    private void initializeCatalogAttributes(Model model) {
        String catalogLink = null;
        String catalogStatusText = "The API Catalog is not running";
        String catalogIconName = "warning";
        boolean linkEnabled = false;
        boolean authServiceEnabled = authorizationServiceUp();

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("apicatalog");
        if (serviceInstances != null && authServiceEnabled) {
            long catalogCount = serviceInstances.size();
            if (catalogCount == 1) {
                linkEnabled = true;
                catalogIconName = SUCCESS_ICON_NAME;
                catalogStatusText = "The API Catalog is running";
                catalogLink = getCatalogLink(serviceInstances.get(0));
            }
        }

        model.addAttribute("catalogLink", catalogLink);
        model.addAttribute("catalogIconName", catalogIconName);
        model.addAttribute("linkEnabled", linkEnabled);
        model.addAttribute("catalogStatusText", catalogStatusText);
    }

    private String getCatalogLink(ServiceInstance catalogInstance) {
        String gatewayUrl = catalogInstance.getMetadata().get(String.format("%s.ui_v1.%s", ROUTES, ROUTES_GATEWAY_URL));
        String serviceUrl = catalogInstance.getMetadata().get(String.format("%s.ui_v1.%s", ROUTES, ROUTES_SERVICE_URL));
        return gatewayUrl + serviceUrl;
    }

    private boolean authorizationServiceUp() {
        boolean authUp = true;

        if (authConfigurationProperties.getProvider().equalsIgnoreCase(LoginProvider.ZOSMF.toString())) {
            authUp = !this.discoveryClient.getInstances(authConfigurationProperties.validatedZosmfServiceId()).isEmpty();
        }

        return authUp;
    }
}
