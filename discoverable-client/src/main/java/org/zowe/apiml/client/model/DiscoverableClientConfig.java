/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
public class DiscoverableClientConfig {
    @Value("${apiml.service.discoveryServiceUrls}")
    private List<String> discoveryServiceUrls;

    @Value("${apiml.service.scheme}")
    private String scheme;

    @Value("${apiml.service.hostname}")
    private String hostname;

    @Value("${apiml.service.catalog.tile.id}")
    private String catalogId;

    @Value("${apiml.service.ssl.enabled:true}")
    private boolean sslEnabled;

    @Value("${apiml.service.ssl.verifySslCertificatesOfServices:false}")
    private boolean verifyCerts;

    @Value("${server.ssl.protocol}")
    private String sslProtocol;

    @Value("${server.ssl.keyStoreType}")
    private String keyStoreType;

    @Value("${server.ssl.trustStoreType}")
    private String trustStoreType;

    @Value("${server.ssl.keyAlias}")
    private String keyAlias;

    @Value("${server.ssl.keyPassword}")
    private String keyPassword;

    @Value("${server.ssl.keyStore}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword}")
    private String keyStorePassword;

    @Value("${server.ssl.trustStore}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword}")
    private String trustStorePassword;
}
