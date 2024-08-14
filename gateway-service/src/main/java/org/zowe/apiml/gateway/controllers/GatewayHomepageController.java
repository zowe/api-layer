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

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.version.BuildInfo;
import org.zowe.apiml.product.version.BuildInfoDetails;

import java.util.List;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

/**
 * Main page for Gateway, displaying status of Apiml services and build version information
 */
@Tag(name = "Home page")
@Controller
public class GatewayHomepageController {

    private static final String SUCCESS_ICON_NAME = "success";
    private static final String WARNING_ICON_NAME = "warning";
    private static final String UI_V1_ROUTE = "%s.ui-v1.%s";
    private static final String ZAAS_SERVICEID = CoreService.ZAAS.getServiceId();

    private final DiscoveryClient discoveryClient;

    private final BuildInfo buildInfo;
    private String buildString;

    private final String apiCatalogServiceId;

    @Autowired
    public GatewayHomepageController(DiscoveryClient discoveryClient,
                                     @Value("${apiml.catalog.serviceId:}") String apiCatalogServiceId) {
        this(discoveryClient, new BuildInfo(), apiCatalogServiceId);
    }

    public GatewayHomepageController(DiscoveryClient discoveryClient,
                                     BuildInfo buildInfo,
                                     String apiCatalogServiceId) {
        this.discoveryClient = discoveryClient;
        this.buildInfo = buildInfo;
        this.apiCatalogServiceId = apiCatalogServiceId;

        initializeBuildInfos();
    }

    @Hidden
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
        String authStatusText = "The Authentication Service is not running";
        String authIconName = WARNING_ICON_NAME;
        long zaasCount = authorizationServiceCount();

        if (zaasCount > 0) {
            authIconName = SUCCESS_ICON_NAME;
            authStatusText = zaasCount > 1 ?
                zaasCount + " Authentication Service instances are running" : "The Authentication Service is running";
        }

        model.addAttribute("authStatusText", authStatusText);
        model.addAttribute("authIconName", authIconName);
    }

    private void initializeCatalogAttributes(Model model) {
        boolean isAnyCatalogAvailable = (apiCatalogServiceId != null && !apiCatalogServiceId.isEmpty());
        model.addAttribute("isAnyCatalogAvailable", isAnyCatalogAvailable);
        if (!isAnyCatalogAvailable) {
            return;
        }

        String catalogLink = null;
        String catalogStatusText = "The API Catalog is not running";
        String catalogIconName = WARNING_ICON_NAME;
        boolean linkEnabled = false;
        long zaasCount = authorizationServiceCount();
        List<ServiceInstance> catalogServiceInstances = discoveryClient.getInstances(apiCatalogServiceId);
        if (catalogServiceInstances != null && zaasCount > 0) {
            long catalogCount = catalogServiceInstances.size();
            if (catalogCount > 0) {
                linkEnabled = true;
                catalogIconName = SUCCESS_ICON_NAME;
                catalogLink = getCatalogLink(catalogServiceInstances.get(0));

                catalogStatusText = catalogCount > 1 ?
                    catalogCount + " API Catalog instances are running" : "The API Catalog is running";
            }
        }

        model.addAttribute("catalogLink", catalogLink);
        model.addAttribute("catalogIconName", catalogIconName);
        model.addAttribute("catalogLinkEnabled", linkEnabled);
        model.addAttribute("catalogStatusText", catalogStatusText);
    }

    private int authorizationServiceCount() {
        List<ServiceInstance> zaasServiceInstances = discoveryClient.getInstances(ZAAS_SERVICEID);
        if (zaasServiceInstances != null) {
            return zaasServiceInstances.size();
        }
        return 0;
    }

    private String getCatalogLink(ServiceInstance catalogInstance) {
        String gatewayUrl = catalogInstance.getMetadata().get(String.format(UI_V1_ROUTE, ROUTES, ROUTES_GATEWAY_URL));
        String serviceUrl = catalogInstance.getMetadata().get(String.format(UI_V1_ROUTE, ROUTES, ROUTES_SERVICE_URL));
        return serviceUrl + gatewayUrl;
    }

}
