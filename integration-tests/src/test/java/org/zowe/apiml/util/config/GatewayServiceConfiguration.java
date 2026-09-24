/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.zowe.apiml.product.constants.CoreService;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayServiceConfiguration extends ServiceConfiguration {

    private String dvipaHost;
    //TODO remove external port from config - not used in v3
    // and from Integration tests
    //private int externalPort;
    private String internalPorts;
    private String servicesEndpoint;
    private int bucketCapacity;
    private String authProvider;
    private Integer connectionTimeout;

    GatewayServiceConfiguration(String scheme, String host, String dvipaHost, String port, int instances, String internalPorts, String servicesEndpoint, int bucketCapacity, String authProvider, Integer connectionTimeout) {
        super(scheme, null, host, port, instances);
        this.dvipaHost = dvipaHost;
        this.internalPorts = internalPorts;
        this.servicesEndpoint = servicesEndpoint;
        this.bucketCapacity = bucketCapacity;
        this.authProvider = authProvider;
        this.connectionTimeout = connectionTimeout;
    }

    public String getServiceId() {
        return CoreService.GATEWAY.getServiceId();
    }

}
