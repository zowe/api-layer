/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.controllers;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.gateway.security.login.LoginProvider;
import com.ca.mfaas.product.service.BuildInfo;
import com.ca.mfaas.product.service.BuildInfoDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.PostConstruct;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class GatewayHomepageController {

    private static final String SUCCESS_ICON_NAME = "success";

    private final DiscoveryClient discoveryClient;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    private String buildString;

    @PostConstruct
    private void init() {
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
        BuildInfoDetails buildInfo = new BuildInfo().getBuildInfoDetails();
        buildString = "Build information is not available";
        if (!buildInfo.getVersion().equalsIgnoreCase("unknown")) {
            buildString = String.format("Version %s build # %s", buildInfo.getVersion(), buildInfo.getNumber());
        }
    }

    private void initializeDiscoveryAttributes(Model model) {
        String discoveryStatusText;
        String discoveryIconName;

        List<ServiceInstance> discoveryInstances = getInstancesById("discovery");
        int discoveryCount = discoveryInstances.size();
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

        model.addAttribute("discoveryStatusText", discoveryStatusText);
        model.addAttribute("discoveryIconName", discoveryIconName);
    }

    private void initializeAuthenticationAttributes(Model model) {
        String authStatusText = "The Authentication service is not running";
        String authIconName = "warning";
        boolean authUp = true;

        if (!securityConfigurationProperties.getProvider().equalsIgnoreCase(LoginProvider.DUMMY.toString())) {
            authUp = !this.discoveryClient.getInstances(securityConfigurationProperties.validatedZosmfServiceId()).isEmpty();
        }

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

        List<ServiceInstance> catalogInstances = getInstancesById("apicatalog");
        int catalogCount = catalogInstances.size();
        if (catalogCount == 1) {
            linkEnabled = true;
            catalogIconName = SUCCESS_ICON_NAME;
            catalogStatusText = "The API Catalog is running";
            catalogLink = getCatalogLink(catalogInstances.get(0));
        }

        model.addAttribute("catalogLink", catalogLink);
        model.addAttribute("catalogIconName", catalogIconName);
        model.addAttribute("linkEnabled", linkEnabled);
        model.addAttribute("catalogStatusText", catalogStatusText);
    }

    private List<ServiceInstance> getInstancesById(String serviceId) {
        return this.discoveryClient.getInstances(serviceId);
    }

    private String getCatalogLink(ServiceInstance catalogInstance) {
        String gatewayUrl = catalogInstance.getMetadata().get("routed-services.ui_v1.gateway-url");
        String serviceUrl = catalogInstance.getMetadata().get("routed-services.ui_v1.service-url");
        return gatewayUrl + serviceUrl;
    }

}
