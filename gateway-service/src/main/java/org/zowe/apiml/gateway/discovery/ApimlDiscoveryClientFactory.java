/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class ApimlDiscoveryClientFactory {

    public ApimlDiscoveryClient buildApimlDiscoveryClient(ApplicationInfoManager perClientAppManager, EurekaClientConfigBean configBean, MutableDiscoveryClientOptionalArgs args, ApplicationContext context) {
        return new ApimlDiscoveryClient(perClientAppManager, configBean, args, context);
    }
}
