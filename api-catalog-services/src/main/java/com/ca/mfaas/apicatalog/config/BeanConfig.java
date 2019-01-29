/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.config;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

@Configuration
public class BeanConfig {
    @Bean
    public ErrorService errorService() {
        return new ErrorServiceImpl("/messages.yml");
    }

    @Bean
    public RestTemplate apiRestClient() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
            .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        return restTemplate;
    }
}
