/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    @Qualifier("HttpClientChooser")
    public HttpClientChooser httpClientChooser(@Qualifier("secureHttpClientWithKeystore")
                                                   CloseableHttpClient withCertificate,
                                               @Qualifier("secureHttpClientWithoutKeystore")
                                                   CloseableHttpClient withoutCertificate
    ) {
        return new HttpClientChooser(withoutCertificate, withCertificate);
    }
}
