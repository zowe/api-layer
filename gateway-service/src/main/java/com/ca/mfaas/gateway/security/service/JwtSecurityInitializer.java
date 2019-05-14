/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.security.service;

import com.ca.mfaas.product.web.HttpConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


@Slf4j
@Service
@Data
public class JwtSecurityInitializer {

    @Value("${apiml.security.jwt.signatureAlgorithm:HS512}")
    private String signatureAlgorithm;

    @Value("${apiml.security.jwt.secretKey:#{null}}")
    private String secretKey;

    private String initializedSecret;

    private final HttpConfig httpConfig;
    public JwtSecurityInitializer(HttpConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isEmpty()) {
            initializedSecret =  httpConfig.getSecret();
        }
    }

    public String getInitializedSecret() {
        return initializedSecret;
    }
}
