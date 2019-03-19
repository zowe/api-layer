/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.config;

import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.product.web.HttpConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InitializeTokenSecret implements InitializingBean {
  
    @Autowired
    private HttpConfig httpConfig;

    @Autowired
    private AuthenticationService authenticationService;
 
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Initializing token service with secret key");
        authenticationService.setSecret(httpConfig.getSecret());
    }
}
