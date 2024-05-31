/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;


@Service
public class ZosmfFilterFactory extends TokenFilterFactory {

    public ZosmfFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(webClient, instanceInfoService, messageService);
    }

    @Override
    public String getEndpointUrl(ServiceInstance instance) {
        return String.format("%s://%s:%d/%s/zaas/zosmf", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

}
