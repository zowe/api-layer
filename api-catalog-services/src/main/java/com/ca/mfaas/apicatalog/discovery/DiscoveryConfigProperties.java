/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.discovery;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Describes locations of the Discovery Service
 * <p>
 * eurekaUserName and eurekaUserPassword are used only for HTTP connection
 */
@Data
@Configuration
public class DiscoveryConfigProperties {

    @Value("${mfaas.discovery.locations}")
    private String locations;

    @Value("${mfaas.discovery.eurekaUserName:eureka}")
    private String eurekaUserName;

    @Value("${mfaas.discovery.eurekaUserPassword:password}")
    private String eurekaUserPassword;
}
