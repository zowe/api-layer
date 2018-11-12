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

import com.ca.mfaas.product.service.BuildInfo;
import com.ca.mfaas.product.service.BuildInfoDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class GatewayHomepageController {

    private final DiscoveryClient discoveryClient;

    @Autowired
    public GatewayHomepageController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/")
    public String home(Model model) {
        String catalogLink = null;
        String catalogStatusText = null;
        String catalogIconName = "success";
        boolean linkEnabled = false;
        List<ServiceInstance> catalogInstances = getInstancesById("apicatalog");
        int catalogCount = catalogInstances.size();

        if (catalogCount == 1) {
            catalogLink = getCatalogLink(catalogInstances.get(0));
            catalogStatusText = "The API Catalog is running";
            linkEnabled = true;
        } else if (catalogCount == 0) {
            catalogStatusText = "The API Catalog is not running";
            linkEnabled = false;
            catalogIconName = "warning";
        }

        List<ServiceInstance> discoveryInstances = getInstancesById("discovery");
        int discoveryCount = discoveryInstances.size();
        String discoveryStatusText;
        String discoveryIconName;

        switch (discoveryCount) {
            case 0:
                discoveryStatusText = "The Discovery Service is not running";
                discoveryIconName = "danger";
                break;
            case 1:
                discoveryStatusText = "The Discovery Service is running";
                discoveryIconName = "success";
                break;
            default:
                discoveryStatusText = discoveryCount + " Discovery Service instances are running";
                discoveryIconName = "success";
                break;
        }

        BuildInfoDetails buildInfo = new BuildInfo().getBuildInfoDetails();
        String buildString = "Build information is not available";
        if (!buildInfo.getVersion().equalsIgnoreCase("unknown")) {
            buildString = String.format("Version %s build # %s", buildInfo.getVersion(), buildInfo.getNumber());
        }

        model.addAttribute("discoveryStatusText", discoveryStatusText);
        model.addAttribute("discoveryIconName", discoveryIconName);
        model.addAttribute("catalogLink", catalogLink);
        model.addAttribute("catalogIconName", catalogIconName);
        model.addAttribute("linkEnabled", linkEnabled);
        model.addAttribute("catalogStatusText", catalogStatusText);
        model.addAttribute("catalogStatusText", catalogStatusText);
        model.addAttribute("buildInfoText", buildString);
        return "home";
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
