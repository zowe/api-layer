/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */


package org.zowe.apiml.gateway.conformance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Data;


@Data
@Configuration
public class DiscoveryConfigUri {

    @Value("${apiml.service.discoveryServiceUrls}")
    private String[] locations;

    @Value("${apiml.service.eurekaUserName:eureka}")
    private String eurekaUserName;

    @Value("${apiml.service.eurekaUserPassword:password}")
    private String eurekaUserPassword;
}
