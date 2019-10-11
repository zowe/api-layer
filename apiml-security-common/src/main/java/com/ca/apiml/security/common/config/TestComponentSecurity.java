/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.config;

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class TestComponentSecurity {

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();


    @PostConstruct
    public void init() {
        apimlLogger.log("com.ca.mfaas.security.common.ZosmfNotFound");
        apimlLogger.log("com.ca.mfaas.common.httpMethodIsNotSupported");
    }

}
