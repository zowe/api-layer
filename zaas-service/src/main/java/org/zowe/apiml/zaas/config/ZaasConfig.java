/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.product.instance.ServiceAddress;

@Configuration
public class ZaasConfig {

    @Bean
    public ServiceAddress getZaasAddress(@Value("${apiml.zaas.hostname}") String hostname,
                                         @Value("${apiml.service.port}") String port, @Value("${apiml.service.scheme}") String scheme) {
        return ServiceAddress.builder().scheme(scheme).hostname(hostname + ":" + port).build();
    }

}
