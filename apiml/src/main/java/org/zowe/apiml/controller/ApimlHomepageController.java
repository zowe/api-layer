/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.product.version.BuildInfo;
import org.zowe.apiml.product.version.BuildInfoDetails;

import java.util.List;

/**
 * Main page for API ML, displaying status and build version information
 */
@Tag(name = "Home page")
@RequiredArgsConstructor
@Controller
public class ApimlHomepageController {

    private static final String SUCCESS_ICON_NAME = "success";

    private final DiscoveryClient discoveryClient;
    private final BuildInfo buildInfo;

    private final ApplicationInfo applicationInfo;
    private String buildString;

    @PostConstruct
    public void init() {
        initializeBuildInfos();
    }

    @Hidden
    @GetMapping("/")
    public String home(Model model) {

        initializeParameters(model);
        return "home";
    }

    private void initializeBuildInfos() {
        BuildInfoDetails buildInfoDetails = buildInfo.getBuildInfoDetails();
        buildString = "Build information is not available";
        if (!buildInfoDetails.getVersion().equalsIgnoreCase("unknown")) {
            buildString = String.format("Version %s build # %s", buildInfoDetails.getVersion(), buildInfoDetails.getNumber());
        }
    }

    private void initializeParameters(Model model) {
        long apimlInstances = apimlInstancesCount();
        var authStatusText = "Number of API ML instances: " + apimlInstances;
        model.addAttribute("authStatusText", authStatusText);
        model.addAttribute("authIconName", SUCCESS_ICON_NAME);
        model.addAttribute("catalogLink", "/apicatalog/ui/v1");
        model.addAttribute("isAnyCatalogAvailable", true);
        model.addAttribute("catalogIconName", SUCCESS_ICON_NAME);
        model.addAttribute("catalogLinkEnabled", true);
        model.addAttribute("catalogStatusText", "The API Catalog");
        model.addAttribute("buildInfoText", buildString);
    }

    private int apimlInstancesCount() {
        List<ServiceInstance> apimlInstances = discoveryClient.getInstances(applicationInfo.getAuthServiceId());
        if (apimlInstances != null) {
            return apimlInstances.size();
        }
        return 0;
    }

}

