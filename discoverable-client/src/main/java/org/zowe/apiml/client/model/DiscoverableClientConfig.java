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

    @Value("${apiml.service.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifyCerts;

    @Value("${apiml.service.ssl.protocol:${server.ssl.protocol:TLSv1.2}}")
    private String sslProtocol;

    @Value("${apiml.service.ssl.key-store-type:${server.ssl.keyStoreType:PKCS12}}")
    private String keyStoreType;

    @Value("${apiml.service.ssl.trust-store-type:${server.ssl.trustStoreType:PKCS12}}")
    private String trustStoreType;

    @Value("${apiml.service.ssl.key-alias:${server.ssl.keyAlias:#{null}}}")
    private String keyAlias;

    @Value("${apiml.service.ssl.key-password:${server.ssl.keyPassword:#{null}}}")
    private String keyPassword;

    @Value("${apiml.service.ssl.key-store:${server.ssl.keyStore:#{null}}}")
    private String keyStore;

    @Value("${apiml.service.ssl.key-store-password:${server.ssl.keyStorePassword:#{null}}}")
    private String keyStorePassword;

    @Value("${apiml.service.ssl.trust-store:${server.ssl.trustStore:#{null}}}")
    private String trustStore;

    @Value("${apiml.service.ssl.trust-store-password:${server.ssl.trustStorePassword:#{null}}}")
    private String trustStorePassword;

    @Value("${apiml.service.connectTimeout:5}")
    private int connectTimeout;

    @Value("${apiml.service.readTimeout:8}")
    private int readTimeout;
}
