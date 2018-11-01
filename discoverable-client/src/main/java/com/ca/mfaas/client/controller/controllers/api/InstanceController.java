/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.controller.controllers.api;

import com.ca.mfaas.enable.services.DiscoveredServiceInstance;
import com.ca.mfaas.enable.services.DiscoveredServiceInstances;
import com.ca.mfaas.enable.services.MfaasServiceLocator;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.family.ProductFamilyType;
import com.netflix.appinfo.InstanceInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;

/**
 * A Controller for returning instance runtime info
 */
@RestController
@RequestMapping("/api/v1/instance")
@Api(tags = {"Other Operations"}, description = "General Endpoints")
public class InstanceController {

    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private final MfaasServiceLocator mfaasServiceLocator;

    /**
     * Test controller for checking instance services
     * @param propertiesContainer MFaaS properties
     */
    @Autowired
    public InstanceController(MFaaSConfigPropertiesContainer propertiesContainer,
                              MfaasServiceLocator mfaasServiceLocator) {
        this.propertiesContainer = propertiesContainer;
        this.mfaasServiceLocator = mfaasServiceLocator;
    }

    /**
     * What is the configured port
     *
     * @return app.yaml port
     */
    @GetMapping(value = "/configured-port", produces = "text/plain")
    @ApiOperation(value = "What port is this controller configured for",
        notes = "What port is this controller configured for",
        tags = {"Other Operations"},
        response = String.class)
    public String getPort() {
        return propertiesContainer.getServer().getPort();
    }

    /**
     * What is the gateway URL
     *
     * @return gateway url
     */
    @GetMapping(value = "/gateway-url", produces = "text/plain")
    @ApiOperation(value = "What is the URL of the Gateway",
        notes = "What is the URL of the Gateway",
        response = String.class)
    public String getGatewayLocation() throws URISyntaxException {
        DiscoveredServiceInstance instances = mfaasServiceLocator.getServiceInstances(ProductFamilyType.GATEWAY.getServiceId());
        if (instances != null && instances.hasInstances()) {
            if (instances.getInstanceInfos() != null && !instances.getInstanceInfos().isEmpty()) {
                InstanceInfo gatewayInstance = instances.getInstanceInfos().get(0);
                if (gatewayInstance.isPortEnabled(InstanceInfo.PortType.SECURE)) {
                    return new URIBuilder().setScheme("https").setHost(gatewayInstance.getSecureVipAddress()).setPort(gatewayInstance.getSecurePort()).build().toASCIIString();
                } else {
                    return new URIBuilder().setScheme("http").setHost(gatewayInstance.getVIPAddress()).setPort(gatewayInstance.getPort()).build().toASCIIString();
                }
            } else if (instances.getServiceInstances() != null && !instances.getServiceInstances().isEmpty()) {
                ServiceInstance info = instances.getServiceInstances().get(0);
                return new URIBuilder().setScheme(info.getScheme()).setHost(info.getHost()).setPort(info.getPort()).build().toASCIIString();
            }
        }
        return null;
    }
}
