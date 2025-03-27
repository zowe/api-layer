/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("apiml.gateway.eureka.instance")
public class GatewayEurekaInstanceConfigBean extends EurekaInstanceConfigBean {

    public GatewayEurekaInstanceConfigBean(InetUtils inetUtils) {
        super(inetUtils);
    }

}
